package it.pagopa.io.app.cie.cie

import android.nfc.TagLostException
import it.pagopa.io.app.cie.IntAuthMRTDResponse
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.cie.commands.readCieAtr
import it.pagopa.io.app.cie.nfc.NfcReading
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.nis.InternalAuthenticationResponse
import it.pagopa.io.app.cie.pace.MRTDResponse
import it.pagopa.io.app.cie.pace.PaceManager

internal class ReadCie(
    private val onTransmit: OnTransmit, private val readingInterface: NfcReading
) {
    private fun readIncludingExceptions(whatToDo: () -> Unit) {
        try {
            whatToDo.invoke()
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

    private fun provideNisAuth(challenge: String): InternalAuthenticationResponse {
        val commands = CieCommands(onTransmit)
        val nis: ByteArray = commands.readNis()
        val pubKeyBytes = ReadFileManager(onTransmit).readFile(
            0x1005,
            false,
            NfcEvent.READ_PUBLIC_KEY,
            NfcEvent.READ_PUBLIC_KEY
        )
        val sod = ReadFileManager(onTransmit).readFile(
            0x1006,
            false,
            NfcEvent.READ_SOD_SELECT,
            NfcEvent.READ_SOD
        )
        val challengeSigned = commands.intAuth(challenge)
        if (challengeSigned == null || challengeSigned.isEmpty()) {
            throw CieSdkException(NfcError.NIS_NO_CHALLENGE_SIGNED)
        } else {
            return InternalAuthenticationResponse(nis, pubKeyBytes, sod, challengeSigned)
        }
    }

    private fun providePaceFlow(can: String): MRTDResponse {
        val paceManager = PaceManager(onTransmit)
        val sessionValues = paceManager.doPACE(can)
        return paceManager.retrieveValues(sessionValues)
    }

    fun read(pin: String) {
        readIncludingExceptions {
            cieCommands = CieCommands(onTransmit)
            cieCommands!!.getServiceID()
            cieCommands!!.startSecureChannel(pin)
            val certificate = cieCommands!!.readCertCie()
            readingInterface.read(certificate)
        }
    }

    fun readCieAtr() {
        readIncludingExceptions {
            val commands = CieCommands(onTransmit)
            val cieType = commands.readCieAtr()
            readingInterface.read(cieType)
        }
    }

    fun readNis(challenge: String) {
        readIncludingExceptions {
            val nisAuth = this.provideNisAuth(challenge)
            readingInterface.read(nisAuth)
        }
    }

    fun doPace(can: String) {
        readIncludingExceptions {
            val paceRead = this.providePaceFlow(can)
            readingInterface.read(paceRead)
        }
    }

    fun nisAndPace(challenge: String, can: String) {
        readIncludingExceptions {
            val paceRead = this.providePaceFlow(can)
            onTransmit.sendCommand(
                Utils.hexStringToByteArray("00a40000"),
                NfcEvent.SELECT_EMPTY
            )
            val nisAuth = this.provideNisAuth(challenge)
            readingInterface.read(IntAuthMRTDResponse(nisAuth, paceRead))
        }
    }

    companion object {
        internal var cieCommands: CieCommands? = null
    }
}