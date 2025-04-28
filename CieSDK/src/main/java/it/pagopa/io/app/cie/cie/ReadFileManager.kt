package it.pagopa.io.app.cie.cie

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.nfc.Utils

/**9.7.1 SELECT of IAS ECC*/
internal class ReadFileManager(private val onTransmit: OnTransmit) {
    private fun hiByte(b: Int) = (b shr 8 and 0xFF).toByte()
    private fun loByte(b: Int) = b.toByte()
    private val selectFile = byteArrayOf(0x00, 0xa4.toByte(), 0x02, 0x04)
    private fun apduReadBinary(hiByte: Byte, loByte: Byte): ByteArray {
        return byteArrayOf(0x00, 0xb0.toByte(), hiByte, loByte)
    }

    private val maxPacketSize = 256

    @Throws(Exception::class)
    fun readFile(id: Int): ByteArray {
        var content = byteArrayOf()
        val fileId = byteArrayOf(hiByte(id), loByte(id))
        val apduManager = ApduManager(onTransmit)
        apduManager.sendApdu(selectFile, fileId, null, NfcEvent.SELECT_FOR_READ_FILE)
        var cnt = 0
        //9.7.2 READ BINARY
        while (true) {
            val readFile = apduReadBinary(hiByte(cnt), loByte(cnt))
            val response = apduManager.sendApdu(
                readFile,
                byteArrayOf(),
                byteArrayOf(maxPacketSize.toByte()),
                NfcEvent.READ_FILE
            )
            var chn = response.response
            //0x6c means wrong length so we have to finish reading last bytes
            if ((response.swInt shr 8).toByte().compareTo(0x6c.toByte()) == 0) {
                CieLogger.i("ENTERING", "response.swInt shr 8!!")
                val le = Utils.unsignedToBytes(response.swInt and 0xff)
                val respApdu = apduManager.sendApdu(
                    readFile,
                    byteArrayOf(),
                    byteArrayOf(le),
                    NfcEvent.READ_FILE
                )
                chn = respApdu.response
            }
            //if 9000 continue reading
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
            } else {
                //if 0x6282 (end of file record reached before reading le bytes) appending last content
                if (response.swHex == "6282")
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
        val fileId = byteArrayOf(hiByte(id), loByte(id))
        seq = secureMessageManager.sendApduSM(
            seq,
            sessionEncryption,
            sessMac,
            selectFile,
            fileId,
            null,
            NfcEvent.READ_FILE_SM
        ).first
        var cnt = 0
        while (true) {
            val readFile = apduReadBinary(hiByte(cnt), loByte(cnt))
            val pairBack = secureMessageManager.sendApduSM(
                seq,
                sessionEncryption,
                sessMac,
                readFile,
                byteArrayOf(),
                byteArrayOf(maxPacketSize.toByte()),
                NfcEvent.READ_FILE_SM
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
                    byteArrayOf(le),
                    NfcEvent.READ_FILE_SM
                )
                seq = pairBack.first
                val respApdu = pairBack.second
                chn = respApdu.response
            }
            //if 9000 continue reading
            if (response.swHex == "9000") {
                content = Utils.appendByteArray(content, chn)
                cnt += chn.size
            } else {
                //if 6282 (end of file record reached before reading le bytes) appending last content
                if (response.swHex == "6282")
                    content = Utils.appendByteArray(content, chn)
                break
            }
        }
        return seq to content
    }
}