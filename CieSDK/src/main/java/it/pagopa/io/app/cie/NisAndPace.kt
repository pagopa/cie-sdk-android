package it.pagopa.io.app.cie

import it.pagopa.io.app.cie.nis.NisAuthenticated
import it.pagopa.io.app.cie.pace.PaceRead

data class NisAndPace(
    val nis: NisAuthenticated,
    val paceRead: PaceRead
) {
    override fun toString(): String {
        return "NIS:\n$nis\nPACE:\n$paceRead"
    }

    fun toTerminalString(): String {
        return "NIS:\n\t$nis\nPACE:\n\t$paceRead"
    }
}