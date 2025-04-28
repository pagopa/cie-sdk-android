package it.pagopa.io.app.cie.nfc

import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent

interface NfcEvents {
    fun error(error: NfcError)
    fun event(event: NfcEvent)
}