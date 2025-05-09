package it.pagopa.io.app.cie.cie.key_store

import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.ReadCie
import java.security.AlgorithmParameters
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.CipherSpi

internal class Cipher : CipherSpi() {
    override fun engineSetMode(mode: String?) {
    }

    override fun engineInit(opmode: Int, key: Key?, random: SecureRandom?) {
    }

    override fun engineInit(
        opmode: Int,
        key: Key?,
        params: AlgorithmParameterSpec?,
        random: SecureRandom?
    ) {
    }

    override fun engineInit(
        opmode: Int,
        key: Key?,
        params: AlgorithmParameters?,
        random: SecureRandom?
    ) {
    }

    override fun engineGetIV(): ByteArray {
        return ByteArray(0)
    }

    override fun engineDoFinal(input: ByteArray?, inputOffset: Int, inputLen: Int): ByteArray? {
        if (input == null) return byteArrayOf()
        return ReadCie.cieCommands?.sign(input, NfcEvent.SIGN_WITH_CIPHER)
    }

    override fun engineDoFinal(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
        output: ByteArray?,
        outputOffset: Int
    ): Int {
        return 0
    }

    override fun engineSetPadding(padding: String?) {
    }

    override fun engineGetParameters(): AlgorithmParameters? {
        return null
    }

    override fun engineUpdate(input: ByteArray?, inputOffset: Int, inputLen: Int): ByteArray {
        return ByteArray(0)
    }

    override fun engineUpdate(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
        output: ByteArray?,
        outputOffset: Int
    ): Int {
        return 0
    }

    override fun engineGetBlockSize(): Int {
        return 0
    }

    override fun engineGetOutputSize(inputLen: Int): Int {
        return 0
    }
}
