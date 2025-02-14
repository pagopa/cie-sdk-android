package it.pagopa.cie

import it.pagopa.cie.cie.Asn1Tag
import org.junit.Test

class Asn1TagTest {
    @Test
    fun known_tag_test() {
        assert(Asn1Tag.knownTag(byteArrayOf(2.toByte())) == "INTEGER")
        assert(Asn1Tag.knownTag(byteArrayOf(3.toByte())) == "BIT STRING")
        assert(Asn1Tag.knownTag(byteArrayOf(4.toByte())) == "OCTET STRING")
        assert(Asn1Tag.knownTag(byteArrayOf(5.toByte())) == "NULL")
        assert(Asn1Tag.knownTag(byteArrayOf(6.toByte())) == "OBJECT IDENTIFIER")
        assert(Asn1Tag.knownTag(byteArrayOf(0x30.toByte())) == "SEQUENCE")
        assert(Asn1Tag.knownTag(byteArrayOf(0x31.toByte())) == "SET")
        assert(Asn1Tag.knownTag(byteArrayOf(12.toByte())) == "UTF8 String")
        assert(Asn1Tag.knownTag(byteArrayOf(19.toByte())) == "PrintableString")
        assert(Asn1Tag.knownTag(byteArrayOf(20.toByte())) == "T61String")
        assert(Asn1Tag.knownTag(byteArrayOf(22.toByte())) == "IA5String")
        assert(Asn1Tag.knownTag(byteArrayOf(23.toByte())) == "UTCTime")
        assert(Asn1Tag.knownTag(byteArrayOf(34.toByte())) == null)
        assert(Asn1Tag.knownTag(byteArrayOf(19.toByte(), 0x00)) == null)
    }

    @Test
    fun parse_test() {
        val byteArray = byteArrayOf(0x00, 0x04, 0x05, 0x00, 0x04, 0x05, 0x00, 0x04, 0x05)
        val back: Asn1Tag? = Asn1Tag.parse(byteArray, false)
        assert(back != null)
        val byteArray2 = byteArrayOf(31.toByte(), 0x04, 0xb, 0x00, 0x04, 0x05, 0x00, 0x04, 0x05)
        try {
            Asn1Tag.parse(byteArray2, true)
        } catch (e: Exception) {
            assert(e.message == "ASN1 non valido")
        }
        val byteArray3 = byteArrayOf(3.toByte(), 0x04, 0x05, 0x00, 0x04, 0x05, 0x00, 0x04, 0x05)
        try {
            Asn1Tag.parse(byteArray3, true)
        } catch (e: Exception) {
            assert(e.message == "ASN1 non valido")
        }
    }
}