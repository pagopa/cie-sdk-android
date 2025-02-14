package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.Asn1Tag
import it.pagopa.cie.nfc.RSA
import it.pagopa.cie.nfc.Sha256
import it.pagopa.cie.nfc.Utils

/**
 * recupera i parametri delle chiavi per Diffie Hellman
 */
@Throws(Exception::class)
internal fun CieCommands.initDHParam() {
    CieLogger.i("Starting secure channel", "initDHParam()")
    val getDHDoup = byteArrayOf(0, 0xcb.toByte(), 0x3f, 0xff.toByte())
    val getDHDuopData_g = byteArrayOf(
        0x4D,
        0x0A,
        0x70,
        0x08,
        0xBF.toByte(),
        0xA1.toByte(),
        0x01,
        0x04,
        0xA3.toByte(),
        0x02,
        0x97.toByte(),
        0x00
    )
    var resp = sendApdu(getDHDoup, getDHDuopData_g, null, "Init dh param")
    var asn1Tag = Asn1Tag.Companion.parse(resp.response, false)
    dhG = asn1Tag!!.child(0).child(0).child(0).data
    val getDHDuopData_p = byteArrayOf(
        0x4D,
        0x0A,
        0x70,
        0x08,
        0xBF.toByte(),
        0xA1.toByte(),
        0x01,
        0x04,
        0xA3.toByte(),
        0x02,
        0x98.toByte(),
        0x00
    )
    resp = sendApdu(getDHDoup, getDHDuopData_p, null, "init dh param")
    asn1Tag = Asn1Tag.Companion.parse(resp.response, false)
    dhP = asn1Tag!!.child(0).child(0).child(0).data
    val getDHDuopData_q = byteArrayOf(
        0x4D,
        0x0A,
        0x70,
        0x08,
        0xBF.toByte(),
        0xA1.toByte(),
        0x01,
        0x04,
        0xA3.toByte(),
        0x02,
        0x99.toByte(),
        0x00
    )
    resp = sendApdu(getDHDoup, getDHDuopData_q, null, "init dh param")
    asn1Tag = Asn1Tag.Companion.parse(resp.response, false)
    dhQ = asn1Tag!!.child(0).child(0).child(0).data
}

/**
 * scambio di chiavi Diffie Hellman
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.dhKeyExchange() {
    CieLogger.i("COMMAND", "dhKeyExchange()")
    var dh_prKey: ByteArray = byteArrayOf()
    do {
        dh_prKey = Utils.getRandomByte(dhQ.size)
    } while (dhQ[0] < dh_prKey[0])

    val dhg = dhG.clone()
    val rsa = RSA(dhP, dh_prKey)
    dhpubKey = rsa.encrypt(dhg)

    val algo = byteArrayOf(0x9b.toByte())
    val keyId = byteArrayOf(0x81.toByte())
    val tmp1 = Utils.appendByteArray(
        Utils.appendByteArray(Utils.asn1Tag(algo, 0x80), Utils.asn1Tag(keyId, 0x83)),
        Utils.asn1Tag(dhpubKey, 0x91)
    )
    val MSE_SET = byteArrayOf(0x00, 0x22, 0x41, 0xa6.toByte())
    sendApdu(MSE_SET, tmp1, null, "SETTING MSE")

    val GET_DATA = byteArrayOf(0x00, 0xcb.toByte(), 0x3f, 0xff.toByte())
    val GET_DATA_Data = byteArrayOf(0x4d, 0x04, 0xa6.toByte(), 0x02, 0x91.toByte(), 0x00)
    val respAsn = sendApdu(GET_DATA, GET_DATA_Data, null, "GET DATA")
    val asn1 = Asn1Tag.Companion.parse(respAsn.response, true)
    dhICCpubKey = asn1!!.child(0).data
    val secret = rsa.encrypt(dhICCpubKey)

    val diffENC = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    val diffMAC = byteArrayOf(0x00, 0x00, 0x00, 0x02)

    var d1 = Sha256.encrypt(Utils.appendByteArray(secret, diffENC))
    sessionEncryption = Utils.getLeft(d1, 16)

    d1 = Sha256.encrypt(Utils.appendByteArray(secret, diffMAC))
    sessMac = Utils.getLeft(d1, 16)

    seq = ByteArray(8)
    seq[7] = 0x01
}