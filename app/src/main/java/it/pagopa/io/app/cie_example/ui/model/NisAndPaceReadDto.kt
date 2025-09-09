package it.pagopa.io.app.cie_example.ui.model

import it.pagopa.io.app.cie.nis.InternalAuthenticationResponse
import it.pagopa.io.app.cie.pace.DgParser
import it.pagopa.io.app.cie.pace.MRTDResponse
import kotlinx.serialization.Serializable

@Serializable
data class NisAndPaceReadDto(
    val nisAuth: String,
    val mrz: String,
    val paceReadString: String
)

fun Pair<InternalAuthenticationResponse, MRTDResponse>.toNisAndPaceReadDto(): NisAndPaceReadDto {
    val (nisAuth, paceRead) = this
    val mrz = DgParser().parseDG1(paceRead.dg1)
    return NisAndPaceReadDto(nisAuth.toStringUi(), mrz, paceRead.toString())
}
