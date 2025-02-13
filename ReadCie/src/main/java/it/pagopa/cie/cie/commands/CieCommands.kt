package it.pagopa.cie.cie.commands

import android.util.Base64
import androidx.annotation.VisibleForTesting
import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.Asn1Tag
import it.pagopa.cie.cie.OnTransmit
import it.pagopa.cie.cie.highByte
import it.pagopa.cie.cie.lowByte
import it.pagopa.cie.cie.unsignedToBytes
import it.pagopa.cie.nfc.Algorithms
import it.pagopa.cie.nfc.Utils
import kotlin.experimental.or
import kotlin.math.min

internal class CieCommands(internal val onTransmit: OnTransmit) {
    internal var sessEnc: ByteArray = byteArrayOf()
    internal var sessMac: ByteArray = byteArrayOf()
    internal var dh_g: ByteArray = byteArrayOf()
    internal var dh_p: ByteArray = byteArrayOf()
    internal var dh_q: ByteArray = byteArrayOf()
    internal var dappPubKey: ByteArray = byteArrayOf()
    internal var dappModule: ByteArray = byteArrayOf()
    internal var caModule: ByteArray = byteArrayOf()
    internal var caPubExp: ByteArray = byteArrayOf()
    internal var baExtAuth_PrivExp: ByteArray = byteArrayOf()
    internal var caCar: ByteArray = byteArrayOf()
    internal var caAid: ByteArray = byteArrayOf()
    internal var caPrivExp: ByteArray = byteArrayOf()
    internal var seq: ByteArray = byteArrayOf()
    internal var dh_pubKey: ByteArray = byteArrayOf()
    internal var dh_ICCpubKey: ByteArray = byteArrayOf()
    private fun HIBYTE(b: Int): Byte {
        return (b shr 8 and 0xFF).toByte()
    }

    private fun LOBYTE(b: Int): Byte {
        return b.toByte()
    }

    fun intAuth(challenge: String): ByteArray? {
        val challengeByte: ByteArray = Base64.decode(challenge, Base64.DEFAULT)
        return signIntAuth(challengeByte)
    }

    /**
     *
     * @param pin verifica il pin dell'utente
     * @return restituisce il numero di tentativi possibili
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun verifyPin(pin: String): Int {
        CieLogger.i("COOMANDS", "verifyPin()")
        if (pin.length != 8) {
            throw Exception("pin not valid")
        }
        val verifyPIN = byteArrayOf(0x00, 0x20, 0x00, 0x81.toByte())
        val response = sendApduSM(verifyPIN, pin.toByteArray(), null)
        val nt = Utils.bytesToHex(response.swByte)
        return when {
            nt.equals("9000", ignoreCase = true) -> 3
            nt.equals("ffc2", ignoreCase = true) -> 2
            nt.equals("ffc1", ignoreCase = true) -> 1
            else -> 0
        }
    }

    /**
     * inizializza un canale sicuro tra carta e dispositivo passando il pin dell'utente
     * @param pin
     * @throws Exception
     */
    @Throws(Exception::class)
    fun startSecureChannel(pin: String) {
        this.selectIAS()
        this.selectCie()
        this.initDHParam()
        if (dappPubKey.isEmpty())
            readDappPubKey()
        this.initExtAuthKeyParam()
        this.dhKeyExchange()
        this.dApp()
        val numberOfAttempts = verifyPin(pin)
        if (numberOfAttempts < 3) {
            if (numberOfAttempts == 0)
                throw Exception("pin blocked")
            else
                throw Exception(numberOfAttempts.toString())
        }
    }


    /**
     * recupera la chiave per la Internal Authentication
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun readDappPubKey() {
        CieLogger.i("Command", "readDappPubKey()")
        val dappKey: ByteArray = readFile(0x1004)
        dappModule = byteArrayOf()
        if (this.dappPubKey.isNotEmpty())
            throw Exception("public key is not empty")
        //selectAidCie()
        val asn1 = Asn1Tag.Companion.parse(dappKey, false)
        dappModule = asn1!!.child(0).data
        while (dappModule[0].toInt() == 0)
            dappModule = Utils.getSub(dappModule, 1, dappModule.size - 1)
        dappPubKey = asn1.child(1).data

        while (dappPubKey[0].toInt() == 0)
            dappPubKey = Utils.getSub(dappPubKey, 1, dappPubKey.size - 1)
    }

    private fun signIntAuth(dataToSign: ByteArray): ByteArray? {
        onTransmit.sendCommand(
            byteArrayOf(
                0x00,
                0x22,
                0x41,
                0xA4.toByte(),
                0x06,
                0x80.toByte(),
                0x01,
                0x02,
                0x84.toByte(),
                0x01,
                0x83.toByte()
            ),
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
            "6100" -> onTransmit.sendCommand(
                byteArrayOf(
                    0x00,
                    0xC0.toByte(),
                    0x00,
                    0x00.toByte(),
                    0x00
                ), "get response"
            ).response

            else -> null
        }
    }

    @Throws(Exception::class)
    fun sign(dataToSign: ByteArray): ByteArray? {
        val setKey = byteArrayOf(0x00, 0x22, 0x41, 0xA4.toByte())
        val val02 = byteArrayOf(0x02)
        val keyId = byteArrayOf(0x81.toByte())//CIE_KEY_Sign_ID
        val data = Utils.appendByteArray(Utils.asn1Tag(val02, 0x80), Utils.asn1Tag(keyId, 0x84))
        sendApduSM(setKey, data, null)
        val signApdu = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00)
        val response = sendApduSM(signApdu, dataToSign, null)
        return response.response
    }


    /**Reads the NIS value from the card and returns it
     *@return: The NIS value in form of [ByteArray]*/
    fun readNis(): ByteArray {
        selectIAS()
        selectCie()
        return onTransmit.sendCommand(
            Utils.hexStringToByteArray("00B081000C"),
            "reading NIS.."
        ).response
    }

