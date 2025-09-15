package it.pagopa.io.app.cie.pace.general_authenticate.model

data class Phase3Model(
    val macKey: ByteArray,
    val encKey: ByteArray
) : PhaseModel

typealias SessionValues = Phase3Model
