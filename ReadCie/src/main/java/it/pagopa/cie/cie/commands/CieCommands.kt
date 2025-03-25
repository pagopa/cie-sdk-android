package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduSecureMessageManager
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent
import it.pagopa.cie.cie.OnTransmit
import it.pagopa.cie.cie.ReadFileManager
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
    internal var caCar: ByteArray = byteArrayOf()
    internal var caAid: ByteArray = byteArrayOf()
    internal var seq: ByteArray = byteArrayOf()
    internal var dhpubKey: ByteArray = byteArrayOf()
    internal var dhICCpubKey: ByteArray = byteArrayOf()

    /**
     * @return user certificate
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readCertCie(): ByteArray {
        CieLogger.i("COMMAND", "readCieCertificate()")
        val readFileManager = ReadFileManager(onTransmit)
        val pairBack = readFileManager.readFileSM(
            0x1003,
            seq,
            sessionEncryption,
            sessMac
        )
        seq = pairBack.first
        return pairBack.second
    }

    /**It selects IAS, CIE service id and reads file service id
     * @throws [CieSdkException] if tag is not a cie*/
    @Throws(CieSdkException::class)
    fun getServiceID(): String {
        CieLogger.i("COMMAND", "getServiceID()")
        this.onTransmit.sendCommand(
            "00A4040C0DA0000000308000000009816001".hexStringToByteArray(),
            NfcEvent.SELECT_IAS_SERVICE_ID
        )
        this.onTransmit.sendCommand(
            "00A4040406A00000000039".hexStringToByteArray(),
            NfcEvent.SELECT_CIE_SERVICE_ID
        )
        this.onTransmit.sendCommand(
            "00a40204021001".hexStringToByteArray(),
            NfcEvent.SELECT_READ_FILE_SERVICE_ID
        )
        val response = this.onTransmit.sendCommand(
            "00b000000c".hexStringToByteArray(),
            NfcEvent.READ_FILE_SERVICE_ID_RESPONSE
        )
        if (response.swHex != "9000") {
            throw CieSdkException(NfcError.NOT_A_CIE)
        }
        return Utils.bytesToHex(response.response)
    }

    /**
     *It starts a secure channel between card and device using user PIN
     * @param pin
     * @throws Exception
     */
    @Throws(Exception::class)
    fun startSecureChannel(pin: String) {
        this.selectIAS()
        this.selectCie()
        this.initDHParam()
        if (dappPubKey.isEmpty())
            this.readDappPubKey()
        this.initExtAuthKeyParam()
        this.dhKeyExchange()
        this.dApp()
        val numberOfAttempts = this.verifyPin(pin)
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
     * iso97962RSASHA256 = 0x41 //'41' ≡ algorithm identifier for signature using ISO 9796-2 scheme 1 – SHA-256
     * diffieHellmanRSASHA256 = 0x9B //'9B' ≡ DH asymmetric authentication algorithm (privacy) with SHA-256
     * clientServerRSAPKCS1 = 2 //'02' ≡ algorithm identifier for IFC/ICC authentication RSA PKCS#1 -SHA-1 with not data formatting*/
    @Throws(Exception::class)
    fun sign(dataToSign: ByteArray, event: NfcEvent): ByteArray? {
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
            null,
            event
        ).first
        val signApdu = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00)
        val pairBack = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            signApdu,
            dataToSign,
            null,
            event
        )
        seq = pairBack.first
        val response = pairBack.second
        return response.response
    }
}