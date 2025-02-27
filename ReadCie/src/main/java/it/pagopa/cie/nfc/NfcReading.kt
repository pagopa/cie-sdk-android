package it.pagopa.cie.nfc

import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent

internal interface NfcReading {
    fun onTransmit(message: NfcEvent)
    fun <T> read(element: T)
    fun error(error: NfcError)
}