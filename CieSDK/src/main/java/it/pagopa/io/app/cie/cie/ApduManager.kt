package it.pagopa.io.app.cie.cie

import androidx.annotation.VisibleForTesting
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Utils
import kotlin.experimental.or
import kotlin.math.min

/**8 PROTOCOL MANAGEMENT*/
internal class ApduManager(private val onTransmit: OnTransmit) {
    @Throws(Exception::class)
    fun sendApdu(
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?,
        event: NfcEvent
    ): ApduResponse {
        var apdu = byteArrayOf()

        if (data.size > STANDARD_APDU_SIZE) {
            var i = 0
            val cla = head[0]

            CieLogger.i("APDU_CHAINING", "DATA size=${data.size} > 255 → command chaining attivo")

            while (true) {
                apdu = byteArrayOf()
                val s: ByteArray = Utils.getSub(data, i, min(data.size - i, STANDARD_APDU_SIZE))
                i += s.size

                // Bit 5 del CLA = 1 se non siamo all'ultimo chunk
                if (i != data.size) {
                    head[0] = (cla or 0x10)
                    CieLogger.i(
                        "APDU_CHAINING",
                        "Chunk NON finale → CLA=${String.format("%02X", head[0])}"
                    )
                } else {
                    head[0] = cla
                    CieLogger.i(
                        "APDU_CHAINING",
                        "Ultimo chunk → CLA=${String.format("%02X", head[0])}"
                    )
                }

                apdu = Utils.appendByteArray(apdu, head)
                apdu = Utils.appendByte(apdu, s.size.toByte())
                apdu = Utils.appendByteArray(apdu, s)
                if (le != null) apdu = Utils.appendByteArray(apdu, le)

                // Log dettagliato chunk
                CieLogger.i(
                    "APDU_CHAINING",
                    "Chunk size=${s.size} totale inviato finora=$i/${data.size}\n" +
                            "APDU HEX: ${Utils.bytesToString(apdu)}"
                )

                val apduResponse: ApduResponse = onTransmit.sendCommand(apdu, event)

                CieLogger.i("APDU_CHAINING_RESP", "SW=${apduResponse.swHex}")

                if (apduResponse.swHex != "9000") {
                    CieLogger.e("APDU_ERROR", apduResponse.swHex)
                    throw CieSdkException(NfcError.APDU_ERROR)
                }

                if (i == data.size)
                    return getResp(apduResponse, event)
            }
        } else {
            CieLogger.i("SEND_APDU", "data.size <= STANDARD_APDU_SIZE(255) → invio diretto")
            if (data.isNotEmpty()) {
                apdu = Utils.appendByteArray(apdu, head)
                apdu = Utils.appendByte(apdu, data.size.toByte())
                apdu = Utils.appendByteArray(apdu, data)
            } else {
                apdu = Utils.appendByteArray(apdu, head)
            }
            if (le != null)
                apdu = Utils.appendByteArray(apdu, le)

            CieLogger.i("SENDING THIS APDU", Utils.bytesToString(apdu))
            val response: ApduResponse = onTransmit.sendCommand(apdu, event)
            val resp = getResp(response, event)
            CieLogger.i("RESPONSE", Utils.bytesToString(resp.response))
            CieLogger.i("RESPONSE SW HEX", resp.swHex)
            return resp
        }
    }

    fun sendApduExtended(
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?,
        event: NfcEvent
    ): ApduResponse {
        var apdu = byteArrayOf()
        apdu += head
        // Extended length: 0x00 + [len high] + [len low]
        val len = data.size
        apdu += byteArrayOf(0x00, (len shr 8).toByte(), (len and 0xFF).toByte())
        apdu += data
        if (le != null) apdu += le

        CieLogger.i("SEND_APDU_EXTENDED", Utils.bytesToString(apdu))
        val response = onTransmit.sendCommand(apdu, event)
        val resp = getResp(response, event)
        CieLogger.i("RESPONSE_EXTENDED", Utils.bytesToString(resp.response))
        CieLogger.i("RESPONSE_SW_HEX_EXTENDED", resp.swHex)
        return resp
    }

    private val apduHeadGetResp = byteArrayOf(0x00.toByte(), 0xc0.toByte(), 0x00, 0x00)

    /**8.6.5 GET RESPONSE of IAS ECC Rev 1.0.1*/
    @VisibleForTesting
    fun getResp(responseTmp: ApduResponse, event: NfcEvent): ApduResponse {
        var responseTmpHere: ApduResponse = responseTmp
        var response: ApduResponse
        val resp: ByteArray = responseTmp.response
        var sw: Int = responseTmp.swInt
        var elaborateResp: ByteArray = byteArrayOf()
        if (resp.isNotEmpty()) elaborateResp = Utils.appendByteArray(elaborateResp, resp)
        val apduGetRsp: ByteArray = apduHeadGetResp
        //8.6.4 Command returning more than 256 bytes
        while (true) {
            // if bytes are still available
            if (Utils.byteCompare((sw shr 8), 0x61) == 0) {
                val ln: Byte = (sw and 0xff).toByte()
                if (ln.toInt() != 0) {
                    //we've ended to read
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
                    //still bytes to read
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

    companion object {
        const val STANDARD_APDU_SIZE = 255
    }
}