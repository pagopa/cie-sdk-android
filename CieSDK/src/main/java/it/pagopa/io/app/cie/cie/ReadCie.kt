package it.pagopa.io.app.cie.cie

import android.nfc.TagLostException
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.cie.commands.readCieAtr
import it.pagopa.io.app.cie.nfc.NfcReading

internal class ReadCie(
    private val onTransmit: OnTransmit,
    private val readingInterface: NfcReading
) {
    fun read(pin: String) {
        try {
            cieCommands = CieCommands(onTransmit)
            cieCommands!!.getServiceID()
            cieCommands!!.startSecureChannel(pin)
            val certificate = cieCommands!!.readCertCie()
            readingInterface.read<ByteArray>(certificate)
        } catch (e: Exception) {
            when (e) {
                is CieSdkException -> onTransmit.error(e.getError())
                is TagLostException -> onTransmit.error(NfcError.TAG_LOST)
                else -> onTransmit.error(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = e.message.orEmpty()
                })
            }
        }
    }

    fun readCieAtr() {
        try {
            // no need to impl companion as here we don't sign nothing
            val commands = CieCommands(onTransmit)
            val cieType = commands.readCieAtr()
            readingInterface.read<ByteArray>(cieType)
        } catch (e: Exception) {
            when (e) {
                is CieSdkException -> onTransmit.error(e.getError())
                is TagLostException -> onTransmit.error(NfcError.TAG_LOST)
                else -> onTransmit.error(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = e.message.orEmpty()
                })
            }
        }
    }

    companion object {
        internal var cieCommands: CieCommands? = null
    }
}