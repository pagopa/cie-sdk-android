package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Algorithms
import it.pagopa.io.app.cie.nfc.Utils
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter

internal class AuthToken {
    /**
     * It generates the PACE authentication token.
     *
     * @param publicKey Ephemeral public key (raw bytes) from the PACE key exchange.
     * @param macKey    The MAC key derived during PACE.
     * @param oid       OID identifying the key agreement parameters.
     * @param cipherAlg Cipher algorithm to be used for MAC (DESede or AES).
     * @return First 8 bytes of the computed MAC (authentication token).
     */
    @Throws(Exception::class)
    fun generateAuthenticationToken(
        publicKey: ByteArray,
        macKey: ByteArray,
        oid: PaceOID,
        cipherAlg: PACECipherAlgorithms
    ): ByteArray {
        // 1️⃣ Encode OID + public key into ASN.1 DER structure
        val encodedPublicKeyData = encodePublicKey(
            oid.objIdentifier,
            publicKey,
            oid.keyAgreementAlgorithm() == KeyAgreementAlgorithm.DH
        )
        CieLogger.i("ASN1_ENCODED_FOR_MAC", Utils.bytesToString(encodedPublicKeyData))
        CieLogger.i("OID_FOR_AUTH_TOKEN", oid.objIdentifier)

        // 2️⃣ Prepare the MAC input depending on algorithm
        val macInput: ByteArray = when (cipherAlg) {
            PACECipherAlgorithms.DESede -> {
                // DESede (3DES) MAC requires ISO 9797-1 Method 2 padding (0x80 then 0x00)
                val padded = pkcs7Pad(encodedPublicKeyData)
                CieLogger.i("PADDED_DATA_FOR_MAC", Utils.bytesToString(padded))
                padded
            }

            PACECipherAlgorithms.AES -> {
                // AES CMAC takes the data as is (no padding here)
                CieLogger.i("DATA_FOR_MAC", Utils.bytesToString(encodedPublicKeyData))
                encodedPublicKeyData
            }
        }

        // 3️⃣ Compute the MAC based on the chosen algorithm
        val macced: ByteArray = when (cipherAlg) {
            PACECipherAlgorithms.DESede -> Algorithms.macEnc(macKey, macInput) // Retail MAC 3DES
            PACECipherAlgorithms.AES -> aesMac(macKey, macInput)               // AES CMAC
        }
        CieLogger.i("FULL_MAC", Utils.bytesToString(macced))

        // 4️⃣ Return the first 8 bytes as the final authentication token
        val token = macced.copyOfRange(0, 8)
        CieLogger.i("FINAL_TOKEN", Utils.bytesToString(token))
        return token
    }

    /**
     * Builds the ASN.1 DER structure for the public key and OID.
     * Structure:
     *   7F49 (container)
     *     06   (OID tag)
     *     84/86 (Public key tag: 0x84 for DH, 0x86 for EC)
     */
    fun encodePublicKey(oid: String, pubKeyRaw: ByteArray, isDh: Boolean): ByteArray {
        val oidEncoded = ASN1ObjectIdentifier(oid).encoded // already TLV encoded
        val pubKeyTag = if (isDh) 0x84 else 0x86            // Tag depends on key agreement type
        val encOid = oidEncoded
        val encPub = tlv(pubKeyTag, pubKeyRaw)
        val inner = encOid + encPub
        val record = tlv(0x7F49, inner)                     // Wrap inside 0x7F49 container
        return record
    }

    /**
     * Encodes a TLV (Tag-Length-Value) structure.
     */
    private fun tlv(tag: Int, value: ByteArray): ByteArray {
        val tagBytes = tagToBytes(tag)
        val lengthBytes = lengthToBytes(value.size)
        return tagBytes + lengthBytes + value
    }

    /**
     * Converts an integer tag to its byte representation (1 or 2 bytes).
     */
    private fun tagToBytes(tag: Int): ByteArray {
        return if (tag <= 0xFF) {
            byteArrayOf(tag.toByte())
        } else {
            byteArrayOf((tag shr 8).toByte(), (tag and 0xFF).toByte())
        }
    }

    /**
     * Encodes length in ASN.1 DER format.
     * If < 128 → short form, otherwise → long form.
     */
    private fun lengthToBytes(length: Int): ByteArray {
        return when {
            length < 0x80 -> byteArrayOf(length.toByte())
            else -> {
                val lenBytes =
                    length.toBigInteger().toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
                byteArrayOf((0x80 or lenBytes.size).toByte()) + lenBytes
            }
        }
    }

    /**
     * ISO/IEC 9797-1 Method 2 padding (often wrongly called PKCS#7 here).
     * Adds 0x80 followed by zero bytes until block size is reached.
     * Default block size = 8 bytes (3DES).
     */
    private fun pkcs7Pad(data: ByteArray, blockSize: Int = 8): ByteArray {
        val ret = data.toMutableList()
        ret.add(0x80.toByte())
        while (ret.size % blockSize != 0) {
            ret.add(0x00)
        }
        return ret.toByteArray()
    }

    /**
     * Computes AES CMAC over given message using given key.
     * Returns full MAC (16 bytes). If you need only 8 bytes, truncate outside this method.
     */
    private fun aesMac(key: ByteArray, msg: ByteArray): ByteArray {
        val cmac = CMac(AESEngine.newInstance()) // AES CMAC
        cmac.init(KeyParameter(key))
        cmac.update(msg, 0, msg.size)
        return ByteArray(cmac.macSize).also { cmac.doFinal(it, 0) }
    }
}