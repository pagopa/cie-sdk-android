package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.ApduSecureMessageManager
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.OnTransmit
import it.pagopa.io.app.cie.cie.ReadFileManager
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.pace.general_authenticate.Phase1
import it.pagopa.io.app.cie.pace.general_authenticate.Phase2
import it.pagopa.io.app.cie.pace.general_authenticate.Phase3
import it.pagopa.io.app.cie.pace.general_authenticate.model.Phase3Model
import it.pagopa.io.app.cie.pace.general_authenticate.model.SessionValues
import it.pagopa.io.app.cie.pace.utils.addBcIfNeeded

internal class PaceManager(private val onTransmit: OnTransmit) {
    init {
        addBcIfNeeded()
    }

    @Throws(Exception::class)
    fun doPACE(can: String): Phase3Model {
        val commands = CieCommands(onTransmit)
        CieLogger.i("PACE-DEBUG", "=== START doPACE ===")
        CieLogger.i("PACE-DEBUG", "CAN: $can")
        val gaPhase1 = Phase1(commands).execute(can)
        val gaPhase2 = Phase2(commands).execute(gaPhase1)
        //mutual authentication, throws Exception if not ok, else gives back mac and enc keys
        val sessionValues = Phase3(commands).execute(gaPhase2)
        CieLogger.i("PACE-DEBUG", "Mutual authentication OK - PACE completed successfully!")
        CieLogger.i("PACE-DEBUG", "=== END doPACE ===")
        return sessionValues
    }

    fun retrieveValues(model: SessionValues): MRTDResponse {
        CieLogger.i("PACE-DEBUG", "=== RETRIEVING VALUES ===")
        val (sessionEnc, sessionMac) = model.encKey to model.macKey
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
        //TODO SOD AND DG11 are equal
        CieLogger.i("sodBytes", Utils.bytesToString(sodBytes))
        CieLogger.i("PACE-DEBUG", "=== Values retrieved!! ===")
        return MRTDResponse(dg1Bytes, dg11Bytes, sodBytes)
    }
}