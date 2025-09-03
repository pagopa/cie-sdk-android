package it.pagopa.io.app.cie.pace.pace_model

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.pace.PaceOID
import it.pagopa.io.app.cie.pace.TlvReader
import it.pagopa.io.app.cie.pace.utils.decodeOid

internal data class PACEInfo(
    val paceRawValue: ByteArray,
    val paceValue: String,
    val version: Int,
    val parameterId: Int?
) {
    fun objIdentifier() = PaceOID(this.paceValue)

    companion object {
        @JvmStatic
        fun fromCardAccess(efData: ByteArray): List<PACEInfo> {
            val backList = ArrayList<PACEInfo>()
            var oidBytesSequenceRaw = TlvReader(efData).readRaw()
            oidBytesSequenceRaw.forEachIndexed { i, tlv ->
                CieLogger.i(
                    "RAW I TLV of $i",
                    "TAG: ${tlv.tag}\nLENGTH: ${tlv.length}\nVALUE: ${Utils.bytesToString(tlv.value)}"
                )
                oidBytesSequenceRaw = TlvReader(tlv.value).readRaw()
                oidBytesSequenceRaw.forEachIndexed { j, tlv ->
                    CieLogger.i(
                        "RAW J TLV of $i",
                        "\tTAG: ${tlv.tag}\nLENGTH: ${tlv.length}\nVALUE: ${Utils.bytesToString(tlv.value)}"
                    )
                    oidBytesSequenceRaw = TlvReader(tlv.value).readRaw()
                    if (oidBytesSequenceRaw.size >= 2) {
                        val rawValue = oidBytesSequenceRaw[0].value
                        val paceInfo = PACEInfo(
                            rawValue,
                            decodeOid(rawValue).first,
                            oidBytesSequenceRaw[1].value[0].toInt(),
                            if (oidBytesSequenceRaw.size > 2) oidBytesSequenceRaw[2].value[0].toInt() else null
                        )
                        CieLogger.i("PACE INFO", paceInfo.toString())
                        backList.add(paceInfo)
                    }
                }
            }
            return backList.toList()
        }
    }
}
