package it.pagopa.io.app.cie.nfc

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * supports method for Encryption and decryption purpose
 */
internal object Algorithms {

    /**
     * MAC3 implementation
     * @param masterKey master key
     * @param data to cipher data
     * @return the ciphers data
     * @throws Exception
     */
    @Throws(Exception::class)
    internal fun macEnc(masterKey: ByteArray, data: ByteArray): ByteArray {
        val k1 = ByteArray(8)
        val k2 = ByteArray(8)
        val k3 = ByteArray(8)
        System.arraycopy(masterKey, 0, k1, 0, 8)
        System.arraycopy(masterKey, if (masterKey.size >= 16) 8 else 0, k2, 0, 8)
        System.arraycopy(masterKey, if (masterKey.size >= 24) 16 else 0, k3, 0, 8)
        val mid1 = desEnc(k1, data)//40byte
        val mid2 = desDec(k2, getSub(mid1, mid1.size - 8, 8))//8byte
        return desEnc(k3, getSub(mid2, 0, 8))
    }

    /**
     * Des encrypt
     * @param masterKey master key
     * @param data to cipher data
     * @return the ciphers data
     * @throws Exception
     */
    @Throws(Exception::class)
    internal fun desEnc(masterKey: ByteArray, data: ByteArray): ByteArray {
        var key24 = ByteArray(24)
        if (masterKey.size == 8) {
            System.arraycopy(masterKey, 0, key24, 0, 8)
            System.arraycopy(masterKey, 0, key24, 8, 8)
            System.arraycopy(masterKey, 0, key24, 16, 8)
        } else if (masterKey.size == 16) {
            System.arraycopy(masterKey, 0, key24, 0, 16)
            System.arraycopy(masterKey, 0, key24, 16, 8)
        } else {
            key24 = masterKey
        }

        val key = SecretKeySpec(key24, "TripleDES")
        val iv = IvParameterSpec(ByteArray(8))
        val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }

    /**
     * decrypt Des
     * @param masterKey master key
     * @param data to cipher data
     * @return the ciphers data
     * @throws Exception
     */
    @Throws(Exception::class)
    internal fun desDec(masterKey: ByteArray, data: ByteArray): ByteArray {
        var key24 = ByteArray(24)
        if (masterKey.size == 8) {
            System.arraycopy(masterKey, 0, key24, 0, 8)
            System.arraycopy(masterKey, 0, key24, 8, 8)
            System.arraycopy(masterKey, 0, key24, 16, 8)
        } else if (masterKey.size == 16) {
            System.arraycopy(masterKey, 0, key24, 0, 16)
            System.arraycopy(masterKey, 0, key24, 16, 8)
        } else {
            key24 = masterKey
        }
        val key = SecretKeySpec(key24, "TripleDES")
        val iv = IvParameterSpec(ByteArray(8))
        val decipher = Cipher.getInstance("DESede/CBC/NoPadding")
        decipher.init(Cipher.DECRYPT_MODE, key, iv)

        return decipher.doFinal(data)
    }

    /**
     * support method
     * @param array vector in which extracts data
     * @param start first index
     * @param num how much bytes should give back
     * @return a byte subset
     * @throws Exception
     */
    @Throws(Exception::class)
    internal fun getSub(array: ByteArray, start: Int, num: Int): ByteArray {
        val data = ByteArray(num)
        System.arraycopy(array, start, data, 0, data.size)
        return data
    }
}