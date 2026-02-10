package it.pagopa.io.app.cie.nfc

import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.ReadCie

internal abstract class BaseNfcImpl {
    lateinit var readingInterface: NfcReading
    abstract fun connect(
        isoDepTimeout: Int,
        doSound: Boolean,
        onTagDiscovered: () -> Unit,
        actionDone: () -> Unit
    )

    abstract val readCie: ReadCie
    abstract fun disconnect()
    fun transmit(
        isoDepTimeout: Int,
        doSound: Boolean,
        pin: String,
        onTagDiscovered: () -> Unit,
    ) {
        connect(isoDepTimeout, doSound, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.read(pin)
        }
    }

    fun readCieAtr(
        isoDepTimeout: Int,
        doSound: Boolean,
        onTagDiscovered: () -> Unit
    ) {
        connect(isoDepTimeout, doSound, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.readCieAtr()
        }
    }

    fun readNis(
        challenge: String,
        isoDepTimeout: Int,
        doSound: Boolean,
        onTagDiscovered: () -> Unit
    ) {
        connect(isoDepTimeout, doSound, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.readNis(challenge)
        }
    }

    fun doPace(
        can: String,
        isoDepTimeout: Int,
        doSound: Boolean,
        onTagDiscovered: () -> Unit
    ) {
        connect(isoDepTimeout, doSound, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.doPace(can)
        }
    }

    fun doNisAndPace(
        challenge: String,
        can: String,
        isoDepTimeout: Int,
        doSound: Boolean,
        onTagDiscovered: () -> Unit
    ) {
        connect(isoDepTimeout, doSound, onTagDiscovered) {
            readingInterface.onTransmit(NfcEvent.CONNECTED)
            readCie.nisAndPace(challenge, can)
        }
    }
}