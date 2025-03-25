package it.pagopa.cie.cie.commands

import android.util.Base64
import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.ApduSecureMessageManager
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent
import it.pagopa.cie.nfc.RSA
import it.pagopa.cie.nfc.Sha256
import it.pagopa.cie.nfc.Utils

/**
 * Device Authentication With privacy protection
 * contains ExtAuth e IntAuth
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.dApp() {
    CieLogger.i("COMMAND", "dApp()")
    // end entity certificate
    val psoVerifyAlgo = byteArrayOf(0x41)
    val shaOID: Byte = 0x04
    val shaSize = 32

    val module = defModule
    val pubExp = defPubExp
    val privExp = defPrivExp
    val snIFD = byteArrayOf(0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01)
    val CPI = 0x8A.toByte()
    val baseCHR = byteArrayOf(0x00, 0x00, 0x00, 0x00)
    var CHR = byteArrayOf()
    CHR = Utils.appendByteArray(CHR, baseCHR)
    CHR = Utils.appendByteArray(CHR, snIFD)
    var CHA = byteArrayOf()
    CHA = Utils.appendByteArray(CHA, caAid)
    CHA = Utils.appendByte(CHA, 0x01.toByte())
    val baseOID = byteArrayOf(0x2A, 0x81.toByte(), 0x22, 0xF4.toByte(), 0x2A, 0x02, 0x04, 0x01)
    var OID = byteArrayOf()
    OID = Utils.appendByteArray(OID, baseOID)
    OID = Utils.appendByte(OID, shaOID)
    var endEntityCert = byteArrayOf()
    endEntityCert = Utils.appendByte(endEntityCert, CPI)
    endEntityCert = Utils.appendByteArray(endEntityCert, caCar)
    endEntityCert = Utils.appendByteArray(endEntityCert, CHR)
    endEntityCert = Utils.appendByteArray(endEntityCert, CHA)
    endEntityCert = Utils.appendByteArray(endEntityCert, OID)
    endEntityCert = Utils.appendByteArray(endEntityCert, module)
    endEntityCert = Utils.appendByteArray(endEntityCert, pubExp)
    var d1 = Sha256.encrypt(endEntityCert)
    val ba99 = Utils.getLeft(endEntityCert, caModule.size - shaSize - 2)
    var toSign = byteArrayOf()
    toSign = Utils.appendByte(toSign, 0x6a.toByte())
    toSign = Utils.appendByteArray(toSign, ba99)
    toSign = Utils.appendByteArray(toSign, d1)
    toSign = Utils.appendByte(toSign, 0xbc.toByte())
    val rsa = RSA(caModule, caPrivExp)
    val certSign = rsa.encrypt(toSign)
    val pkRem = Utils.getSub(
        endEntityCert,
        caModule.size - shaSize - 2,
        endEntityCert.size - (caModule.size - shaSize - 2)
    )
    // end - end entity certificate
    //IAS ECC v1_0_1UK.pdf 7.2.6.1
    val tmp = Utils.asn1Tag(certSign, 0x5f37)
    val tmp1 = Utils.asn1Tag(pkRem, 0x5F38)
    val tmp2 = Utils.asn1Tag(caCar, 0x42)

    val cert =
        Utils.asn1Tag(Utils.appendByteArray(Utils.appendByteArray(tmp, tmp1), tmp2), 0x7f21)

    val selectKey = byteArrayOf(0x00, 0x22, 0x81.toByte(), 0xb6.toByte())

    val dataTmp = Utils.appendByteArray(
        Utils.asn1Tag(psoVerifyAlgo, 0x80),
        Utils.asn1Tag(byteArrayOf(0x84.toByte()), 0x83)
    )
    val secureMessageManager = ApduSecureMessageManager(onTransmit)
    // selecting key
    seq = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        selectKey,
        dataTmp,
        null,
        NfcEvent.SIGN1_SELECT
    ).first
    // verifying key
    val verifyCert = byteArrayOf(0x00, 0x2A, 0x00, 0xAE.toByte())
    seq = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        verifyCert,
        cert,
        null,
        NfcEvent.SIGN1_VERIFY_CERT
    ).first
    val setCHR = byteArrayOf(0x00, 0x22, 0x81.toByte(), 0xA4.toByte())
    CieLogger.i("SEQ BEFORE:", Base64.encodeToString(seq, Base64.DEFAULT))
    val asn1=Utils.asn1Tag(CHR, 0x83)
    CieLogger.i("Utils.asn1Tag(CHR, 0x83):", Base64.encodeToString(asn1, Base64.DEFAULT))
    val appPair = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        setCHR,
        asn1,
        null,
        NfcEvent.SET_CHALLENGE_RESPONSE
    )
    seq = appPair.first
    CieLogger.i("SEQ AFTER:", Base64.encodeToString(seq, Base64.DEFAULT))
    CieLogger.i("setCHR RESPONSE", "${appPair.second}")
    //IAS ECC v1_0_1UK.pdf 9.5.1 GET CHALLENGE
    val getChallenge = byteArrayOf(0x00.toByte(), 0x84.toByte(), 0x00.toByte(), 0x00.toByte())
    val chLen = byteArrayOf(8)
    val pairBack = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        getChallenge,
        byteArrayOf(),
        chLen,
        NfcEvent.GET_CHALLENGE_RESPONSE
    )
    seq = pairBack.first
    val challengeResp = pairBack.second
    val padSize = module.size - shaSize - 2
    val PRND = Utils.getRandomByte(padSize)
    var toHash = byteArrayOf()
    //IAS ECC v1_0_1UK.pdf 5.2.3.3.1 Protocol steps
    //PuK.IFD.DH = diffieHellmanPublicKey.modulus
    //SN.IFD = snIFD
    //RND.ICC = challenge
    //PuK.ICC.DH = iccPublicKey
    //h(PRND|PuK.IFD.DH|SN.IFD|RN D.ICC|PuK.ICC.DH|g|p|q)
    toHash = Utils.appendByteArray(toHash, PRND)
    toHash = Utils.appendByteArray(toHash, dhpubKey)
    toHash = Utils.appendByteArray(toHash, snIFD)
    toHash = Utils.appendByteArray(toHash, challengeResp.response)
    toHash = Utils.appendByteArray(toHash, dhICCpubKey)
    toHash = Utils.appendByteArray(toHash, dhG)
    toHash = Utils.appendByteArray(toHash, dhP)
    toHash = Utils.appendByteArray(toHash, dhQ)
    d1 = Sha256.encrypt(toHash)

    toSign = byteArrayOf()
    toSign = Utils.appendByte(toSign, 0x6a.toByte())
    toSign = Utils.appendByteArray(toSign, PRND)
    toSign = Utils.appendByteArray(toSign, d1)
    toSign = Utils.appendByte(toSign, 0xbc.toByte())

    val signResp: ByteArray
    val rsaCertKey = RSA(module, privExp)
    signResp = rsaCertKey.encrypt(toSign)
    //IAS ECC v1_0_1UK.pdf 9.5.5 EXTERNAL AUTHENTICATE for Role authentication
    var chResponse = byteArrayOf()
    chResponse = Utils.appendByteArray(chResponse, snIFD)
    chResponse = Utils.appendByteArray(chResponse, signResp)
    val resp: ApduResponse?
    val extAuth = byteArrayOf(0x00, 0x82.toByte(), 0x00, 0x00)
    seq = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        extAuth,
        chResponse,
        null,
        NfcEvent.EXTERNAL_AUTHENTICATION
    ).first
    val intAuth = byteArrayOf(0x00, 0x22, 0x41, 0xa4.toByte())
    val val82 = byteArrayOf(0x82.toByte())
    val pKdScheme = byteArrayOf(0x9b.toByte())

    val temp: ByteArray =
        Utils.appendByteArray(Utils.asn1Tag(val82, 0x84), Utils.asn1Tag(pKdScheme, 0x80))

    seq = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        intAuth,
        temp,
        null,
        NfcEvent.INTERNAL_AUTHENTICATION
    ).first
    //IAS ECC v1_0_1UK.pdf 5.2.3.5 Internal authentication of the ICC
    val rndIFD = Utils.getRandomByte(8)
    val giveRandom = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00)
    val pair = secureMessageManager.sendApduSM(
        seq,
        sessionEncryption,
        sessMac,
        giveRandom,
        rndIFD,
        null,
        NfcEvent.GIVE_RANDOM
    )
    seq = pair.first
    //SN.ICC | SIG.ICC
    resp = pair.second

    val snIcc = Utils.getSub(resp.response, 0, 8)
    val intAuthResp: ByteArray
    val rsaIntAuthKey = RSA(dappModule, dappPubKey)
    intAuthResp = rsaIntAuthKey.encrypt(Utils.getSub(resp.response, 8, resp.response.size - 8))

    if (intAuthResp[0].compareTo(0x6a.toByte()) != 0)
        throw CieSdkException(NfcError.CHIP_AUTH_ERROR)

    val prnD2 = Utils.getSub(intAuthResp, 1, intAuthResp.size - 32 - 2)
    val hashICC = Utils.getSub(intAuthResp, prnD2.size + 1, 32)
    var toHashIFD = byteArrayOf()
    toHashIFD = Utils.appendByteArray(toHashIFD, prnD2)
    toHashIFD = Utils.appendByteArray(toHashIFD, dhICCpubKey)
    toHashIFD = Utils.appendByteArray(toHashIFD, snIcc)
    toHashIFD = Utils.appendByteArray(toHashIFD, rndIFD)
    toHashIFD = Utils.appendByteArray(toHashIFD, dhpubKey)
    toHashIFD = Utils.appendByteArray(toHashIFD, dhG)
    toHashIFD = Utils.appendByteArray(toHashIFD, dhP)
    toHashIFD = Utils.appendByteArray(toHashIFD, dhQ)
    val calcHashIFD = Sha256.encrypt(toHashIFD)
    if (Utils.bytesToHex(calcHashIFD) != Utils.bytesToHex(hashICC))
        throw CieSdkException(NfcError.CHIP_AUTH_ERROR)
    if (intAuthResp[intAuthResp.size - 1].compareTo(0xbc.toByte()) != 0)
        throw CieSdkException(NfcError.CHIP_AUTH_ERROR)
    val ba888 = Utils.getRight(challengeResp.response, 4)
    val ba889 = Utils.getRight(rndIFD, 4)
    //The secure messaging for integrity for the subsequent command is computed with the correct starting value for
    //SSC = RND.ICC (4 least significant bytes) || RND.IFD (4 least significant bytes)
    seq = Utils.appendByteArray(ba888, ba889)
}
