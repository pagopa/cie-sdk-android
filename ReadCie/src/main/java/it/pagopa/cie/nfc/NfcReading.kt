package it.pagopa.cie.nfc

import it.pagopa.cie.cie.NfcError

interface NfcReading {
    fun onTransmit(message: String)
    fun <T> read(element: T)
    fun error(error: NfcError)
}