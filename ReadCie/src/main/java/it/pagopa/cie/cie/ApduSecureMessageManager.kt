package it.pagopa.cie.cie

import it.pagopa.cie.nfc.Algorithms
import it.pagopa.cie.nfc.Utils
import kotlin.experimental.or

internal class ApduSecureMessageManager(private val onTransmit: OnTransmit) {
    /**It Sends a secure message APDU
     * @return [Pair] of the manipulated sequence and the [ApduResponse]*/
    @Throws(Exception::class)
    fun sendApduSM(
        sequence: ByteArray,
        sessionEncryption: ByteArray,
        sessMac: ByteArray,
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?
    ): Pair<ByteArray, ApduResponse> {
        var seq = sequence
        var apduSm: ByteArray
        val ds = data.size
        if (ds < unsignedToBytes(0xe7)) {
            apduSm = byteArrayOf()
            apduSm = Utils.appendByteArray(apduSm, head)
            apduSm = Utils.appendByte(apduSm, ds.toByte())
            apduSm = Utils.appendByteArray(apduSm, data)
            if (le != null)
                apduSm = Utils.appendByteArray(apduSm, le)
            val pair = sm(seq, sessionEncryption, sessMac, apduSm)
            seq = pair.first
            apduSm = pair.second
            var apduResponse = onTransmit.sendCommand(apduSm, "sending apduSM")
            return getRespSM(seq, apduResponse, sessionEncryption, sessMac)
        } else {
            var i = 0
            val cla = head[0]
            while (true) {
                apduSm = byteArrayOf()
                val s = Utils.getSub(data, i, (data.size - i).coerceAtMost(0xE7))
                i += s.size
                if (i != data.size)
                    head[0] = (cla or 0x10)
                else
                    head[0] = cla
                if (s.isNotEmpty()) {
                    apduSm = byteArrayOf()
                    apduSm = Utils.appendByteArray(apduSm, head)
                    apduSm = Utils.appendByte(apduSm, s.size.toByte())
                    apduSm = Utils.appendByteArray(apduSm, s)
                    if (le != null)
                        apduSm = Utils.appendByteArray(apduSm, byteArrayOf())
                } else {
                    apduSm = byteArrayOf()
                    apduSm = Utils.appendByteArray(apduSm, head)
                    if (le != null)
                        apduSm = Utils.appendByteArray(apduSm, le)
                }
                val pair = sm(seq, sessionEncryption, sessMac, apduSm)
                seq = pair.first
                apduSm = pair.second
                val response = onTransmit.sendCommand(apduSm, "sending apduSM")
                val resp = getRespSM(seq, response, sessionEncryption, sessMac)
                if (i == data.size) {
                    return resp
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun sm(
        sequence: ByteArray,
        keyEnc: ByteArray,
        keyMac: ByteArray,
        apdu: ByteArray
    ): Pair<ByteArray, ByteArray> {
        Utils.increment(sequence)
        val smHead = Utils.getLeft(apdu, 4)
        smHead[0] = smHead[0] or 0x0C
        var calcMac = Utils.getIsoPad(Utils.appendByteArray(sequence, smHead))
        val smMac: ByteArray
        var dataField = byteArrayOf()
        var doob: ByteArray

        if (apdu[4].toInt() != 0 && apdu.size > 5) {
            //encript la parte di dati
            val d1 = Utils.getIsoPad(Utils.getSub(apdu, 5, apdu[4].toInt()))
            val enc = Algorithms.desEnc(keyEnc, d1)
            doob = if (apdu[1].toInt() and 1 == 0) {
                Utils.asn1Tag(Utils.appendByteArray(byteArrayOf(0x01), enc), 0x87)
            } else Utils.asn1Tag(enc, 0x85)
            calcMac = Utils.appendByteArray(calcMac, doob)
            dataField = Utils.appendByteArray(dataField, doob)
        }
        if (apdu.size == 5 || apdu.size == apdu[4] + 6)
        //--
        { // ' se c'è un le
            doob = Utils.asn1Tag(byteArrayOf(apdu[apdu.size - 1]), 0x97.toByte().toInt())
            calcMac = Utils.appendByteArray(calcMac, doob)
            dataField = Utils.appendByteArray(dataField, doob)
        }
        val d1 = Utils.getIsoPad(calcMac)
        smMac = Algorithms.macEnc(keyMac, d1)
        val tmp = Utils.asn1Tag(smMac, 0x8e)
        dataField = Utils.appendByteArray(dataField, tmp)
        return sequence to Utils.appendByte(
            Utils.appendByteArray(
                Utils.appendByteArray(
                    smHead, byteArrayOf(dataField.size.toByte())
                ), dataField
            ), 0x00.toByte()
        )
    }

    @Throws(Exception::class)
    private fun respSM(
        sequence: ByteArray,
        keyEnc: ByteArray,
        keySig: ByteArray,
        resp: ByteArray
    ): Pair<ByteArray, ApduResponse> {
        Utils.increment(sequence)
        // cerco il tag 87
        var index = setIndex(0)
        var encData: ByteArray = byteArrayOf()
        var encObj = byteArrayOf()
        var dataObj: ByteArray = byteArrayOf()

        var sw = 0
        do {
            if (resp[index].compareTo(0x99.toByte()) == 0) {
                if (resp[index + 1].compareTo(0x02.toByte()) != 0)
                    throw CieSdkException(NfcError.VERIFY_SM_DATA_OBJECT_LENGTH)
                dataObj = Utils.getSub(resp, index, 4)
                sw = resp[index + 2].toInt().shl(8) or resp[index + 3].toInt()
                index = setIndex(index, 4)//index += 4;
                continue
            }
            if (resp[index].compareTo(0x8e.toByte()) == 0) {
                val calcMac = Algorithms.macEnc(
                    keySig,
                    Utils.getIsoPad(
                        Utils.appendByteArray(
                            Utils.appendByteArray(sequence, encObj),
                            dataObj
                        )
                    )
                )
                index = setIndex(index, 1)//index++;
                if (resp[index].compareTo(0x08.toByte()) != 0)
                    throw CieSdkException(NfcError.VERIFY_SM_MAC_LENGTH)
                index = setIndex(index, 1)//index++;
                if (!calcMac.contentEquals(Utils.getSub(resp, index, 8)))
                    throw CieSdkException(NfcError.VERIFY_SM_NOT_SAME_MAC)
                index = setIndex(index, 8)//index += 8;
                continue
            }
            if (resp[index] == 0x87.toByte()) {
                if (Utils.unsignedToBytes(resp[index + 1]) > Utils.unsignedToBytes(0x80.toByte())) {

                    var lgn = 0
                    val llen = Utils.unsignedToBytes(resp[index + 1]) - 0x80
                    if (llen == 1)
                        lgn = Utils.unsignedToBytes(resp[index + 2])
                    if (llen == 2)
                        lgn = resp[index + 2].toInt().shl(8) or resp[index + 3].toInt()
                    encObj = Utils.getSub(resp, index, llen + lgn + 2)
                    encData = Utils.getSub(
                        resp,
                        index + llen + 3,
                        lgn - 1
                    ) // ' levo il padding indicator
                    index = setIndex(index, llen, lgn, 2)//index += llen + lgn + 2;
                } else {
                    encObj = Utils.getSub(resp, index, resp[index + 1] + 2)
                    encData = Utils.getSub(
                        resp,
                        index + 3,
                        resp[index + 1] - 1
                    ) // ' levo il padding indicator
                    index =
                        setIndex(index, resp[index + 1].toInt(), 2) //index += resp[index + 1] + 2;
                }
                continue
            } else if (resp[index].compareTo(0x85.toByte()) == 0) {
                if (resp[index + 1] > 0x80.toByte()) {
                    val llen = resp[index + 1] - 0x80
                    encObj = Utils.getSub(resp, index, llen + 2)
                    encData =
                        Utils.getSub(resp, index + llen + 2, 0) // ' levo il padding indicator
                    index = setIndex(index, llen, 0, 2)//index += llen + lgn + 2;
                } else {
                    encObj = Utils.getSub(resp, index, resp[index + 1] + 2)
                    encData = Utils.getSub(resp, index + 2, resp[index + 1].toInt())
                    index =
                        setIndex(index, resp[index + 1].toInt(), 2) //index += resp[index + 1] + 2;

                }
                continue
            } else
                throw CieSdkException(NfcError.NOT_EXPECTED_SM_TAG)
            //index = index + resp[index + 1] + 1;
        } while (index < resp.size)

        if (encData.isNotEmpty()) {
            var resp = Algorithms.desDec(keyEnc, encData)
            resp = Utils.isoRemove(resp)
            return sequence to ApduResponse(resp, Utils.intToByteArray(sw))
        }
        return sequence to ApduResponse(Utils.intToByteArray(sw))
    }

    @Throws(Exception::class)
    private fun getRespSM(
        sequence: ByteArray,
        responseTmp: ApduResponse,
        sessionEncryption: ByteArray,
        sessMac: ByteArray
    ): Pair<ByteArray, ApduResponse> {
        var responseTmp = responseTmp
        var elaboraResp = byteArrayOf()
        if (responseTmp.response.isNotEmpty())
            elaboraResp = Utils.appendByteArray(elaboraResp, responseTmp.response)
        val sw = responseTmp.swInt
        while (true) {
            if (Utils.byteCompare(sw shr 8, 0x61) == 0) {
                val ln = (sw and 0xff).toByte()
                if (ln.toInt() != 0) {
                    val apdu = byteArrayOf(0x00, 0xc0.toByte(), 0x00, 0x00, ln)
                    responseTmp = onTransmit.sendCommand(apdu, "GETTING TEMPORARY SM RESP")
                    elaboraResp = Utils.appendByteArray(elaboraResp, responseTmp.response)
                    if (responseTmp.swInt == 0x9000)
                        break
                    if (Utils.byteCompare(responseTmp.swInt shr 8, 0x61) != 0) {
                        break
                    }
                } else {
                    val apdu = byteArrayOf(0x00, 0xc0.toByte(), 0x00, 0x00, 0x00)
                    responseTmp = onTransmit.sendCommand(
                        apdu,
                        "GETTING TEMPORARY SM RESP with ln.toInt() ==0x"
                    )
                    elaboraResp = Utils.appendByteArray(elaboraResp, responseTmp.response)
                }
            } else return if (Utils.byteArrayCompare(
                    responseTmp.swByte,
                    byteArrayOf(0x90.toByte(), 0x00.toByte())
                ) ||
                Utils.byteArrayCompare(
                    responseTmp.swByte,
                    byteArrayOf(0x6b.toByte(), 0x00.toByte())
                ) ||
                Utils.byteArrayCompare(
                    responseTmp.swByte,
                    byteArrayOf(0x62.toByte(), 0x82.toByte())
                )
            ) {
                break
            } else
                return sequence to ApduResponse(byteArrayOf(), byteArrayOf(sw.toByte()))
        }
        return respSM(sequence, sessionEncryption, sessMac, elaboraResp)
    }

    @Throws(Exception::class)
    private fun setIndex(vararg argomenti: Int): Int {
        var tmpIndex = 0
        var tmpSign: Int
        for (i in argomenti.indices) {
            if (kotlin.math.sign(argomenti[i].toFloat()) < 0) {
                tmpSign = argomenti[i] and 0xFF
                tmpIndex += tmpSign
            } else
                tmpIndex += argomenti[i]
        }
        return tmpIndex
    }
}