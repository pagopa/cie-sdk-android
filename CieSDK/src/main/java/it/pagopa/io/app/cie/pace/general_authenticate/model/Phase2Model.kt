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
) : PhaseModel
