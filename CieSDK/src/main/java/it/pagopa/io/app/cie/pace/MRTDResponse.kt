package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.nfc.Utils

data class MRTDResponse(
    val dg1: ByteArray,
    val dg2: ByteArray,
    val dg11: ByteArray,
    val sod: ByteArray
) {

    override fun toString(): String {
        val dg1Hex = Utils.bytesToString(dg1)
        val dg2Hex = Utils.bytesToString(dg2)
        val dg11Hex = Utils.bytesToString(dg11)
        val sodHex = Utils.bytesToString(sod)

        return "dg1:\n${dg1Hex}\ndg2:\n${dg2Hex}\ndg11:\n${dg11Hex}\nsod:\n${sodHex}"
    }

    fun toTerminalString(): String {
        val dg1Hex = Utils.bytesToString(dg1)
        val dg2Hex = Utils.bytesToString(dg2)
        val dg11Hex = Utils.bytesToString(dg11)
        val sodHex = Utils.bytesToString(sod)

        return "dg1:\n\t${dg1Hex}\ndg2:\n\t${dg2Hex}\ndg11:\n\t${dg11Hex}\nsod:\n\t${sodHex}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MRTDResponse

        if (!dg1.contentEquals(other.dg1)) return false
        if (!dg2.contentEquals(other.dg2)) return false
        if (!dg11.contentEquals(other.dg11)) return false
        if (!sod.contentEquals(other.sod)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dg1.contentHashCode()
        result = 31 * result + dg2.contentHashCode()
        result = 31 * result + dg11.contentHashCode()
        result = 31 * result + sod.contentHashCode()
        return result
    }
}