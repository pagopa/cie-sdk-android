package it.pagopa.io.app.cie.pace.general_authenticate.model

import it.pagopa.io.app.cie.pace.PACECipherAlgorithms
import it.pagopa.io.app.cie.pace.PaceOID
import it.pagopa.io.app.cie.pace.Tlv

internal data class Phase2Model(
    val cieEphemeralPublicKey: Tlv,
    val macKey: ByteArray,
    val encKey: ByteArray,
    val paceOid: PaceOID,
    val publicKeyBytes: ByteArray,
    val cipherAlgName: PACECipherAlgorithms
) : PhaseModel {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Phase2Model
        if (cieEphemeralPublicKey != other.cieEphemeralPublicKey) return false
        if (!macKey.contentEquals(other.macKey)) return false
        if (!encKey.contentEquals(other.encKey)) return false
        if (paceOid != other.paceOid) return false
        if (!publicKeyBytes.contentEquals(other.publicKeyBytes)) return false
        if (cipherAlgName != other.cipherAlgName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cieEphemeralPublicKey.hashCode()
        result = 31 * result + macKey.contentHashCode()
        result = 31 * result + encKey.contentHashCode()
        result = 31 * result + paceOid.hashCode()
        result = 31 * result + publicKeyBytes.contentHashCode()
        result = 31 * result + cipherAlgName.hashCode()
        return result
    }
}
