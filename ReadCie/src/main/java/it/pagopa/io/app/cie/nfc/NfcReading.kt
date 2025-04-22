package it.pagopa.io.app.cie.nfc

import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent

internal interface NfcReading {
    fun onTransmit(message: NfcEvent)
    fun <T> read(element: T)
    fun error(error: NfcError)
}