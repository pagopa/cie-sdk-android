package it.pagopa.cie.cie

import it.pagopa.cie.CieLogger
import it.pagopa.cie.nfc.Utils

internal class ReadFileManager(private val onTransmit: OnTransmit) {
    private fun hiByte(b: Int) = (b shr 8 and 0xFF).toByte()
    private fun loByte(b: Int) = b.toByte()

    @Throws(Exception::class)
    fun readFile(id: Int): ByteArray {
        var content = byteArrayOf()
        val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
        val fileId = byteArrayOf(hiByte(id), loByte(id))
        val apduManager = ApduManager(onTransmit)
        apduManager.sendApdu(selectFile, fileId, null, "SELECT FOR READ FILE")
        var cnt = 0
        val chunk = 256
        while (true) {
            val readFile = byteArrayOf(0x00, 0xb0.toByte(), hiByte(cnt), loByte(cnt))
            val response =
                apduManager.sendApdu(
                    readFile,
                    byteArrayOf(),
                    byteArrayOf(chunk.toByte()),
                    "reading file.."
                )
            var chn = response.response
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                CieLogger.i("ENTERING", "response.swInt shr 8!!")
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val respApdu =
                    apduManager.sendApdu(
                        readFile,
                        byteArrayOf(),
                        byteArrayOf(le),
                        "response from reading"
                    )
                chn = respApdu.response
            }
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
            } else {
                if (response.swHex == "0x6282")
                    content = Utils.appendByteArray(content, chn)
                break
            }
        }
        return content
    }

    /**
     * @return a [Pair] of the manipulated sequence and the certificate in [ByteArray]*/
    @Throws(Exception::class)
    fun readFileSM(
        id: Int,
        sequence: ByteArray,
        sessionEncryption: ByteArray,
        sessMac: ByteArray,
    ): Pair<ByteArray, ByteArray> {
        var seq = sequence
        CieLogger.i("ON COMMAND", "readfileSM()")
        val secureMessageManager = ApduSecureMessageManager(onTransmit)
        var content = byteArrayOf()
        val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
        val fileId = byteArrayOf(hiByte(id), loByte(id))
        seq = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            selectFile,
            fileId,
            null
        ).first
        var cnt = 0
        var chunk = 256

        while (true) {
            val readFile = byteArrayOf(0x00, 0xb0.toByte(), hiByte(cnt), loByte(cnt))
            val pairBack = secureMessageManager.sendApduSM(
                seq,
                sessionEncryption,
                sessMac,
                readFile,
                byteArrayOf(),
                byteArrayOf(chunk.toByte())
            )
            seq = pairBack.first
            val response = pairBack.second
            var chn = response.response
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val pairBack = secureMessageManager.sendApduSM(
                    seq,
                    sessionEncryption,
                    sessMac,
                    readFile,
                    byteArrayOf(),
                    byteArrayOf(le)
                )
                seq = pairBack.first
                val respApdu = pairBack.second
                chn = respApdu.response
            }
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
                chunk = 256
            } else {
                if (response.swHex == "6282") {
                    content = Utils.appendByteArray(content, chn)
                } else if (response.swHex != "6b00") {
                    return seq to content
                }
                break
            }
        }
        return seq to content
    }
}