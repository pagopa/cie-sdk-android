package it.pagopa.io.app.cie.nfc

import it.pagopa.io.app.cie.toHex
import javax.crypto.Cipher
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateKeySpec

internal class RSA @Throws(Exception::class)
constructor(mod: ByteArray, exp: ByteArray) {
    private var key: RSAPrivateKey? = null
    private var cipher: Cipher? = null

    init {
        createPrivateKey(mod, exp)
    }

    @Throws(Exception::class)
    fun createPrivateKey(module: ByteArray, exp: ByteArray) {
        val modulus = BigInteger(module.toHex(), 16)
        val privateExp = BigInteger(exp.toHex(), 16)
        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
        val pubKeySpec = RSAPrivateKeySpec(modulus, privateExp)
        cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        this.key = keyFactory.generatePrivate(pubKeySpec) as RSAPrivateKey
    }

    @Throws(Exception::class)
    fun encrypt(data: ByteArray): ByteArray {
        cipher!!.init(Cipher.DECRYPT_MODE, key)
        return cipher!!.doFinal(data)
    }
}