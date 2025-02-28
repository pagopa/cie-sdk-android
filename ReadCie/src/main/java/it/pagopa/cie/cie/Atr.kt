package it.pagopa.cie.cie

internal class Atr(private val atr: ByteArray) {
    fun getCieType(): CieType {
        CieType.entries.forEach {
            if (this.atr.isSubset(it.atr))
                return it
        }
        return CieType.UNKNOWN
    }

    private fun ByteArray.isSubset(second: ByteArray): Boolean {
        if (this.size < second.size) return false
        val sortedA = this.sortedArray()
        val sortedB = second.sortedArray()
        return sortedB.all { it in sortedA }
    }
}