package it.pagopa.io.app.cie.pace.general_authenticate.model

import it.pagopa.io.app.cie.pace.KeyAgreementAlgorithm
import it.pagopa.io.app.cie.pace.PaceOID
import it.pagopa.io.app.cie.pace.Tlv
import java.security.KeyPair

internal data class Phase1Model(
    val kindOfAlgorithm: KeyAgreementAlgorithm,
    val keyPair: KeyPair,
    val ciePublicKeyMapping: Tlv,
    val decryptedNonce: ByteArray,
    val paceOid: PaceOID
) : PhaseModel {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Phase1Model

        if (kindOfAlgorithm != other.kindOfAlgorithm) return false
        if (keyPair != other.keyPair) return false
        if (ciePublicKeyMapping != other.ciePublicKeyMapping) return false
        if (!decryptedNonce.contentEquals(other.decryptedNonce)) return false
        if (paceOid != other.paceOid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kindOfAlgorithm.hashCode()
        result = 31 * result + keyPair.hashCode()
        result = 31 * result + ciePublicKeyMapping.hashCode()
        result = 31 * result + decryptedNonce.contentHashCode()
        result = 31 * result + paceOid.hashCode()
        return result
    }
}
