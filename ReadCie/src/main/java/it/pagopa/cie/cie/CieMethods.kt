package it.pagopa.cie.cie

fun highByte(b: Int): Byte {
    return (b shr 8 and 0xFF).toByte()
}

fun lowByte(b: Int): Byte {
    return b.toByte()
}

@Throws(Exception::class)
fun unsignedToBytes(b: Int): Byte {
    return (b and 0xFF).toByte()
}

fun ByteArray.transmitLogic(): Pair<ByteArray, ByteArray> {
    val filteredByteArray = this.copyOfRange(0, this.size - 2)
    val temp = this.copyOfRange(this.size - 2, this.size)
    return filteredByteArray to temp
}