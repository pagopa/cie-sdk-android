package it.pagopa.cie.cie

import androidx.annotation.VisibleForTesting
import it.pagopa.cie.CieLogger
import it.pagopa.cie.nfc.Utils
import kotlin.experimental.or
import kotlin.math.min

internal class ApduManager(private val onTransmit: OnTransmit) {
    @Throws(Exception::class)
    fun sendApdu(
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?,
        event: NfcEvent
    ): ApduResponse {
        var apdu = byteArrayOf()
        if (data.size > 255) {
            var i = 0
            val cla = head[0]
            while (true) {
                apdu = byteArrayOf()
                val s: ByteArray = Utils.getSub(data, i, min(data.size - i, 255))
                i += s.size
                if (i != data.size) head[0] = (cla or 0x10) else head[0] = cla
                apdu = Utils.appendByteArray(apdu, head)
                apdu = Utils.appendByte(apdu, s.size.toByte())
                apdu = Utils.appendByteArray(apdu, s)
                if (le != null) apdu = Utils.appendByteArray(apdu, le)
                val apduResponse: ApduResponse = onTransmit.sendCommand(apdu, event)
                if (apduResponse.swHex != "9000") {
                    CieLogger.e("APDU_ERROR", apduResponse.swHex)
                    throw CieSdkException(NfcError.APDU_ERROR)
                }
                if (i == data.size)
                    return getResp(apduResponse, event)
            }
        } else {
            CieLogger.i("SEND_APDU", "data.size <= 255")
            if (data.isNotEmpty()) {
                apdu = Utils.appendByteArray(apdu, head)
                apdu = Utils.appendByte(apdu, data.size.toByte())
                apdu = Utils.appendByteArray(apdu, data)
            } else
                apdu = Utils.appendByteArray(apdu, head)
            if (le != null)
                apdu = Utils.appendByteArray(apdu, le)
            val response: ApduResponse = onTransmit.sendCommand(apdu, event)
            return getResp(response, event)
        }
    }

    @VisibleForTesting
    fun getResp(responseTmp: ApduResponse, event: NfcEvent): ApduResponse {
        var responseTmpHere: ApduResponse = responseTmp
        var response: ApduResponse
        val resp: ByteArray = responseTmp.response
        var sw: Int = responseTmp.swInt
        var elaborateResp: ByteArray = byteArrayOf()
        if (resp.isNotEmpty()) elaborateResp = Utils.appendByteArray(elaborateResp, resp)
        val apduGetRsp: ByteArray = byteArrayOf(0x00.toByte(), 0xc0.toByte(), 0x00, 0x00)
        while (true) {
            if (Utils.byteCompare((sw shr 8), 0x61) == 0) {
                val ln: Byte = (sw and 0xff).toByte()
                if (ln.toInt() != 0) {
                    val apdu: ByteArray = Utils.appendByte(apduGetRsp, ln)
                    response = onTransmit.sendCommand(apdu, event)
                    elaborateResp = Utils.appendByteArray(elaborateResp, response.response)
                    return ApduResponse(
                        Utils.appendByteArray(
                            elaborateResp,
                            Utils.hexStringToByteArray(response.swHex)
                        )
                    )
                } else {
                    val apdu: ByteArray = Utils.appendByte(apduGetRsp, 0x00.toByte())
                    response = onTransmit.sendCommand(apdu, event)
                    sw = response.swInt
                    elaborateResp = Utils.appendByteArray(elaborateResp, response.response)
                    responseTmpHere = response
                }
            } else {
                return responseTmpHere
            }
        }
    }
}