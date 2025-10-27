package it.pagopa.io.app.cie.pace.general_authenticate.model

data class Phase3Model(
    val macKey: ByteArray,
    val encKey: ByteArray
) : PhaseModel {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Phase3Model
        if (!macKey.contentEquals(other.macKey)) return false
        if (!encKey.contentEquals(other.encKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = macKey.contentHashCode()
        result = 31 * result + encKey.contentHashCode()
        return result
    }
}

typealias SessionValues = Phase3Model