    fun readPublicKey(): ByteArray {
        val first = onTransmit.sendCommand(
            Utils.hexStringToByteArray("00B0850000"),
            "reading public key 0"
        ).response
        val second = onTransmit.sendCommand(
            Utils.hexStringToByteArray("00B085E700"),
            "reading public key 1"
        ).response
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
            val respApdu =
                sendApdu(apdu, dataInput, byteArrayOf(0xe7.toByte()), "reading sod")
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
    fun sendApdu(
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?,
        why: String
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
                val apduResponse: ApduResponse = onTransmit.sendCommand(apdu, why)
                if (apduResponse.swHex != "9000")
                    throw Exception("Errore apdu")
                if (i == data.size)
                    return getResp(apduResponse, why)
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
            return getResp(response, why)
        }
    }

    @Throws(Exception::class)
    fun sendApduSM(
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?
    ): ApduResponse {
        //CieIDSdkLogger.log("sendApduSM()");
        var apduSm: ByteArray
        val ds = data.size
        if (ds < unsignedToBytes(0xe7)) {
            //CieIDSdkLogger.log("ds < unsignedToBytes((byte)0xe7): ");
            apduSm = byteArrayOf()
            apduSm = Utils.appendByteArray(apduSm, head)
            apduSm = Utils.appendByte(apduSm, ds.toByte())
            apduSm = Utils.appendByteArray(apduSm, data)
            if (le != null)
                apduSm = Utils.appendByteArray(apduSm, le)
            //CieIDSdkLogger.log("apduSm:  " + apduSm);
            apduSm = sm(sessEnc, sessMac, apduSm)
            var apduResponse = onTransmit.sendCommand(apduSm, "sending apduSM")
            apduResponse = getRespSM(apduResponse)
            return apduResponse
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
                apduSm = sm(sessEnc, sessMac, apduSm)

                var response = onTransmit.sendCommand(apduSm, "sending apduSM")
                response = getRespSM(response)
                if (i == data.size) {
                    return response
                }
            }

        }
    }

    @Throws(Exception::class)
    private fun getRespSM(responseTmp: ApduResponse): ApduResponse {
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
                    responseTmp = onTransmit.sendCommand(apdu, "")
                    elaboraResp = Utils.appendByteArray(elaboraResp, responseTmp.response)
                    if (responseTmp.swInt == 0x9000)
                        break
                    if (Utils.byteCompare(responseTmp.swInt shr 8, 0x61) != 0) {
                        break
                    }
                } else {
                    val apdu = byteArrayOf(0x00, 0xc0.toByte(), 0x00, 0x00, 0x00)
                    responseTmp = onTransmit.sendCommand(apdu, "")
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
                ApduResponse(byteArrayOf(), byteArrayOf(sw.toByte()))
        }
        return respSM(sessEnc, sessMac, elaboraResp)
    }

    @Throws(Exception::class)
    private fun setIndex(vararg argomenti: Int): Int {
        var tmpIndex = 0
        var tmpSegno: Int
        for (i in argomenti.indices) {
            if (Math.signum(argomenti[i].toFloat()) < 0) {
                tmpSegno = argomenti[i] and 0xFF
                tmpIndex += tmpSegno
            } else
                tmpIndex += argomenti[i]
            //System.out.print("sommo: " +  tmpIndex+" , ");
        }
        return tmpIndex
    }

    @Throws(Exception::class)
    private fun respSM(keyEnc: ByteArray, keySig: ByteArray, resp: ByteArray): ApduResponse {
        Utils.increment(seq)
        // cerco il tag 87
        var index = setIndex(0)
        var encData: ByteArray = byteArrayOf()
        var encObj = byteArrayOf()
        var dataObj: ByteArray = byteArrayOf()

        var sw = 0
        do {
            if (resp[index].compareTo(0x99.toByte()) == 0) {
                if (resp[index + 1].compareTo(0x02.toByte()) != 0)
                    throw Exception("Errore nella verifica del SM - lunghezza del DataObject")
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
                            Utils.appendByteArray(seq, encObj),
                            dataObj
                        )
                    )
                )
                index = setIndex(index, 1)//index++;
                if (resp[index].compareTo(0x08.toByte()) != 0)
                    throw Exception("Errore nella verifica del SM - lunghezza del MAC errata")
                index = setIndex(index, 1)//index++;
                if (!calcMac.contentEquals(Utils.getSub(resp, index, 8)))
                    throw Exception("Errore nella verifica del SM - MAC non corrispondente")
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
                if (resp[index + 1].compareTo(0x80.toByte()) > 0) {
                    var lgn = 0
                    val llen = resp[index + 1] - 0x80
                    if (llen == 1)
                        lgn = resp[index + 2].toInt()
                    if (llen == 2)
                        lgn = resp[index + 2].toInt().shl(8) or resp[index + 3].toInt()
                    encObj = Utils.getSub(resp, index, llen + lgn + 2)
                    encData =
                        Utils.getSub(resp, index + llen + 2, lgn) // ' levo il padding indicator
                    index = setIndex(index, llen, lgn, 2)//index += llen + lgn + 2;
                } else {
                    encObj = Utils.getSub(resp, index, resp[index + 1] + 2)
                    encData = Utils.getSub(resp, index + 2, resp[index + 1].toInt())
                    index =
                        setIndex(index, resp[index + 1].toInt(), 2) //index += resp[index + 1] + 2;

                }
                continue
            } else
                throw Exception("Tag non previsto nella risposta in SM")
            //index = index + resp[index + 1] + 1;
        } while (index < resp.size)

        if (encData.isNotEmpty()) {
            var resp = Algorithms.desDec(keyEnc, encData)
            resp = Utils.isoRemove(resp)
            return ApduResponse(resp, Utils.intToByteArray(sw))
        }
        return ApduResponse(Utils.intToByteArray(sw))
    }

    @VisibleForTesting
    fun getResp(responseTmp: ApduResponse, why: String): ApduResponse {
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
                    return ApduResponse(
                        Utils.appendByteArray(
                            elaborateResp,
                            Utils.hexStringToByteArray(response.swHex)
                        )
                    )
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

    @Throws(Exception::class)
    private fun sm(keyEnc: ByteArray, keyMac: ByteArray, apdu: ByteArray): ByteArray {
        Utils.increment(seq)
        val smHead = Utils.getLeft(apdu, 4)
        smHead[0] = smHead[0] or 0x0C
        var calcMac = Utils.getIsoPad(Utils.appendByteArray(seq, smHead))
        val smMac: ByteArray
        var dataField = byteArrayOf()
        var doob: ByteArray
        //CieIDSdkLogger.log("calcMac: " + AppUtil.bytesToHex(calcMac));

        if (apdu[4].toInt() != 0 && apdu.size > 5) {
            //encript la parte di dati
            val d1 = Utils.getIsoPad(Utils.getSub(apdu, 5, apdu[4].toInt()))
            //CieIDSdkLogger.log("d1: "+ AppUtil.bytesToHex(d1));
            val enc = Algorithms.desEnc(keyEnc, d1)
            //CieIDSdkLogger.log("enc: "+ AppUtil.bytesToHex(enc));
            doob = if (apdu[1].toInt() and 1 == 0) {
                Utils.asn1Tag(Utils.appendByteArray(byteArrayOf(0x01), enc), 0x87)
            } else
                Utils.asn1Tag(enc, 0x85)
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
        return Utils.appendByte(
            Utils.appendByteArray(
                Utils.appendByteArray(
                    smHead,
                    byteArrayOf(dataField.size.toByte())
                ), dataField
            ), 0x00.toByte()
        )

    }

    @Throws(Exception::class)
    private fun readFile(id: Int): ByteArray {
        //CieIDSdkLogger.log("readFile()");
        var content = byteArrayOf()
        val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
        val fileId = byteArrayOf(HIBYTE(id), LOBYTE(id))

        sendApdu(selectFile, fileId, null, "SELECT FOR READ FILE")

        var cnt = 0
        var chunk = 256

        while (true) {
            val readFile = byteArrayOf(0x00, 0xb0.toByte(), HIBYTE(cnt), LOBYTE(cnt))
            val response =
                sendApdu(readFile, byteArrayOf(), byteArrayOf(chunk.toByte()), "reading file..")
            var chn = response.response
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val respApdu =
                    sendApdu(readFile, byteArrayOf(), byteArrayOf(le), "response from reading")
                chn = respApdu.response
            }
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
                chunk = 256
            } else {
                if (response.swHex == "0x6282") {
                    content = Utils.appendByteArray(content, chn)
                } else if (response.swHex != "0x6b00") {
                    return content
                }
                break
            }
        }
        return content
    }
}