package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.nfc.Utils
import java.io.ByteArrayInputStream

data class Tlv(val tag: Int, val length: Int, val value: ByteArray) {
    override fun toString(): String {
        return "tag: $tag;\nlength: $length;\nvalue: ${Utils.bytesToString(value)}\nvalue size: ${value.size}"
    }
}

class TlvReader(private val data: ByteArray) {
    private fun ByteArrayInputStream.readTag(): Int {
        val firstByte = this.read()
        if (firstByte and 0x1F == 0x1F) {
            // Tag multi-byte
            var tag = firstByte
            var next: Int
            do {
                next = this.read()
                tag = (tag shl 8) or next
            } while (next and 0x80 != 0)
            return tag
        } else {
            return firstByte
        }
    }

    fun readRaw(): List<Tlv> {
        val list = mutableListOf<Tlv>()
        val input = ByteArrayInputStream(data)

        while (input.available() > 0) {
            val tag = input.readTag()
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
            val tag = input.readTag()
            if (tag == -1) break

            val length = readLength(input)
            val value = ByteArray(length)
            val readBytes = input.read(value)
            if (readBytes != length) throw IllegalStateException("Errore di lettura TLV")

            list.add(Tlv(tag, length, value))
            if (tag.isConstructedTag() && !tag.isBinaryDataTag())
                list.addAll(TlvReader(value).readAll())
        }
        return list
    }

    // DG2 photo ctrl
    private fun Tag.isBinaryDataTag(): Boolean = this == 0x5F2E

    private fun readLength(input: ByteArrayInputStream): Int {
        val firstByte = input.read()
        return if (firstByte < 0x80) firstByte else {
            val lengthBytesCount = firstByte and 0x7F
            var length = 0
            repeat(lengthBytesCount) { length = (length shl 8) or input.read() }
            length
        }
    }

    private fun Tag.isConstructedTag() = (this and 0x20) != 0
}

private typealias Tag = Int