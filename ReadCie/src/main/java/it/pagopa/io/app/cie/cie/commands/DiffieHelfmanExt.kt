package it.pagopa.io.app.cie.cie.commands

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.ApduManager
import it.pagopa.io.app.cie.cie.Asn1Tag
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.RSA
import it.pagopa.io.app.cie.nfc.Sha256
import it.pagopa.io.app.cie.nfc.Utils

/**
 * It retrieves Diffie Hellman keys parameters
 */
@Throws(Exception::class)
internal fun CieCommands.initDHParam() {
    CieLogger.i("Starting secure channel", "initDHParam()")
    val getDHDoup = byteArrayOf(0, 0xcb.toByte(), 0x3f, 0xff.toByte())
    val getDHDuopDataG = byteArrayOf(
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
    val manager = ApduManager(onTransmit)
    var resp = manager.sendApdu(getDHDoup, getDHDuopDataG, null, NfcEvent.DH_INIT_GET_G)
    var asn1Tag = Asn1Tag.Companion.parse(resp.response, false)
    dhG = asn1Tag!!.child(0).child(0).child(0).data
    val getDHDuopDataP = byteArrayOf(
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
    resp = manager.sendApdu(getDHDoup, getDHDuopDataP, null, NfcEvent.DH_INIT_GET_P)
    asn1Tag = Asn1Tag.parse(resp.response, false)
    dhP = asn1Tag!!.child(0).child(0).child(0).data
    val getDHDuopDataQ = byteArrayOf(
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
    resp = manager.sendApdu(getDHDoup, getDHDuopDataQ, null, NfcEvent.DH_INIT_GET_Q)
    asn1Tag = Asn1Tag.parse(resp.response, false)
    dhQ = asn1Tag!!.child(0).child(0).child(0).data
}

/**
 * Diffie Hellman Key exchange
 * @throws Exception
 */
@Throws(Exception::class)
internal fun CieCommands.dhKeyExchange() {
    CieLogger.i("COMMAND", "dhKeyExchange()")
    //creating RSA parameters
    var dh_prKey: ByteArray = byteArrayOf()
    do {
        dh_prKey = Utils.getRandomByte(dhQ.size)
    } while (dhQ[0] < dh_prKey[0])

    val dhg = dhG.clone()
    val rsa = RSA(dhP, dh_prKey)
    //creating Diffie Hellman public key
    dhpubKey = rsa.encrypt(dhg)

    //internal key agreement
    val algo = byteArrayOf(0x9b.toByte())
    val keyId = byteArrayOf(0x81.toByte())
    val tmp1 = Utils.appendByteArray(
        Utils.appendByteArray(Utils.asn1Tag(algo, 0x80), Utils.asn1Tag(keyId, 0x83)),
        Utils.asn1Tag(dhpubKey, 0x91)
    )
    val setMse = byteArrayOf(0x00, 0x22, 0x41, 0xa6.toByte())
    val manager = ApduManager(onTransmit)
    manager.sendApdu(setMse, tmp1, null, NfcEvent.SET_MSE)

    // get ICC public key
    val getData = byteArrayOf(0x00, 0xcb.toByte(), 0x3f, 0xff.toByte())
    val getIccData = byteArrayOf(0x4d, 0x04, 0xa6.toByte(), 0x02, 0x91.toByte(), 0x00)
    val respAsn =
        manager.sendApdu(getData, getIccData, null, NfcEvent.D_H_KEY_EXCHANGE_GET_DATA)
    val asn1 = Asn1Tag.parse(respAsn.response, true)
    dhICCpubKey = asn1!!.child(0).data
    val secret = rsa.encrypt(dhICCpubKey)

    val diffENC = byteArrayOf(0x00, 0x00, 0x00, 0x01)
    val diffMAC = byteArrayOf(0x00, 0x00, 0x00, 0x02)
    // set secure messaging
    var d1 = Sha256.encrypt(Utils.appendByteArray(secret, diffENC))
    sessionEncryption = Utils.getLeft(d1, 16)

    d1 = Sha256.encrypt(Utils.appendByteArray(secret, diffMAC))
    sessMac = Utils.getLeft(d1, 16)

    seq = ByteArray(8)
    seq[7] = 0x01
}