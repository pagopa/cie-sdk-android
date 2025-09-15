package it.pagopa.io.app.cie_example.ui.model

import it.pagopa.io.app.cie.pace.MRTDResponse
import it.pagopa.io.app.cie_example.utils.parseMrz
import kotlinx.serialization.Serializable

@Serializable
data class PaceReadDto(
    val mrz: String,
    val paceReadString: String
)

fun MRTDResponse.toPaceReadDto(): PaceReadDto {
    val mrz = parseMrz(this.dg1)
    return PaceReadDto(mrz, this.toString())
}

