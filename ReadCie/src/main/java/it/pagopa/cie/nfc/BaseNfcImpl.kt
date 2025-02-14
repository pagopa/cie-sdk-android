package it.pagopa.cie.nfc

import it.pagopa.cie.cie.ReadCie

internal abstract class BaseNfcImpl {
    lateinit var readingInterface: NfcReading
    abstract fun connect(isoDepTimeout: Int, actionDone: () -> Unit)
    abstract val readCie: ReadCie
    abstract fun disconnect()
    fun transmit(isoDepTimeout: Int, pin: String) {
        connect(isoDepTimeout) {
            readingInterface.onTransmit("connected")
            readCie.read(pin)
        }
    }
}