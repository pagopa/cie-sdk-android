package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduManager
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.Asn1Tag
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.OnTransmit
import it.pagopa.cie.cie.unsignedToBytes
import it.pagopa.cie.hexStringToByteArray
import it.pagopa.cie.nfc.Algorithms
import it.pagopa.cie.nfc.Utils
import kotlin.experimental.or

internal class CieCommands(internal val onTransmit: OnTransmit) {
    internal var sessionEncryption: ByteArray = byteArrayOf()
    internal var sessMac: ByteArray = byteArrayOf()
    internal var dhG: ByteArray = byteArrayOf()
    internal var dhP: ByteArray = byteArrayOf()
    internal var dhQ: ByteArray = byteArrayOf()
    internal var dappPubKey: ByteArray = byteArrayOf()
    internal var dappModule: ByteArray = byteArrayOf()
    internal var caModule: ByteArray = byteArrayOf()
    internal var caPubExp: ByteArray = byteArrayOf()
    internal var baExtAuthPrivExp: ByteArray = byteArrayOf()
    internal var caCar: ByteArray = byteArrayOf()
    internal var caAid: ByteArray = byteArrayOf()
    internal var caPrivExp: ByteArray = byteArrayOf()
    internal var seq: ByteArray = byteArrayOf()
    internal var dhpubKey: ByteArray = byteArrayOf()
    internal var dhICCpubKey: ByteArray = byteArrayOf()
    private fun hiByte(b: Int): Byte {
        return (b shr 8 and 0xFF).toByte()
    }

    private fun loByte(b: Int): Byte {
        return b.toByte()
    }

