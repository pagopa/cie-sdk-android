package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduManager
import it.pagopa.cie.cie.ApduSecureMessageManager
import it.pagopa.cie.cie.Asn1Tag
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.OnTransmit
import it.pagopa.cie.hexStringToByteArray
import it.pagopa.cie.nfc.Utils

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
        val secureMessageManager = ApduSecureMessageManager(onTransmit)
        val pairBack = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            verifyPIN,
            pin.toByteArray(),
            null
        )
        seq = pairBack.first
        val response = pairBack.second
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
                throw CieSdkException(NfcError.WRONG_PIN.apply {
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
        val secureMessageManager = ApduSecureMessageManager(onTransmit)
        seq = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            setKey,
            data,
            null
        ).first
        val signApdu = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00)
        val pairBack = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            signApdu,
            dataToSign,
            null
        )
        seq = pairBack.first
        val response = pairBack.second
        return response.response
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
        val secureMessageManager = ApduSecureMessageManager(onTransmit)
        var content = byteArrayOf()
        val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
        val fileId = byteArrayOf(hiByte(id), loByte(id))
        seq = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            selectFile,
            fileId,
            null
        ).first
        var cnt = 0
        var chunk = 256

        while (true) {
            val readFile = byteArrayOf(0x00, 0xb0.toByte(), hiByte(cnt), loByte(cnt))
            val pairBack = secureMessageManager.sendApduSM(
                seq,
                sessionEncryption,
                sessMac,
                readFile,
                byteArrayOf(),
                byteArrayOf(chunk.toByte())
            )
            seq = pairBack.first
            val response = pairBack.second
            var chn = response.response
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val pairBack = secureMessageManager.sendApduSM(
                    seq,
                    sessionEncryption,
                    sessMac,
                    readFile,
                    byteArrayOf(),
                    byteArrayOf(le)
                )
                seq = pairBack.first
                val respApdu = pairBack.second
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