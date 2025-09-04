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
        // 1. Public Key in ASN.1 DER with OID
        var encodedPublicKeyData = encodePublicKey(oid, publicKey)
        CieLogger.i("ASN1_ENCODED_PUBKEY", Utils.bytesToString(encodedPublicKeyData))

        val maccedPublicKeyDataObject: ByteArray = when (cipherAlg) {
            PACECipherAlgorithms.DESede -> {
                encodedPublicKeyData = pkcs7Pad(encodedPublicKeyData)
                desMac(macKey, encodedPublicKeyData)
            }

            PACECipherAlgorithms.AES -> {
                aesMac(macKey, encodedPublicKeyData)
            }
        }
        // 2. First MAC 8 bytes
        return maccedPublicKeyDataObject.copyOfRange(0, 8)
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