package it.pagopa.io.app.cie.pace.utils

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.ApduManager
import it.pagopa.io.app.cie.cie.ApduResponse
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.cie.commands.CieCommands
import it.pagopa.io.app.cie.nfc.Utils
import it.pagopa.io.app.cie.pace.Tlv
import it.pagopa.io.app.cie.pace.TlvReader
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
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
    return if (tlv7c.size > ApduManager.STANDARD_APDU_SIZE) {
        ApduManager(onTransmit).sendApduExtended(
            head = head,
            data = tlv7c,
            le = null,
            event = NfcEvent.GENERAL_AUTHENTICATE_STEP1
        )
    } else {
        ApduManager(onTransmit).sendApdu(
            head = head,
            data = tlv7c,
            le = null,
            event = NfcEvent.GENERAL_AUTHENTICATE_STEP1
        )
    }
}

internal fun CieCommands.generalAuthenticateStep2(mappedPublicKey: ByteArray): ApduResponse {
    CieLogger.i(
        "MAPPED PUB KEY HEX",
        "Size: ${mappedPublicKey.size}\n${Utils.bytesToString(mappedPublicKey)}"
    )
    val tlv83 = tlv(0x83, mappedPublicKey)
    val tlv7c = tlv(0x7C, tlv83)
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

internal fun CieCommands.sendGeneralAuthenticateToken(token: ByteArray): ApduResponse {
    val tlv85 = tlv(0x85, token)       // token in TLV 85
    val tlv7c = tlv(0x7C, tlv85)       // wrapper 7C
    val head = byteArrayOf(0x00, 0x86.toByte(), 0x00, 0x00) // GENERAL AUTHENTICATE

    return if (tlv7c.size > ApduManager.STANDARD_APDU_SIZE) {
        ApduManager(onTransmit).sendApduExtended(
            head = head,
            data = tlv7c,
            le = null,
            event = NfcEvent.GENERAL_AUTHENTICATE_STEP3
        )
    } else {
        ApduManager(onTransmit).sendApdu(
            head = head,
            data = tlv7c,
            le = null,
            event = NfcEvent.GENERAL_AUTHENTICATE_STEP3
        )
    }
}

internal fun ByteArray.offsetToBigInt(): BigInteger {
    return os2i(0, this.size)
}

private fun ByteArray.os2i(offset: Int, length: Int): BigInteger {
    var result = BigInteger.ZERO
    val base = BigInteger.valueOf(256)
    for (i in offset until offset + length) {
        result = result.multiply(base)
        result = result.add(BigInteger.valueOf((this[i].toInt() and 0xFF).toLong()))
    }
    return result
}

internal fun TlvReader.parse7c82Tlv(): Tlv {
    // Parse outer TLV container (tag 0x7C)
    val tlvs = this.readRaw()
    val container = tlvs.firstOrNull {
        it.tag == 0x7c
    }
    if (container == null) {
        CieLogger.e("parse7c82Tlv", "Unable to get public key from CIE")
        throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
            this.msg = "Unable to get public key from CIE"
        })
    }

    // Parse inner TLVs from container
    val readerPublicKeyTlvs = TlvReader(container.value).readAll()
    readerPublicKeyTlvs.forEach {
        CieLogger.i("TLV", it.toString())
    }

    // Search for public key TLV tag (0x82 or 0x84)
    val publicKeyTLv = readerPublicKeyTlvs.firstOrNull {
        it.tag == 0x82 || it.tag == 0x83 || it.tag == 0x84
    }
    if (publicKeyTLv == null) {
        CieLogger.e("parse7c82Tlv", "Unable to get public key from container")
        throw CieSdkException(NfcError.GENERAL_EXCEPTION.apply {
            this.msg = "Unable to get public key from CIE from container"
        })
    }
    return publicKeyTLv
}
