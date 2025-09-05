package it.pagopa.io.app.cie.cie

import android.nfc.TagLostException
import android.util.Base64
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.cie.commands.readCieAtr
import it.pagopa.io.app.cie.nfc.NfcReading
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.nis.NisAuthenticated
import it.pagopa.io.app.cie.pace.DgParser
import it.pagopa.io.app.cie.pace.PaceManager
import it.pagopa.io.app.cie.pace.PaceRead
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal class ReadCie(
    private val onTransmit: OnTransmit, private val readingInterface: NfcReading
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

    fun readNis(challenge: String) {
        try {
            val commands = CieCommands(onTransmit)
            val efIntServ1001: ByteArray = commands.readNis()
            val nis = String(efIntServ1001, StandardCharsets.UTF_8)
            val bytes = commands.readPublicKey()
            val asn1Tag: Asn1Tag? = try {
                Asn1Tag.parse(bytes, false)
            } catch (_: Exception) {
                null
            }
            val a5noHash = if (asn1Tag != null) {
                Utils.bytesToString(
                    MessageDigest.getInstance("SHA-256")
                        .digest(Utils.getLeft(bytes, asn1Tag.endPos.toInt()))
                )
            } else ""
            val sod = commands.readSodFileCompleted()
            val challengeSigned = commands.intAuth(challenge)
            if (challengeSigned == null || challengeSigned.isEmpty()) {
                onTransmit.error(NfcError.NIS_NO_CHALLENGE_SIGNED)
            } else {
                readingInterface.read(
                    NisAuthenticated(
                        nis,
                        Base64.encodeToString(bytes, Base64.DEFAULT),
                        a5noHash,
                        Base64.encodeToString(sod, Base64.DEFAULT),
                        Base64.encodeToString(challengeSigned, Base64.DEFAULT)
                    )
                )
            }
        } catch (e: Exception) {
            onTransmit.error(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = e.message.orEmpty()
            })
        }
    }

    fun doPace(can: String) {
        try {
            val paceManager = PaceManager(onTransmit)
            val (sessionEnc, sessionMac) = paceManager.doPACE(can)
            val seq = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            val aid = byteArrayOf(
                0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
            )
            val head = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x0C)
            val (sequence, response) = ApduSecureMessageManager(onTransmit).sendApduSM(
                seq,
                sessionEnc,
                sessionMac,
                head,
                aid,
                null,
                NfcEvent.SELECT_PACE_SM
            )
            CieLogger.i("PACE-DEBUG", "SELECT_PACE_SM_RESPONSE:${response.swInt}")
            val readFileManager = ReadFileManager(onTransmit)
            val dgParser = DgParser()
            val (newSequence, dg1Bytes) = readFileManager.readFileSM(
                0x0101, sequence, sessionEnc, sessionMac, true
            )
            //dg11: 0x010B
            //sod: 0x011B
            val (_, dg2Bytes) = readFileManager.readFileSM(
                0x0102, newSequence, sessionEnc, sessionMac, true
            )
            val mrz = dgParser.parseDG1(dg1Bytes)
            val photoBytes = dgParser.parseDG2(dg2Bytes)
            //val photoFile = File(filesDir, "foto.jp2")
            //photoFile.writeBytes(photoBytes)
            // dg1, dg11, sod
            readingInterface.read(
                PaceRead(mrz, photoBytes)
            )
        } catch (e: Exception) {
            onTransmit.error(NfcError.GENERAL_EXCEPTION.apply {
                this.msg = e.message.orEmpty()
            })
        }
    }

    companion object {
        internal var cieCommands: CieCommands? = null
    }
}