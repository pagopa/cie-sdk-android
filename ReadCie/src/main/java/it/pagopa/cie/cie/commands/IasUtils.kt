package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduManager
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.ApduSecureMessageManager
import it.pagopa.cie.cie.Asn1Tag
import it.pagopa.cie.cie.Atr
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.CieType
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent
import it.pagopa.cie.cie.ReadFileManager
import it.pagopa.cie.nfc.Utils
import kotlin.Exception
import kotlin.jvm.Throws

internal fun CieCommands.readAtr() = ReadFileManager(onTransmit).readFile(0x2f01)
internal fun CieCommands.selectRoot(): ApduResponse {
    val selectMF = byteArrayOf(0x00, 0xa4.toByte(), 0x00, 0x00, 0x02, 0x3f, 0x00)
    return ApduManager(onTransmit)
        .sendApdu(
            selectMF,
            byteArrayOf(),
            null,
            NfcEvent.SELECT_ROOT
        )
}

@Throws(CieSdkException::class)
internal fun CieCommands.readCieType(): CieType {
    this.selectIAS()
    this.selectCie()
    val selectRoot = this.selectRoot()
    if (selectRoot.swHex != "9000") {
        CieLogger.i("SELECT ROOT", selectRoot.swHex)
        throw CieSdkException(NfcError.SELECT_ROOT_EXCEPTION)
    }
    return Atr(this.readAtr()).getCieType()
}

/**
 * recupera i parametri delle chiavi per external authentication
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.initExtAuthKeyParam() {
    CieLogger.i("COMMAND", "initExtAuthParam()")
    val getKeyDoup = byteArrayOf(0, 0xcb.toByte(), 0x3f, 0xff.toByte())
    val getKeyDuopData = byteArrayOf(
        0x4d,
        0x09,
        0x70,
        0x07,
        0xBF.toByte(),
        0xA0.toByte(),
        0x04,
        0x03,
        0x7F,
        0x49,
        0x80.toByte()
    )
    val response = ApduManager(onTransmit)
        .sendApdu(getKeyDoup, getKeyDuopData, null, NfcEvent.INIT_EXTERNAL_AUTHENTICATION)
    val asn1 = Asn1Tag.Companion.parse(response.response, true)
    caModule = asn1!!.child(0).child(0).childWithTagID(byteArrayOf(0x81.toByte()))!!.data
    //caPubExp = asn1.child(0).child(0).childWithTagID(byteArrayOf(0x82.toByte()))!!.data
    val caCha = asn1.child(0).child(0).childWithTagID(byteArrayOf(0x5f, 0x4c))!!.data
    val caChr = asn1.child(0).child(0).childWithTagID(byteArrayOf(0x5f, 0x20))!!.data
    caCar = Utils.getSub(caChr, 4)
    caAid = Utils.getLeft(caCha, 6)
}

/**
 * recupera la chiave per la Internal Authentication
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.readDappPubKey() {
    CieLogger.i("Command", "readDappPubKey()")
    val readFileManager = ReadFileManager(onTransmit)
    val dappKey: ByteArray = readFileManager.readFile(0x1004)
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

/**
 *
 * @param pin verifica il pin dell'utente
 * @return restituisce il numero di tentativi possibili
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.verifyPin(pin: String): Int {
    CieLogger.i("COMMANDS", "verifyPin()")
    if (!ciePinRegex.matches(pin)) {
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
        null,
        NfcEvent.VERIFY_PIN
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