    /**
     * @return il certificato dell'utente
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readCertCie(): ByteArray {
        CieLogger.i("COMMAND", "readCieCertificate()")
        return readFileSM(0x1003)
    }

    @Throws(Exception::class)
    fun getServiceID(): String {
        CieLogger.i("COMMAND", "getServiceID()")
        this.onTransmit.sendCommand(
            "00A4040C0DA0000000308000000009816001".hexStringToByteArray(),
            "Service ID 1"
        )
        this.onTransmit.sendCommand(
            "00A4040406A00000000039".hexStringToByteArray(),
            "Service ID 2"
        )
        this.onTransmit.sendCommand(
            "00a40204021001".hexStringToByteArray(),
            "Service ID 3"
        )
        val response = this.onTransmit.sendCommand(
            "00b000000c".hexStringToByteArray(),
            "Service ID GET RESPONSE"
        )
        if (response.swHex != "9000") {
            throw CieSdkException(NfcError.NOT_A_CIE)
        }
        return Utils.bytesToHex(response.response)
    }

    /**
     *
     * @param pin verifica il pin dell'utente
     * @return restituisce il numero di tentativi possibili
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun verifyPin(pin: String): Int {
        CieLogger.i("COMMANDS", "verifyPin()")
        if (pin.length != 8) {
            throw CieSdkException(NfcError.PIN_REGEX_NOT_VALID)
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
                throw CieSdkException(NfcError.PIN_BLOCKED)
            else
                throw CieSdkException(NfcError.PIN_NOT_RIGHT.apply {
                    this.numberOfAttempts = numberOfAttempts
                })
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
        //selectAidCie()
        val asn1 = Asn1Tag.parse(dappKey, false)
        dappModule = asn1!!.child(0).data
        if (dappModule.isNotEmpty())
            CieLogger.i("dappModule", "${dappModule[0].toInt() == 0}")
        else
            CieLogger.i("dappModule", "is empty")
        while (dappModule[0].toInt() == 0)
            dappModule = Utils.getSub(dappModule, 1, dappModule.size - 1)
        dappPubKey = asn1.child(1).data
        if (dappPubKey.isNotEmpty())
            CieLogger.i("dappPubKey", "${dappPubKey[0].toInt() == 0}")
        else
            CieLogger.i("dappPubKey", "is empty")
        while (dappPubKey[0].toInt() == 0)
            dappPubKey = Utils.getSub(dappPubKey, 1, dappPubKey.size - 1)
    }

    @Throws(Exception::class)
    fun sign(dataToSign: ByteArray): ByteArray? {
        CieLogger.i("COMMAND", "SIGN")
        val setKey = byteArrayOf(0x00, 0x22, 0x41, 0xA4.toByte())
        val val02 = byteArrayOf(0x02)
        val keyId = byteArrayOf(0x81.toByte())//CIE_KEY_Sign_ID
        val data = Utils.appendByteArray(Utils.asn1Tag(val02, 0x80), Utils.asn1Tag(keyId, 0x84))
        sendApduSM(setKey, data, null)
        val signApdu = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00)
        val response = sendApduSM(signApdu, dataToSign, null)
        return response.response
    }

    @Throws(Exception::class)
    fun sendApduSM(
        head: ByteArray,
        data: ByteArray,
        le: ByteArray?
    ): ApduResponse {
        var apduSm: ByteArray
        val ds = data.size
        if (ds < unsignedToBytes(0xe7)) {
            apduSm = byteArrayOf()
            apduSm = Utils.appendByteArray(apduSm, head)
            apduSm = Utils.appendByte(apduSm, ds.toByte())
            apduSm = Utils.appendByteArray(apduSm, data)
            if (le != null)
                apduSm = Utils.appendByteArray(apduSm, le)
            apduSm = sm(sessionEncryption, sessMac, apduSm)
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
                apduSm = sm(sessionEncryption, sessMac, apduSm)

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
                ApduResponse(byteArrayOf(), byteArrayOf(sw.toByte()))
        }
        return respSM(sessionEncryption, sessMac, elaboraResp)
    }

    @Throws(Exception::class)
    private fun setIndex(vararg argomenti: Int): Int {
        var tmpIndex = 0
        var tmpSegno: Int
        for (i in argomenti.indices) {
            if (kotlin.math.sign(argomenti[i].toFloat()) < 0) {
                tmpSegno = argomenti[i] and 0xFF
                tmpIndex += tmpSegno
            } else
                tmpIndex += argomenti[i]
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
                            Utils.appendByteArray(seq, encObj),
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
            return ApduResponse(resp, Utils.intToByteArray(sw))
        }
        return ApduResponse(Utils.intToByteArray(sw))
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
        var content = byteArrayOf()
        val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
        val fileId = byteArrayOf(hiByte(id), loByte(id))
        val apduManager = ApduManager(onTransmit)
        apduManager.sendApdu(selectFile, fileId, null, "SELECT FOR READ FILE")
        var cnt = 0
        val chunk = 256
        while (true) {
            val readFile = byteArrayOf(0x00, 0xb0.toByte(), hiByte(cnt), loByte(cnt))
            val response =
                apduManager.sendApdu(
                    readFile,
                    byteArrayOf(),
                    byteArrayOf(chunk.toByte()),
                    "reading file.."
                )
            var chn = response.response
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                CieLogger.i("ENTERING", "response.swInt shr 8!!")
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val respApdu =
                    apduManager.sendApdu(
                        readFile,
                        byteArrayOf(),
                        byteArrayOf(le),
                        "response from reading"
                    )
                chn = respApdu.response
            }
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
            } else {
                if (response.swHex == "0x6282")
                    content = Utils.appendByteArray(content, chn)
                break
            }
        }
        return content
    }

    @Throws(Exception::class)
    private fun readFileSM(id: Int): ByteArray {
        CieLogger.i("ON COMMAND", "readfileSM()")
        var content = byteArrayOf()
        val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
        val fileId = byteArrayOf(hiByte(id), loByte(id))

        sendApduSM(selectFile, fileId, null)
        var cnt = 0
        var chunk = 256

        while (true) {
            val readFile = byteArrayOf(0x00, 0xb0.toByte(), hiByte(cnt), loByte(cnt))
            val response = sendApduSM(readFile, byteArrayOf(), byteArrayOf(chunk.toByte()))
            var chn = response.response
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val respApdu = sendApduSM(readFile, byteArrayOf(), byteArrayOf(le))
                chn = respApdu.response
            }
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
                chunk = 256
            } else {
                if (response.swHex == "6282") {
                    content = Utils.appendByteArray(content, chn)
                } else if (response.swHex != "6b00") {
                    return content
                }
                break
            }
        }
        return content
    }
}