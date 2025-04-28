package it.pagopa.io.app.cie.cie

interface OnTransmit {
    fun sendCommand(apdu: ByteArray, nfcEvents: NfcEvent): ApduResponse
    fun error(error: NfcError)
}