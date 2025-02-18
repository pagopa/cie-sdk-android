package it.pagopa.cie.nfc

import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent

interface NfcEvents {
    fun onTransmit(message: String)
    fun error(error: NfcError)
    fun event(event: NfcEvent)
}