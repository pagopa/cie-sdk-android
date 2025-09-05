package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Algorithms
import it.pagopa.io.app.cie.nfc.Utils
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter

internal class AuthToken {
    @Throws(Exception::class)
    fun generateAuthenticationToken(
        publicKey: ByteArray,
        macKey: ByteArray,
        oid: PaceOID,
        cipherAlg: PACECipherAlgorithms
    ): ByteArray {
        // 1️⃣ Costruzione ASN.1 DER corretta
        val encodedPublicKeyData = encodePublicKey(
            oid.objIdentifier,
            publicKey,
            oid.keyAgreementAlgorithm() == KeyAgreementAlgorithm.DH
        )
        CieLogger.i("ASN1_ENCODED_FOR_MAC", Utils.bytesToString(encodedPublicKeyData))
        CieLogger.i("OID_FOR_AUTH_TOKEN", oid.objIdentifier)

        // 2️⃣ Preparazione input per MAC
        val macInput: ByteArray = when (cipherAlg) {
            PACECipherAlgorithms.DESede -> {
                val padded = pkcs7Pad(encodedPublicKeyData)
                CieLogger.i("PADDED_DATA_FOR_MAC", Utils.bytesToString(padded))
                padded
            }

            PACECipherAlgorithms.AES -> {
                CieLogger.i("DATA_FOR_MAC", Utils.bytesToString(encodedPublicKeyData))
                encodedPublicKeyData
            }
        }

        // 3️⃣ Calcolo MAC
        val macced: ByteArray = when (cipherAlg) {
            PACECipherAlgorithms.DESede -> Algorithms.macEnc(macKey, macInput)
            PACECipherAlgorithms.AES -> aesMac(macKey, macInput)
        }
        CieLogger.i("FULL_MAC", Utils.bytesToString(macced))

        // 4️⃣ Restituisci primi 8 byte come token
        val token = macced.copyOfRange(0, 8)
        CieLogger.i("FINAL_TOKEN", Utils.bytesToString(token))
        return token
    }

    fun encodePublicKey(oid: String, pubKeyRaw: ByteArray, isDh: Boolean): ByteArray {
        // 1. OID in DER form
        val oidEncoded = ASN1ObjectIdentifier(oid).encoded // contiene già tag+len+value

        // 2. Scegli il tag per la chiave pubblica
        val pubKeyTag = if (isDh) 0x84 else 0x86

        // 3. Crea TLV per OID e chiave pubblica
        val encOid = oidEncoded // già TLV completo
        val encPub = tlv(pubKeyTag, pubKeyRaw)

        // 4. Contenitore 0x7F49 con dentro OID e chiave pubblica
        val inner = encOid + encPub
        val record = tlv(0x7F49, inner)

        return record
    }

    /**
     * Codifica un TLV con tag e valore dati
     */
    private fun tlv(tag: Int, value: ByteArray): ByteArray {
        val tagBytes = tagToBytes(tag)
        val lengthBytes = lengthToBytes(value.size)
        return tagBytes + lengthBytes + value
    }

    private fun tagToBytes(tag: Int): ByteArray {
        return if (tag <= 0xFF) {
            byteArrayOf(tag.toByte())
        } else {
            byteArrayOf((tag shr 8).toByte(), (tag and 0xFF).toByte())
        }
    }

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
     * Padding PKCS#7
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
     * AES CMAC (come AES-MAC)
     */
    private fun aesMac(key: ByteArray, msg: ByteArray): ByteArray {
        val cmac = CMac(AESEngine.newInstance()) // AES CMAC
        cmac.init(KeyParameter(key))
        cmac.update(msg, 0, msg.size)
        return ByteArray(cmac.macSize).also { cmac.doFinal(it, 0) }
    }
}