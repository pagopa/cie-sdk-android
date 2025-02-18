package it.pagopa.cie.cie.commands

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduManager
import it.pagopa.cie.cie.Asn1Tag
import it.pagopa.cie.nfc.Utils

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
        .sendApdu(getKeyDoup, getKeyDuopData, null, "getKeyDuopData")
    val asn1 = Asn1Tag.Companion.parse(response.response, true)
    caModule = asn1!!.child(0).child(0).childWithTagID(byteArrayOf(0x81.toByte()))!!.data
    //caPubExp = asn1.child(0).child(0).childWithTagID(byteArrayOf(0x82.toByte()))!!.data
    val caCha = asn1.child(0).child(0).childWithTagID(byteArrayOf(0x5f, 0x4c))!!.data
    val caChr = asn1.child(0).child(0).childWithTagID(byteArrayOf(0x5f, 0x20))!!.data
    caCar = Utils.getSub(caChr, 4)
    caAid = Utils.getLeft(caCha, 6)
}
