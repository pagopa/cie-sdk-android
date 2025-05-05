package it.pagopa.io.app.cie.nfc

import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.ReadCie

internal abstract class BaseNfcImpl {
    lateinit var readingInterface: NfcReading
    abstract fun connect(
        isoDepTimeout: Int,
        onTagDiscovered: () -> Unit,
        actionDone: () -> Unit
    )

    abstract val readCie: ReadCie
    abstract fun disconnect()
    fun transmit(
        isoDepTimeout: Int,
        pin: String,
        onTagDiscovered: () -> Unit,
    ) {
        connect(isoDepTimeout, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.read(pin)
        }
    }

    fun readCieAtr(
        isoDepTimeout: Int,
        onTagDiscovered: () -> Unit
    ) {
        connect(isoDepTimeout, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.readCieAtr()
        }
    }
}