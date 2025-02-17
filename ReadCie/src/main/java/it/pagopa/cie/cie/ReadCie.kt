package it.pagopa.cie.cie

import it.pagopa.cie.cie.commands.CieCommands
import it.pagopa.cie.nfc.NfcReading

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
            if (e is CieSdkException) {
                onTransmit.error(e.getError())
            } else {
                onTransmit.error(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = e.message.orEmpty()
                })
            }
        }
    }

    companion object {
        internal var cieCommands: CieCommands? = null
    }
}