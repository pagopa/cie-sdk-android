package it.pagopa.io.app.cie.pace

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class PaceDecrypt {
    @Throws(Exception::class)
    fun decryptNonce(
        cipherAlg: PACECipherAlgorithms,
        paceKey: ByteArray,
        encryptedNonce: ByteArray
    ): ByteArray {
        return if (cipherAlg == PACECipherAlgorithms.AES) {
            val iv = ByteArray(16) // 16 byte di zeri
            decryptAES(paceKey, encryptedNonce, iv)
        } else {
            val iv = ByteArray(8) // 8 byte di zeri
            decrypt3DES(paceKey, encryptedNonce, iv)
        }
    }

    @Throws(Exception::class)
    private fun decryptAES(key: ByteArray, message: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(message)
    }

    @Throws(Exception::class)
    private fun decrypt3DES(key: ByteArray, message: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
        val secretKey = SecretKeySpec(key, "DESede")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(message)
    }
}