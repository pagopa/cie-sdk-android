package it.pagopa.io.app.cie_example.ui.model

import it.pagopa.io.app.cie.nis.InternalAuthenticationResponse
import it.pagopa.io.app.cie.pace.MRTDResponse
import kotlinx.serialization.Serializable

@Serializable
data class NisAndPaceReadDto(
    val nisAuth: String,
    val mrz: String,
    val paceReadString: String
) {
    fun mailMessage(): String {
        return """
            $nisAuth
            $paceReadString
        """.trimIndent()
    }
}

fun Pair<InternalAuthenticationResponse, MRTDResponse>.toNisAndPaceReadDto(): NisAndPaceReadDto {
    val (nisAuth, paceRead) = this
    val paceReadDto = paceRead.toPaceReadDto()
    return NisAndPaceReadDto(nisAuth.toStringUi(), paceReadDto.mrz, paceReadDto.paceReadString)
}
