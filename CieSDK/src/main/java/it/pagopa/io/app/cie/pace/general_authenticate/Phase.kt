package it.pagopa.io.app.cie.pace.general_authenticate

import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.pace.general_authenticate.model.PhaseModel

internal abstract class Phase(val commands: CieCommands) {
    abstract fun <T> execute(input: T): PhaseModel
}