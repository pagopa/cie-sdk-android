package it.pagopa.cie.cie

import it.pagopa.cie.cie.commands.CieCommands
import it.pagopa.cie.nfc.NfcReading

internal class ReadCie(
    private val onTransmit: OnTransmit,
    private val readingInterface: NfcReading
) {
    fun read(pin: String) {
        try {
            val commands = CieCommands(onTransmit)
            commands.getServiceID()
            commands.startSecureChannel(pin)
            val certificate = commands.readCertCie()
            readingInterface.read<ByteArray>(certificate)
        } catch (e: Exception) {
            onTransmit.error("exception occurred: ${e.message}")
        }
    }
}