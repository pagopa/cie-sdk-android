package it.pagopa.cie.cie

interface OnTransmit {
    fun sendCommand(apdu: ByteArray, message: String): ApduResponse
    fun error(why: String)
}