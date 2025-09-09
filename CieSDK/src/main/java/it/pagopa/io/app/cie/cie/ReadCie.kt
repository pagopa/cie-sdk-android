package it.pagopa.io.app.cie.cie

import android.nfc.TagLostException
import android.util.Base64
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.NisAndPace
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.cie.commands.readCieAtr
import it.pagopa.io.app.cie.nfc.NfcReading
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.nis.NisAuthenticated
import it.pagopa.io.app.cie.pace.PaceManager
import it.pagopa.io.app.cie.pace.PaceRead
import java.nio.charset.StandardCharsets

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

    private fun provideNisAuth(challenge: String): NisAuthenticated {
        val commands = CieCommands(onTransmit)
        val efIntServ1001: ByteArray = commands.readNis()
        val nis = String(efIntServ1001, StandardCharsets.UTF_8)
        val bytes = commands.readPublicKey()
        val sod = commands.readSodFileCompleted()
        val challengeSigned = commands.intAuth(challenge)
        if (challengeSigned == null || challengeSigned.isEmpty()) {
            throw CieSdkException(NfcError.NIS_NO_CHALLENGE_SIGNED)
        } else {
            return NisAuthenticated(
                nis,
                Base64.encodeToString(bytes, Base64.DEFAULT),
                Base64.encodeToString(sod, Base64.DEFAULT),
                Base64.encodeToString(challengeSigned, Base64.DEFAULT)
            )
        }
    }

    private fun providePaceFlow(can: String): PaceRead {
        val paceManager = PaceManager(onTransmit)
        val (sessionEnc, sessionMac) = paceManager.doPACE(can)
        val seq = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val aid = byteArrayOf(
            0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
        )
        val head = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x0C)
        val (sequence, response) = ApduSecureMessageManager(onTransmit).sendApduSM(
            seq, sessionEnc, sessionMac, head, aid, null, NfcEvent.SELECT_PACE_SM
        )
        CieLogger.i("PACE-DEBUG", "SELECT_PACE_SM_RESPONSE:${response.swInt}")
        val readFileManager = ReadFileManager(onTransmit)
        val (newSequence, dg1Bytes) = readFileManager.readFileSM(
            0x0101, sequence, sessionEnc, sessionMac, true, NfcEvent.READING_DG1
        )
        CieLogger.i("dg1Bytes", Utils.bytesToString(dg1Bytes))
        val (newSequence1, dg11Bytes) = readFileManager.readFileSM(
            0x010B, newSequence, sessionEnc, sessionMac, true, NfcEvent.READING_DG11
        )
        CieLogger.i("DG11", Utils.bytesToString(dg11Bytes))
        val (_, sodBytes) = readFileManager.readFileSM(
            0x011B, newSequence1, sessionEnc, sessionMac, true, NfcEvent.READ_SOD_PACE
        )
        CieLogger.i("sodBytes", Utils.bytesToString(sodBytes))
        return PaceRead(dg1Bytes, dg11Bytes, sodBytes)
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
            readingInterface.read(NisAndPace(nisAuth, paceRead))
        }
    }

    companion object {
        internal var cieCommands: CieCommands? = null
    }
}