package it.pagopa.io.app.cie.nis

import it.pagopa.io.app.cie.nfc.Utils
import java.nio.charset.StandardCharsets

data class InternalAuthenticationResponse(
    val nis: ByteArray,
    val kpubIntServ: ByteArray,
    val sod: ByteArray,
    val challengeSigned: ByteArray
) {
    private fun ByteArray.toHex() = Utils.bytesToString(this)
    fun toStringUi(): String {
        return "Internal Authentication Response:\n nis: ${
            String(
                nis,
                StandardCharsets.UTF_8
            )
        };\n sod: ${sod.toHex()};\n kpubIntServ: ${kpubIntServ.toHex()};\n challengeSigned: ${
            challengeSigned.toHex()
        }"
    }

    override fun toString(): String {
        return "InternalAuthenticationResponse(nis=${nis.toHex()}, kpubIntServ=${kpubIntServ.toHex()}, sod=${sod.toHex()},challengeSigned=${challengeSigned.toHex()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InternalAuthenticationResponse
        if (nis.toHex() != other.nis.toHex()) return false
        if (kpubIntServ.toHex() != other.kpubIntServ.toHex()) return false
        if (sod.toHex() != other.sod.toHex()) return false
        if (challengeSigned.toHex() != other.challengeSigned.toHex()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = nis.contentHashCode()
        result = 31 * result + kpubIntServ.contentHashCode()
        result = 31 * result + sod.contentHashCode()
        result = 31 * result + challengeSigned.contentHashCode()
        return result
    }
}