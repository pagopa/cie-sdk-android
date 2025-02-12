package it.pagopa.cie.nfc

import it.pagopa.cie.cie.ReadCie

abstract class BaseNfcImpl {
    lateinit var readingInterface: NfcReading
    abstract fun connect(actionDone: () -> Unit)
    abstract val readCie: ReadCie
    abstract fun disconnect()
    fun transmit(challenge: String) {
        connect {
            readingInterface.onTransmit("connected")
            readCie.read(challenge) {
                disconnect()
            }
        }
    }
}