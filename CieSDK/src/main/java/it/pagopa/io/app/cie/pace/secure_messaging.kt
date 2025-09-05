package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.nfc.Utils
import org.bouncycastle.crypto.digests.GeneralDigest
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.digests.SHA256Digest

internal enum class PACECipherAlgorithms {
    AES,
    DESede
}

internal enum class PACEDigestAlgorithms {
    SHA1,
    SHA256;

    fun computeHash(dataElements: List<ByteArray>): ByteArray {
        val digest: GeneralDigest = when (this) {
            SHA1 -> SHA1Digest()
            SHA256 -> SHA256Digest()
        }
        dataElements.forEach { digest.update(it, 0, it.size) }
        val out = ByteArray(digest.digestSize)
        digest.doFinal(out, 0)
        return out
    }
}

internal enum class SecureMessagingMode(val value: Byte) {
    ENC_MODE(0x1),
    MAC_MODE(0x2),
    PACE_MODE(0x3)
}

@OptIn(ExperimentalUnsignedTypes::class)
internal fun deriveKey(
    keySeed: ByteArray,
    cipherAlgName: PACECipherAlgorithms,
    digestAlgo: PACEDigestAlgorithms,
    keyLength: Int,
    nonce: ByteArray?,
    mode: SecureMessagingMode
): ByteArray {
    CieLogger.i("CAN IN BYTES", Utils.bytesToString(keySeed))
    // Array con i byte della modalità (0x00, 0x00, 0x00, mode.value)
    val modeArr = byteArrayOf(0x00, 0x00, 0x00, mode.value)

    // Lista degli elementi da passare all’hash
    val dataEls = mutableListOf<ByteArray>()
    dataEls.add(keySeed)
    nonce?.let {
        dataEls.add(it)
    }
    dataEls.add(modeArr)

    // Calcolo dell’hash
    val hashResult: ByteArray = digestAlgo.computeHash(dataEls.map { it })

    val keyBytes: ByteArray = when (cipherAlgName) {
        PACECipherAlgorithms.DESede -> {
            when (keyLength) {
                112, 128 -> {
                    // E (1-8), D (9-16), E (1-8)
                    val partE = hashResult.sliceArray(0 until 8)
                    val partD = hashResult.sliceArray(8 until 16)
                    partE + partD + partE
                }

                else -> throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = "Can only use DESede with 128-bit key length"
                })
            }
        }

        PACECipherAlgorithms.AES -> {
            when (keyLength) {
                128 -> hashResult.sliceArray(0 until 16)
                192 -> hashResult.sliceArray(0 until 24)
                256 -> hashResult.sliceArray(0 until 32)
                else -> throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = "Can only use AES with 128-bit, 192-bit or 256-bit length"
                })
            }
        }
    }

    return keyBytes
}