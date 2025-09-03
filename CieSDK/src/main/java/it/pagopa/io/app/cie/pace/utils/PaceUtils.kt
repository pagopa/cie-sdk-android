package it.pagopa.io.app.cie.pace.utils

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.ApduManager
import it.pagopa.io.app.cie.cie.ApduResponse
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

@Throws(CieSdkException::class)
internal fun ApduResponse.parseResponse(event: NfcEvent) {
    if (this.swHex != "9000" && this.swHex != "6282") {
        CieLogger.e(event.name, "Fail: ${this.swHex}")
        throw CieSdkException(NfcError.RESPONSE_EXCEPTION.apply {
            msg = "SW: ${this@parseResponse.swHex}"
        })
    }
}

/**MSE:Set AT per PACE con CAN*/
internal fun CieCommands.setMsePaceCan(oidBytes: ByteArray): ApduResponse {
    // TLV 80 + OID
    val tlv80: ByteArray =
        Utils.appendByteArray(byteArrayOf(0x80.toByte(), oidBytes.size.toByte()), oidBytes)
    // TLV 83 + KeyRef = 0x02 for CAN
    val tlv83 = byteArrayOf(0x83.toByte(), 0x01, 0x02)

    val data = Utils.appendByteArray(tlv80, tlv83)

    CieLogger.i("MSE_PACE_CAN", "APDU Data: ${Utils.bytesToString(data)}")

    return ApduManager(onTransmit = onTransmit).sendApdu(
        head = byteArrayOf(0x00, 0x22, 0xC1.toByte(), 0xA4.toByte()),
        data = data,
        le = null,
        event = NfcEvent.SET_MSE
    )
}

internal fun CieCommands.generalAuthenticateStep0(): ApduResponse {
    // Tag 7C = wrapper
    val tlv7c = Utils.appendByteArray(byteArrayOf(0x7C.toByte(), 0.toByte()), byteArrayOf())
    CieLogger.i("PACE_STEP0", "APDU Data: ${Utils.bytesToString(tlv7c)}")
    return ApduManager(onTransmit = onTransmit).sendApdu(
        head = byteArrayOf(0x10, 0x86.toByte(), 0x00, 0x00),
        data = tlv7c,
        le = null,
        event = NfcEvent.GENERAL_AUTHENTICATE_STEP0
    )
}

internal fun addBcIfNeeded() {
    val isBcAlreadyIntoProviders = Security.getProviders().any {
        it.name == BouncyCastleProvider.PROVIDER_NAME
    }
    if (!isBcAlreadyIntoProviders) {
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    } else {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}

private fun tlv(tag: Int, value: ByteArray): ByteArray {
    val len = value.size
    val lenBytes = when {
        len <= 0x7F -> byteArrayOf(len.toByte())
        len <= 0xFF -> byteArrayOf(0x81.toByte(), len.toByte())
        else -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xFF).toByte())
    }
    return byteArrayOf(tag.toByte()) + lenBytes + value
}

internal fun CieCommands.generalAuthenticateStep1(ephemeralPublicKey: ByteArray): ApduResponse {
    // Tag 81 = Public Key
    CieLogger.i("PUB KEY HEX", Utils.bytesToString(ephemeralPublicKey))
    val tlv81 = tlv(0x81, ephemeralPublicKey)
    CieLogger.i("PACE_STEP1 tlv81", "tlv81: ${Utils.bytesToString(tlv81)}\n${tlv81.size}")
    val tlv7c = tlv(0x7C, tlv81)
    CieLogger.i("PACE_STEP1", "tlv7c: ${Utils.bytesToString(tlv7c)}\n${tlv7c.size}")
    val head = byteArrayOf(0x10, 0x86.toByte(), 0x00, 0x00)
    return ApduManager(onTransmit = onTransmit).sendApduExtended(
        head = head,
        data = tlv7c,
        le = null,
        event = NfcEvent.GENERAL_AUTHENTICATE_STEP1
    )
}

