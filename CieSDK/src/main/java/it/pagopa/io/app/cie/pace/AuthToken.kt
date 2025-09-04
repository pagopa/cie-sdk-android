package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Utils
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERBitString
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.engines.DESedeEngine
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter

internal class AuthToken {
    @Throws(Exception::class)
    fun generateAuthenticationToken(
        publicKey: ByteArray,
        macKey: ByteArray,
        oid: String,
        cipherAlg: PACECipherAlgorithms
    ): ByteArray {
        // 1️⃣ Costruzione ASN.1 DER corretta
        val encodedPublicKeyData = encodePublicKey(oid, publicKey)
        CieLogger.i("ASN1_ENCODED_FOR_MAC", Utils.bytesToString(encodedPublicKeyData))

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
            PACECipherAlgorithms.DESede -> desMac(macKey, macInput)
            PACECipherAlgorithms.AES -> aesMac(macKey, macInput)
        }
        CieLogger.i("FULL_MAC", Utils.bytesToString(macced))

        // 4️⃣ Restituisci primi 8 byte come token
        val token = macced.copyOfRange(0, 8)
        CieLogger.i("FINAL_TOKEN", Utils.bytesToString(token))
        return token
    }

    /**
     * Encode Public Key con OID custom (ASN.1 DER)
     */
    private fun encodePublicKey(oid: String, rawKeyBytes: ByteArray): ByteArray {
        val oidObj = ASN1ObjectIdentifier(oid)
        val bitString = DERBitString(rawKeyBytes)
        val seq = DERSequence(
            arrayOf<ASN1Encodable>(
                DERSequence(arrayOf<ASN1Encodable>(oidObj, DERNull.INSTANCE)),
                bitString
            )
        )
        return seq.encoded
    }

    /**
     * Padding PKCS#7
     */
    private fun pkcs7Pad(data: ByteArray): ByteArray {
        val blockSize = 8
        val paddingLen = blockSize - (data.size % blockSize)
        return data + ByteArray(paddingLen) { paddingLen.toByte() }
    }

    /**
     * DESede CBC-MAC (64-bit MAC)
     */
    private fun desMac(key: ByteArray, msg: ByteArray): ByteArray {
        val mac = CBCBlockCipherMac(DESedeEngine(), 64)
        mac.init(KeyParameter(key))
        mac.update(msg, 0, msg.size)
        return ByteArray(mac.macSize).also { mac.doFinal(it, 0) }
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