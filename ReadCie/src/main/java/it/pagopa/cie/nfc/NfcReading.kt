package it.pagopa.cie.nfc

interface NfcReading {
    fun onTransmit(message: String)
    fun <T> read(element: T)
    fun error(why:String)
}