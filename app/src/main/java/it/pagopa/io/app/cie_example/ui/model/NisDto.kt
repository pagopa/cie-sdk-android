package it.pagopa.io.app.cie_example.ui.model

import it.pagopa.io.app.cie.nis.InternalAuthenticationResponse
import kotlinx.serialization.Serializable

@Serializable
data class NisDto(
    val nisAuth: String
) {
    fun mailMessage(): String {
        return """
            $nisAuth
        """.trimIndent()
    }
}

fun InternalAuthenticationResponse.toNisDto() = NisDto(this.toStringUi())
