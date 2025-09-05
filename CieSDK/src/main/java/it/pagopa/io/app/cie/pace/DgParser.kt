package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Utils

class DgParser {
    fun parseDG1(dg1Bytes: ByteArray): String {
        CieLogger.i("DG1Bytes", Utils.bytesToString(dg1Bytes))
        val tlvList = TlvReader(dg1Bytes).readAll()
        val mrzBytes = tlvList.find { it.tag == 0x5F1F }?.value
            ?: throw IllegalArgumentException("MRZ non trovata in DG1")
        return String(mrzBytes, Charsets.UTF_8)
    }

    fun parseDG2(dg2Bytes: ByteArray): ByteArray {
        CieLogger.i("DG2Bytes", Utils.bytesToString(dg2Bytes))
        val tlvList = TlvReader(dg2Bytes).readAll()
        return tlvList.find { it.tag == 0x5F2E }?.value
            ?: throw IllegalArgumentException("Foto non trovata in DG2")
    }
}