package it.pagopa.io.app.cie_example.ui.model

import it.pagopa.io.app.cie.pace.DgParser
import it.pagopa.io.app.cie.pace.MRTDResponse
import kotlinx.serialization.Serializable

@Serializable
data class PaceReadDto(
    val mrz: String,
    val paceReadString: String
)

fun MRTDResponse.toPaceReadDto(): PaceReadDto {
    val mrz = DgParser().parseDG1(this.dg1)
    return PaceReadDto(mrz, this.toString())
}

