package it.pagopa.cie.cie

interface OnTransmit {
    fun sendCommand(apdu: ByteArray, nfcEvents: NfcEvent): ApduResponse
    fun error(error: NfcError)
}