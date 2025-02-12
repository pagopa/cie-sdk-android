package it.pagopa.cie.cie

import android.util.Base64
import androidx.annotation.VisibleForTesting
import it.pagopa.cie.nfc.Utils
import kotlin.experimental.or
import kotlin.math.min

class CieCommands(private val onTransmit: OnTransmit) {
    fun intAuth(challenge: String): ByteArray? {
        val challengeByte: ByteArray = Base64.decode(challenge, Base64.DEFAULT)
        return signIntAuth(challengeByte, onTransmit)
    }

    private fun signIntAuth(dataToSign: ByteArray, onTransmit: OnTransmit): ByteArray? {
        onTransmit.sendCommand(
            byteArrayOf(0x00, 0x22, 0x41, 0xA4.toByte(), 0x06, 0x80.toByte(), 0x01, 0x02, 0x84.toByte(), 0x01, 0x83.toByte()),
            "setting auth"
        )
        val intAuthArray = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00, dataToSign.size.toByte())
        val command = onTransmit.sendCommand(
            Utils.appendByteArray(
                intAuthArray,
                Utils.appendByteArray(dataToSign, byteArrayOf(0x00))
            ), "authentication"
        )
        return when (command.swHex) {
            "9000" -> command.response
            "6100" -> onTransmit.sendCommand(byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00.toByte(), 0x00), "get response").response
            else -> null
        }
    }

    /**
     *Sends an APDU to select the CIE section of the card
     *@return: The response sent by the card
     */
    private fun selectCie(): ByteArray {
        return onTransmit.sendCommand(byteArrayOf(0x00, 0xa4.toByte(), 0x04, 0x0c, 0x06, 0xA0.toByte(), 0x00, 0x00, 0x00, 0x00, 0x39), "Select CIE").response
    }

    /**
     *Sends an APDU to select the IAS section of the CIE
     *@return: The response sent by the CIE
     */
    private fun selectIAS(): ByteArray {
        return onTransmit.sendCommand(
            byteArrayOf(
                0x00, 0xa4.toByte(), 0x04, 0x0c, 0x0d,
                0xA0.toByte(), 0x00, 0x00, 0x00, 0x30, 0x80.toByte(), 0x00, 0x00, 0x00, 0x09, 0x81.toByte(), 0x60, 0x01
            ), "select IAS"
        ).response
    }

    /**Reads the NIS value from the card and returns it
     *@return: The NIS value in form of [ByteArray]*/
    fun readNis(): ByteArray {
        selectIAS()
        selectCie()
        return onTransmit.sendCommand(Utils.hexStringToByteArray("00B081000C"), "reading NIS..").response
    }

    fun readPublicKey(): ByteArray {
        val first = onTransmit.sendCommand(Utils.hexStringToByteArray("00B0850000"), "reading public key 0").response
        val second = onTransmit.sendCommand(Utils.hexStringToByteArray("00B085E700"), "reading public key 1").response
        return Utils.appendByteArray(first, second)
    }

    fun readSodFileCompleted(): ByteArray {
        //Read SOD data record
        var idx = 0
        val size = 0xe4
        var sodIASData = ByteArray(0)
        var sodLoaded = false
        val apdu = byteArrayOf(0x00, 0xB1.toByte(), 0x00, 0x06)
        while (!sodLoaded) {
            //byte[] dataInput = { 0x54, (byte)0x02, Byte.parseByte(hexS.substring(0, 2), 16), Byte.parseByte(hexS.substring(2, 4), 16) };
            val dataInput = byteArrayOf(0x54, 0x02.toByte(), highByte(idx), lowByte(idx))
            val respApdu = sendApdu(apdu, dataInput, byteArrayOf(0xe7.toByte()), onTransmit, "reading sod")
            val chn: ByteArray = respApdu.response
            var offset = 2
            if (chn[1] > 0x80) offset = 2 + (chn[1] - 0x80)
            val buf = chn.copyOfRange(offset, chn.size)
            val combined = ByteArray(sodIASData.size + buf.size)
            sodIASData.copyInto(combined, 0, 0, sodIASData.size)
            buf.copyInto(combined, sodIASData.size, 0, buf.size)
            sodIASData = combined
            //idx += size;
            if (respApdu.swHex != "9000") {
                sodLoaded = true
            } else idx += size
        }
        return sodIASData
    }

    @Throws(Exception::class)
    @VisibleForTesting
    fun sendApdu(head: ByteArray, data: ByteArray, le: ByteArray?, onTransmit: OnTransmit, why: String): ApduResponse {
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
                val apduResponse: ApduResponse = onTransmit.sendCommand(apdu, why)
                if (apduResponse.swHex != "9000")
                    throw Exception("Errore apdu")
                if (i == data.size)
                    return getResp(apduResponse, onTransmit, why)
            }
        } else {
            if (data.isNotEmpty()) {
                apdu = Utils.appendByteArray(apdu, head)
                apdu = Utils.appendByte(apdu, data.size.toByte())
                apdu = Utils.appendByteArray(apdu, data)
            } else
                apdu = Utils.appendByteArray(apdu, head)
            if (le != null)
                apdu = Utils.appendByteArray(apdu, le)
            val response: ApduResponse = onTransmit.sendCommand(apdu, why)
            return getResp(response, onTransmit, why)
        }
    }

    @VisibleForTesting
    fun getResp(responseTmp: ApduResponse, onTransmit: OnTransmit, why: String): ApduResponse {
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
                    response = onTransmit.sendCommand(apdu, why)
                    elaborateResp = Utils.appendByteArray(elaborateResp, response.response)
                    return ApduResponse(Utils.appendByteArray(elaborateResp, Utils.hexStringToByteArray(response.swHex)))
                } else {
                    val apdu: ByteArray = Utils.appendByte(apduGetRsp, 0x00.toByte())
                    response = onTransmit.sendCommand(apdu, why)
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