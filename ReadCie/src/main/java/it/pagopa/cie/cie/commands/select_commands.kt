package it.pagopa.cie.cie.commands

import it.pagopa.cie.cie.NfcEvent

internal fun CieCommands.selectIAS(): ByteArray {
    return onTransmit.sendCommand(
        byteArrayOf(
            0x00,
            0xa4.toByte(),
            0x04,
            0x0c,
            0x0d,
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x30,
            0x80.toByte(),
            0x00,
            0x00,
            0x00,
            0x09,
            0x81.toByte(),
            0x60,
            0x01
        ), NfcEvent.SELECT_IAS
    ).response
}

/**
 *Sends an APDU to select the CIE section of the card
 *@return: The response sent by the card
 */
internal fun CieCommands.selectCie(): ByteArray {
    return onTransmit.sendCommand(
        byteArrayOf(
            0x00,
            0xa4.toByte(),
            0x04,
            0x0c,
            0x06,
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x39
        ), NfcEvent.SELECT_CIE
    ).response
}