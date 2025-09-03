package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.nfc.Utils
import java.io.ByteArrayInputStream

data class Tlv(val tag: Int, val length: Int, val value: ByteArray) {
    override fun toString(): String {
        return "tag: $tag;\nlength: $length;\nvalue: ${Utils.bytesToString(value)}\nvalue size: ${value.size}"
    }
}

class TlvReader(private val data: ByteArray) {
    fun readRaw(): List<Tlv> {
        val list = mutableListOf<Tlv>()
        val input = ByteArrayInputStream(data)

        while (input.available() > 0) {
            val tag = input.read()
            if (tag == -1) break

            val length = readLength(input)
            val value = ByteArray(length)
            val readBytes = input.read(value)
            if (readBytes != length) throw IllegalStateException("Errore di lettura TLV")

            list.add(Tlv(tag, length, value))
        }
        return list
    }

    fun readAll(): List<Tlv> {
        val list = mutableListOf<Tlv>()
        val input = ByteArrayInputStream(data)

        while (input.available() > 0) {
            val tag = input.read()
            if (tag == -1) break

            val length = readLength(input)
            val value = ByteArray(length)
            val readBytes = input.read(value)
            if (readBytes != length) throw IllegalStateException("Errore di lettura TLV")

            list.add(Tlv(tag, length, value))
            if (isConstructedTag(tag)) list.addAll(TlvReader(value).readAll())
        }
        return list
    }

    private fun readLength(input: ByteArrayInputStream): Int {
        val firstByte = input.read()
        return if (firstByte < 0x80) firstByte else {
            val lengthBytesCount = firstByte and 0x7F
            var length = 0
            repeat(lengthBytesCount) { length = (length shl 8) or input.read() }
            length
        }
    }

    private fun isConstructedTag(tag: Int) = (tag and 0x20) != 0
}