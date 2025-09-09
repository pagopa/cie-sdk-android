package it.pagopa.io.app.cie

import it.pagopa.io.app.cie.nis.InternalAuthenticationResponse
import it.pagopa.io.app.cie.pace.MRTDResponse

data class IntAuthMRTDResponse(
    val internalAuthentication: InternalAuthenticationResponse,
    val mrtd: MRTDResponse
) {
    override fun toString(): String {
        return "INT_AUTH:\n$internalAuthentication\neMRTD:\n$mrtd"
    }

    fun toTerminalString(): String {
        return "INT_AUTH:\n\t$internalAuthentication\neMRTD:\n\t$mrtd"
    }
}