package it.pagopa.cie.cie.key_store

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ReadCie
import java.security.*

internal open class CieSignatureImpl : SignatureSpi() {
    private var byteToSign: ByteArray = byteArrayOf()

    @Throws(InvalidKeyException::class)
    override fun engineInitVerify(publicKey: PublicKey) {
    }

    @Throws(InvalidKeyException::class)
    override fun engineInitSign(privateKey: PrivateKey) {
        byteToSign = byteArrayOf()

    }

    @Throws(SignatureException::class)
    override fun engineUpdate(b: Byte) {
    }

    @Throws(SignatureException::class)
    override fun engineUpdate(bytes: ByteArray?, off: Int, len: Int) {
        if (bytes != null) {
            byteToSign += bytes
        }
    }

    @Throws(NullPointerException::class)
    override fun engineSign(): ByteArray? = ReadCie.cieCommands?.sign(byteToSign)

    override fun engineVerify(bytes: ByteArray): Boolean {
        return false
    }

    @Throws(InvalidParameterException::class)
    @Deprecated("deprecated by super class")
    override fun engineSetParameter(s: String, o: Any) {
    }

    @Throws(InvalidParameterException::class)
    @Deprecated("deprecated by super class")
    override fun engineGetParameter(s: String): Any? {
        return null
    }

    class None : CieSignatureImpl() {
        init {
            CieLogger.i("CieSignatureImpl", "NONE")
        }
    }
}