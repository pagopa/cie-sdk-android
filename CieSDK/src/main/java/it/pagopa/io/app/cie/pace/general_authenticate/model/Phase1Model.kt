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
) : PhaseModel