internal fun CieCommands.generalAuthenticateStep2(mappedPublicKey: ByteArray): ApduResponse {
    CieLogger.i("MAPPED PUB KEY HEX", "Size: ${mappedPublicKey.size}\n${Utils.bytesToString(mappedPublicKey)}")
    val tlv82 = tlv(0x82, mappedPublicKey) // ASN.1 DER
    val tlv7c = tlv(0x7C, tlv82)
    CieLogger.i("PACE_STEP2 tlv7c", "${Utils.bytesToString(tlv7c)} (${tlv7c.size} bytes)")
    val head = byteArrayOf(0x10, 0x86.toByte(), 0x00, 0x00)
    return if (tlv7c.size > ApduManager.STANDARD_APDU_SIZE) {
        ApduManager(onTransmit).sendApduExtended(
            head = head,
            data = tlv7c,
            le = null,
            event = NfcEvent.GENERAL_AUTHENTICATE_STEP2
        )
    } else {
        ApduManager(onTransmit).sendApdu(
            head = head,
            data = tlv7c,
            le = null,
            event = NfcEvent.GENERAL_AUTHENTICATE_STEP2
        )
    }
}

internal fun CieCommands.selectPace(): ByteArray {
    val aid = byteArrayOf(
        0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01, 0x02
    )
    return onTransmit.sendCommand(
        byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x0C, aid.size.toByte()
        ) + aid,
        NfcEvent.SELECT_PACE
    ).response
}

// SELECT EF.CardAccess (FID 01 1C)
internal fun CieCommands.selectEfCardAccess(): ApduResponse {
    val fid = byteArrayOf(0x01, 0x1C)
    return ApduManager(onTransmit).sendApdu(
        head = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C), // CLA, INS, P1, P2
        data = fid,
        le = null,
        event = NfcEvent.SELECT_EF_CARDACCESS
    )
}

/*
* internal fun CieCommands.readEfCardAccess(): ByteArray {
    val resp = ApduManager(onTransmit).sendApdu(
        head = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00),
        data = byteArrayOf(),
        le = byteArrayOf(0xFF.toByte()),
        event = NfcEvent.READ_BINARY
    )
    return resp.response
}*/
// READ BINARY completo (gestione chunk manuale se file > 255 byte)
internal fun CieCommands.readEfCardAccess(): ByteArray {
    val buffer = mutableListOf<Byte>()
    var offset = 0
    val chunkSize = 0xFF // 255 byte max per APDU read

    while (true) {
        val p1 = ((offset shr 8) and 0xFF).toByte() // offset alto
        val p2 = (offset and 0xFF).toByte()         // offset basso

        val resp = ApduManager(onTransmit).sendApdu(
            head = byteArrayOf(0x00, 0xB0.toByte(), p1, p2),
            data = byteArrayOf(),
            le = byteArrayOf(chunkSize.toByte()), // le = lunghezza lettura
            event = NfcEvent.READ_BINARY
        )
        if (resp.response.isNotEmpty()) {
            buffer.addAll(resp.response.toList())
        }
        if (resp.swHex == "6282" || resp.response.size < chunkSize) {
            break
        }

        offset += chunkSize
    }
    return buffer.toByteArray()
}

// ASN.1 OID decode — DER encoding: 1° byte = 40*x + y
internal fun decodeOid(bytes: ByteArray): Pair<String, ByteArray> {
    if (bytes.isEmpty()) return "" to byteArrayOf()

    val first = bytes[0].toInt() and 0xFF
    val oidParts = mutableListOf<Int>()
    oidParts.add(first / 40)
    oidParts.add(first % 40)

    var value = 0
    for (i in 1 until bytes.size) {
        val b = bytes[i].toInt() and 0xFF
        value = (value shl 7) or (b and 0x7F)
        if ((b and 0x80) == 0) {
            oidParts.add(value)
            value = 0
        }
    }
    val oidString = oidParts.joinToString(".")
    return oidString to bytes
}

internal fun listAllOidsFromCardAccess(efBytes: ByteArray) {
    var i = 0
    while (i < efBytes.size) {
        if (efBytes[i] == 0x06.toByte() && i + 1 < efBytes.size) {
            val len = efBytes[i + 1].toInt() and 0xFF
            if (i + 2 + len <= efBytes.size) {
                val oidBytes = efBytes.copyOfRange(i + 2, i + 2 + len)
                val (oidString, raw) = decodeOid(oidBytes)
                CieLogger.i("OID_FOUND", "$oidString (${Utils.bytesToString(raw)})")
                i += 2 + len
                continue
            }
        }
        i++
    }
}


// GA Phase 1
internal fun CieCommands.gaPhase1(): ApduResponse {
    val ga1 = byteArrayOf(0x7C, 0x02, 0x80.toByte(), 0x00)
    return ApduManager(onTransmit = onTransmit).sendApdu(
        head = byteArrayOf(0x10, 0x86.toByte(), 0x00, 0x00), // solo 4 byte
        data = ga1,
        le = null,
        event = NfcEvent.GA_PHASE_1
    )
}