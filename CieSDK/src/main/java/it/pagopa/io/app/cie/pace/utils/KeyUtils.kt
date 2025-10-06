package it.pagopa.io.app.cie.pace.utils

fun toFixedLengthUnsigned(bytes: ByteArray, length: Int): ByteArray {
    val unsigned = if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
        bytes.copyOfRange(1, bytes.size)
    } else {
        bytes
    }
    return if (unsigned.size < length) {
        ByteArray(length - unsigned.size) + unsigned
    } else if (unsigned.size > length) {
        unsigned.copyOfRange(unsigned.size - length, unsigned.size)
    } else {
        unsigned
    }
}