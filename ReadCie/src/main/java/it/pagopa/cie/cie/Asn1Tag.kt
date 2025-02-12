package it.pagopa.cie.cie

import androidx.annotation.VisibleForTesting
import java.io.ByteArrayInputStream
import kotlin.math.pow

class Asn1Tag(objects: Array<Any>) {
    private var unusedBits: Byte = 0
    private var tag: ByteArray
    private lateinit var data: ByteArray
    private var children: List<Asn1Tag>? = null
    private var startPos: Long = 0
    var endPos: Long = 0
    private var constructed: Long = 0
    private var childSize: Long = 0

    init {
        tag = ByteArray(objects.size)
        for (i in objects.indices) tag[i] = objects[i] as Byte
    }

    val isTagConstructed: Boolean get() = tag[0].toInt() and 0x20 != 0

    companion object {
        private fun unsignedToBytes32(x: Int): Long {
            return if (x > 0) x.toLong() else 32.0.pow(2.0).toLong() + x
        }

        private fun parse(asn: ByteArrayInputStream, start: Long, length: Long, reparse: Boolean): Asn1Tag? {
            var readPos = 0
            val `in`: ByteArrayInputStream = asn
            var tag = unsignedToBytes(`in`.read().toByte()) //96
            if (readPos.toLong() == length) throw Exception("empty array")
            val tagVal: MutableList<Byte> = ArrayList()
            readPos++
            tagVal.add(tag.toByte())
            if (tag.toByte().toInt() and 0x1f == 0x1f) {
                // è un tag a più bytes; proseguo finchè non trovo un bit 8 a 0
                while (true) {
                    if (readPos.toLong() == length) throw Exception()
                    tag = `in`.read()
                    readPos++
                    tagVal.add(tag.toByte())
                    if (tag and 0x80 != 0x80) {
                        // è l'ultimo byte del tag
                        break
                    }
                }
            }
            // leggo la lunghezza
            if (readPos.toLong() == length) throw Exception()
            var len = unsignedToBytes(`in`.read().toByte()).toLong()
            readPos++
            if (len > unsignedToBytes(0x80.toByte())) {
                val lenlen = unsignedToBytes((len - 0x80).toByte())
                len = 0
                for (i in 0 until lenlen) {
                    if (readPos.toLong() == length) throw Exception()
                    val bTmp = unsignedToBytes(`in`.read().toByte())
                    len = unsignedToBytes32((len shl 8 or bTmp.toLong()).toInt())
                    readPos++
                }
            }
            val size = readPos + len
            if (size > length) throw Exception("ASN1 non valido")
            if (tagVal.size == 1 && tagVal[0].toInt() == 0 && len == 0L) {
                return null
            }
            var data: ByteArray? = ByteArray(len.toInt())
            `in`.read(data!!, 0, len.toInt())
            val ms = ByteArrayInputStream(data)
            val newTag = Asn1Tag(tagVal.toTypedArray())
            newTag.childSize = size
            var childern: MutableList<Asn1Tag>? = null
            var parsedLen: Long = 0
            var parseSubTags = false
            if (newTag.isTagConstructed) parseSubTags = true else if (reparse && knownTag(newTag.tag) === "OCTET STRING") parseSubTags =
                true else if (reparse && knownTag(newTag.tag) === "BIT STRING") {
                parseSubTags = true
                newTag.unusedBits = ms.read().toByte()
                parsedLen++
            }
            if (parseSubTags) {
                childern = ArrayList()
                while (true) {
                    val child = parse(ms, start + readPos + parsedLen, len - parsedLen, reparse)
                    if (child != null) childern!!.add(child)
                    parsedLen += child!!.childSize
                    if (parsedLen > len) {
                        childern = null
                        break
                    } else if (parsedLen == len) {
                        data = null
                        break
                    }
                }
            }
            newTag.startPos = start
            newTag.endPos = start + size
            if (childern == null) {
                newTag.data = data ?: byteArrayOf()
            } else {
                newTag.children = childern
                newTag.constructed = len
            }
            return newTag
        }

        @VisibleForTesting
        fun knownTag(tag: ByteArray?): String? {
            if (tag?.size != 1) return null
            return when (tag[0]) {
                2.toByte() -> "INTEGER"
                3.toByte() -> "BIT STRING"
                4.toByte() -> "OCTET STRING"
                5.toByte() -> "NULL"
                6.toByte() -> "OBJECT IDENTIFIER"
                0x30.toByte() -> "SEQUENCE"
                0x31.toByte() -> "SET"
                12.toByte() -> "UTF8 String"
                19.toByte() -> "PrintableString"
                20.toByte() -> "T61String"
                22.toByte() -> "IA5String"
                23.toByte() -> "UTCTime"
                else -> null
            }
        }

        private fun unsignedToBytes(b: Byte): Int {
            return b.toInt() and 0xFF
        }

        fun parse(efCom: ByteArray, reparse: Boolean): Asn1Tag? {
            val size = 0
            val `in` = ByteArrayInputStream(efCom)
            return parse(`in`, size.toLong(), efCom.size.toLong(), reparse)
        }
    }
}