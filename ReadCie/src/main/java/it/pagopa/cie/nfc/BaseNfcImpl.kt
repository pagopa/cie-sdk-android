package it.pagopa.cie.nfc

import it.pagopa.cie.cie.NfcEvent
import it.pagopa.cie.cie.ReadCie

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

    fun readCieType(
        isoDepTimeout: Int,
        onTagDiscovered: () -> Unit
    ) {
        connect(isoDepTimeout, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.readCieType()
        }
    }
}