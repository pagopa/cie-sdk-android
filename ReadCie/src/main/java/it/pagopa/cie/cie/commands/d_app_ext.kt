package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.nfc.RSA
import it.pagopa.cie.nfc.Sha256
import it.pagopa.cie.nfc.Utils

/**
 * Device Authentication With privacy protection
 * contiene ExtAuth e IntAuth
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.dApp() {
    CieLogger.i("COMMAND", "dApp()")
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
    sendApduSM(selectKey, dataTmp, null)

    val verifyCert = byteArrayOf(0x00, 0x2A, 0x00, 0xAE.toByte())
    sendApduSM(verifyCert, cert, null)
    val setCHR = byteArrayOf(0x00, 0x22, 0x81.toByte(), 0xA4.toByte())
    sendApduSM(setCHR, Utils.asn1Tag(CHR, 0x83), null)

    val getChallenge = byteArrayOf(0x00.toByte(), 0x84.toByte(), 0x00.toByte(), 0x00.toByte())
    val chLen = byteArrayOf(8)
    val challengeResp = sendApduSM(getChallenge, byteArrayOf(), chLen)

    val padSize = module.size - shaSize - 2
    val PRND = Utils.getRandomByte(padSize)
    var toHash = byteArrayOf()
    toHash = Utils.appendByteArray(toHash, PRND)
    toHash = Utils.appendByteArray(toHash, dh_pubKey)
    toHash = Utils.appendByteArray(toHash, snIFD)
    toHash = Utils.appendByteArray(toHash, challengeResp.response)
    toHash = Utils.appendByteArray(toHash, dh_ICCpubKey)
    toHash = Utils.appendByteArray(toHash, dh_g)
    toHash = Utils.appendByteArray(toHash, dh_p)
    toHash = Utils.appendByteArray(toHash, dh_q)
    d1 = Sha256.encrypt(toHash)

    toSign = byteArrayOf()
    toSign = Utils.appendByte(toSign, 0x6a.toByte())
    toSign = Utils.appendByteArray(toSign, PRND)
    toSign = Utils.appendByteArray(toSign, d1)
    toSign = Utils.appendByte(toSign, 0xbc.toByte())

    val signResp: ByteArray
    val rsaCertKey = RSA(module, privExp)
    signResp = rsaCertKey.encrypt(toSign)

    var chResponse = byteArrayOf()
    chResponse = Utils.appendByteArray(chResponse, snIFD)
    chResponse = Utils.appendByteArray(chResponse, signResp)
    val resp: ApduResponse?
    val extAuth = byteArrayOf(0x00, 0x82.toByte(), 0x00, 0x00)
    sendApduSM(extAuth, chResponse, null)
    val intAuth = byteArrayOf(0x00, 0x22, 0x41, 0xa4.toByte())
    val val82 = byteArrayOf(0x82.toByte())
    val pKdScheme = byteArrayOf(0x9b.toByte())

    val temp: ByteArray =
        Utils.appendByteArray(Utils.asn1Tag(val82, 0x84), Utils.asn1Tag(pKdScheme, 0x80))
    sendApduSM(intAuth, temp, null)
    val rndIFD = Utils.getRandomByte(8)
    val giveRandom = byteArrayOf(0x00, 0x88.toByte(), 0x00, 0x00)
    resp = sendApduSM(giveRandom, rndIFD, null)

    val SN_ICC = Utils.getSub(resp.response, 0, 8)
    val intAuthResp: ByteArray
    val rsaIntAuthKey = RSA(dappModule, dappPubKey)
    intAuthResp = rsaIntAuthKey.encrypt(Utils.getSub(resp.response, 8, resp.response.size - 8))

    if (intAuthResp[0].compareTo(0x6a.toByte()) != 0)
        throw Exception("Errore nell'autenticazione del chip- Byte.compare(intAuthResp[0], (byte)0x6a) != 0")

    val PRND2 = Utils.getSub(intAuthResp, 1, intAuthResp.size - 32 - 2)
    val hashICC = Utils.getSub(intAuthResp, PRND2.size + 1, 32)
    var toHashIFD = byteArrayOf()
    toHashIFD = Utils.appendByteArray(toHashIFD, PRND2)
    toHashIFD = Utils.appendByteArray(toHashIFD, dh_ICCpubKey)
    toHashIFD = Utils.appendByteArray(toHashIFD, SN_ICC)
    toHashIFD = Utils.appendByteArray(toHashIFD, rndIFD)
    toHashIFD = Utils.appendByteArray(toHashIFD, dh_pubKey)
    toHashIFD = Utils.appendByteArray(toHashIFD, dh_g)
    toHashIFD = Utils.appendByteArray(toHashIFD, dh_p)
    toHashIFD = Utils.appendByteArray(toHashIFD, dh_q)
    val calcHashIFD = Sha256.encrypt(toHashIFD)
    if (Utils.bytesToHex(calcHashIFD) != Utils.bytesToHex(hashICC))
        throw Exception("Errore nell'autenticazione del chip (calcHashIFD,hashICC)")
    if (intAuthResp[intAuthResp.size - 1].compareTo(0xbc.toByte()) != 0)
        throw Exception("Errore nell'autenticazione del chip Utils.byteCompare(intAuthResp[intAuthResp.length - 1],0xcb")
    val ba888 = Utils.getRight(challengeResp.response, 4)
    val ba889 = Utils.getRight(rndIFD, 4)
    seq = Utils.appendByteArray(ba888, ba889)
}

private val defModule = byteArrayOf(
    0xba.toByte(),
    0x28,
    0x37,
    0xab.toByte(),
    0x4c,
    0x6b,
    0xb8.toByte(),
    0x27,
    0x57,
    0x7b,
    0xff.toByte(),
    0x4e,
    0xb7.toByte(),
    0xb1.toByte(),
    0xe4.toByte(),
    0x9c.toByte(),
    0xdd.toByte(),
    0xe0.toByte(),
    0xf1.toByte(),
    0x66,
    0x14,
    0xd1.toByte(),
    0xef.toByte(),
    0x24,
    0xc1.toByte(),
    0xb7.toByte(),
    0x5c,
    0xf7.toByte(),
    0x0f,
    0xb1.toByte(),
    0x2c,
    0xd1.toByte(),
    0x8f.toByte(),
    0x4d,
    0x14,
    0xe2.toByte(),
    0x81.toByte(),
    0x4b,
    0xa4.toByte(),
    0x87.toByte(),
    0x7e,
    0xa8.toByte(),
    0x00,
    0xe1.toByte(),
    0x75,
    0x90.toByte(),
    0x60,
    0x76,
    0xb5.toByte(),
    0x62,
    0xba.toByte(),
    0x53,
    0x59,
    0x73,
    0xc5.toByte(),
    0xd8.toByte(),
    0xb3.toByte(),
    0x78,
    0x05,
    0x1d,
    0x8a.toByte(),
    0xfc.toByte(),
    0x74,
    0x07,
    0xa1.toByte(),
    0xd9.toByte(),
    0x19,
    0x52,
    0x9e.toByte(),
    0x03,
    0xc1.toByte(),
    0x06,
    0xcd.toByte(),
    0xa1.toByte(),
    0x8d.toByte(),
    0x69,
    0x9a.toByte(),
    0xfb.toByte(),
    0x0d,
    0x8a.toByte(),
    0xb4.toByte(),
    0xfd.toByte(),
    0xdd.toByte(),
    0x9d.toByte(),
    0xc7.toByte(),
    0x19,
    0x15,
    0x9a.toByte(),
    0x50,
    0xde.toByte(),
    0x94.toByte(),
    0x68,
    0xf0.toByte(),
    0x2a,
    0xb1.toByte(),
    0x03,
    0xe2.toByte(),
    0x82.toByte(),
    0xa5.toByte(),
    0x0e,
    0x71,
    0x6e,
    0xc2.toByte(),
    0x3c,
    0xda.toByte(),
    0x5b,
    0xfc.toByte(),
    0x4a,
    0x23,
    0x2b,
    0x09,
    0xa4.toByte(),
    0xb2.toByte(),
    0xc7.toByte(),
    0x07,
    0x45,
    0x93.toByte(),
    0x95.toByte(),
    0x49,
    0x09,
    0x9b.toByte(),
    0x44,
    0x83.toByte(),
    0xcb.toByte(),
    0xae.toByte(),
    0x62,
    0xd0.toByte(),
    0x09,
    0x96.toByte(),
    0x74,
    0xdb.toByte(),
    0xf6.toByte(),
    0xf3.toByte(),
    0x9b.toByte(),
    0x72,
    0x23,
    0xa9.toByte(),
    0x9d.toByte(),
    0x88.toByte(),
    0xe3.toByte(),
    0x3f,
    0x1a,
    0x0c,
    0xde.toByte(),
    0xde.toByte(),
    0xeb.toByte(),
    0xbd.toByte(),
    0xc3.toByte(),
    0x55,
    0x17,
    0xab.toByte(),
    0xe9.toByte(),
    0x88.toByte(),
    0x0a,
    0xab.toByte(),
    0x24,
    0x0e,
    0x1e,
    0xa1.toByte(),
    0x66,
    0x28,
    0x3a,
    0x27,
    0x4a,
    0x9a.toByte(),
    0xd9.toByte(),
    0x3b,
    0x4b,
    0x1d,
    0x19,
    0xf3.toByte(),
    0x67,
    0x9f.toByte(),
    0x3e,
    0x8b.toByte(),
    0x5f,
    0xf6.toByte(),
    0xa1.toByte(),
    0xe0.toByte(),
    0xed.toByte(),
    0x73,
    0x6e,
    0x84.toByte(),
    0xd5.toByte(),
    0xab.toByte(),
    0xe0.toByte(),
    0x3c,
    0x59,
    0xe7.toByte(),
    0x34,
    0x6b,
    0x42,
    0x18,
    0x75,
    0x5d,
    0x75,
    0x36,
    0x6c,
    0xbf.toByte(),
    0x41,
    0x36,
    0xf0.toByte(),
    0xa2.toByte(),
    0x6c,
    0x3d,
    0xc7.toByte(),
    0x0a,
    0x69,
    0xab.toByte(),
    0xaa.toByte(),
    0xf6.toByte(),
    0x6e,
    0x13,
    0xa1.toByte(),
    0xb2.toByte(),
    0xfa.toByte(),
    0xad.toByte(),
    0x05,
    0x2c,
    0xa6.toByte(),
    0xec.toByte(),
    0x9c.toByte(),
    0x51,
    0xe2.toByte(),
    0xae.toByte(),
    0xd1.toByte(),
    0x4d,
    0x16,
    0xe0.toByte(),
    0x90.toByte(),
    0x25,
    0x4d,
    0xc3.toByte(),
    0xf6.toByte(),
    0x4e,
    0xa2.toByte(),
    0xbd.toByte(),
    0x8a.toByte(),
    0x83.toByte(),
    0x6b,
    0xba.toByte(),
    0x99.toByte(),
    0xde.toByte(),
    0xfa.toByte(),
    0xcb.toByte(),
    0xa3.toByte(),
    0xa6.toByte(),
    0x13,
    0xae.toByte(),
    0xed.toByte(),
    0xd9.toByte(),
    0x3a,
    0x96.toByte(),
    0x15,
    0x27,
    0x3d
)
private val defPrivExp = byteArrayOf(
    0x47,
    0x16,
    0xc2.toByte(),
    0xa3.toByte(),
    0x8c.toByte(),
    0xcc.toByte(),
    0x7a,
    0x07,
    0xb4.toByte(),
    0x15,
    0xeb.toByte(),
    0x1a,
    0x61,
    0x75,
    0xf2.toByte(),
    0xaa.toByte(),
    0xa0.toByte(),
    0xe4.toByte(),
    0x9c.toByte(),
    0xea.toByte(),
    0xf1.toByte(),
    0xba.toByte(),
    0x75,
    0xcb.toByte(),
    0xa0.toByte(),
    0x9a.toByte(),
    0x68,
    0x4b,
    0x04,
    0xd8.toByte(),
    0x11,
    0x18,
    0x79,
    0xd3.toByte(),
    0xe2.toByte(),
    0xcc.toByte(),
    0xd8.toByte(),
    0xb9.toByte(),
    0x4d,
    0x3c,
    0x5c,
    0xf6.toByte(),
    0xc5.toByte(),
    0x57,
    0x53,
    0xf0.toByte(),
    0xed.toByte(),
    0x95.toByte(),
    0x87.toByte(),
    0x91.toByte(),
    0x0b,
    0x3c,
    0x77,
    0x25,
    0x8a.toByte(),
    0x01,
    0x46,
    0x0f,
    0xe8.toByte(),
    0x4c,
    0x2e,
    0xde.toByte(),
    0x57,
    0x64,
    0xee.toByte(),
    0xbe.toByte(),
    0x9c.toByte(),
    0x37,
    0xfb.toByte(),
    0x95.toByte(),
    0xcd.toByte(),
    0x69,
    0xce.toByte(),
    0xaf.toByte(),
    0x09,
    0xf4.toByte(),
    0xb1.toByte(),
    0x35,
    0x7c,
    0x27,
    0x63,
    0x14,
    0xab.toByte(),
    0x43,
    0xec.toByte(),
    0x5b,
    0x3c,
    0xef.toByte(),
    0xb0.toByte(),
    0x40,
    0x3f,
    0x86.toByte(),
    0x8f.toByte(),
    0x68,
    0x8e.toByte(),
    0x2e,
    0xc0.toByte(),
    0x9a.toByte(),
    0x49,
    0x73,
    0xe9.toByte(),
    0x87.toByte(),
    0x75,
    0x6f,
    0x8d.toByte(),
    0xa7.toByte(),
    0xa1.toByte(),
    0x01,
    0xa2.toByte(),
    0xca.toByte(),
    0x75,
    0xa5.toByte(),
    0x4a,
    0x8c.toByte(),
    0x4c,
    0xcf.toByte(),
    0x9a.toByte(),
    0x1b,
    0x61,
    0x47,
    0xe4.toByte(),
    0xde.toByte(),
    0x56,
    0x42,
    0x3a,
    0xf7.toByte(),
    0x0b,
    0x20,
    0x67,
    0x17,
    0x9c.toByte(),
    0x5e,
    0xeb.toByte(),
    0x64,
    0x68,
    0x67,
    0x86.toByte(),
    0x34,
    0x78,
    0xd7.toByte(),
    0x52,
    0xc7.toByte(),
    0xf4.toByte(),
    0x12,
    0xdb.toByte(),
    0x27,
    0x75,
    0x41,
    0x57,
    0x5a,
    0xa0.toByte(),
    0x61,
    0x9d.toByte(),
    0x30,
    0xbc.toByte(),
    0xcc.toByte(),
    0x8d.toByte(),
    0x87.toByte(),
    0xe6.toByte(),
    0x17,
    0x0b,
    0x33,
    0x43,
    0x9a.toByte(),
    0x2c,
    0x93.toByte(),
    0xf2.toByte(),
    0xd9.toByte(),
    0x7e,
    0x18,
    0xc0.toByte(),
    0xa8.toByte(),
    0x23,
    0x43,
    0xa6.toByte(),
    0x01,
    0x2a,
    0x5b,
    0xb1.toByte(),
    0x82.toByte(),
    0x28,
    0x08,
    0xf0.toByte(),
    0x1b,
    0x5c,
    0xfd.toByte(),
    0x85.toByte(),
    0x67,
    0x3a,
    0xc0.toByte(),
    0x96.toByte(),
    0x4c,
    0x5f,
    0x3c,
    0xfd.toByte(),
    0x2d,
    0xaf.toByte(),
    0x81.toByte(),
    0x42,
    0x35,
    0x97.toByte(),
    0x64,
    0xa9.toByte(),
    0xad.toByte(),
    0xb9.toByte(),
    0xe3.toByte(),
    0xf7.toByte(),
    0x6d,
    0xb6.toByte(),
    0x13,
    0x46,
    0x1c,
    0x1b,
    0xc9.toByte(),
    0x13,
    0xdc.toByte(),
    0x9a.toByte(),
    0xc0.toByte(),
    0xab.toByte(),
    0x50,
    0xd3.toByte(),
    0x65,
    0xf7.toByte(),
    0x7c,
    0xb9.toByte(),
    0x31,
    0x94.toByte(),
    0xc9.toByte(),
    0x8a.toByte(),
    0xa9.toByte(),
    0x66,
    0xd8.toByte(),
    0x9c.toByte(),
    0xdd.toByte(),
    0x55,
    0x51,
    0x25,
    0xa5.toByte(),
    0xe5.toByte(),
    0x9e.toByte(),
    0xcf.toByte(),
    0x4f,
    0xa3.toByte(),
    0xf0.toByte(),
    0xc3.toByte(),
    0xfd.toByte(),
    0x61,
    0x0c,
    0xd3.toByte(),
    0xd0.toByte(),
    0x56,
    0x43,
    0x93.toByte(),
    0x38,
    0xfd.toByte(),
    0x81.toByte()
)
private val defPubExp = byteArrayOf(0x00, 0x01, 0x00, 0x01)