package it.pagopa.io.app.cie.nfc

import java.security.MessageDigest

internal object Sha256 {
    @Throws(Exception::class)
    fun encrypt(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }
}