package it.pagopa.cie.nfc

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.OnTransmit
import it.pagopa.cie.cie.ReadCie
import it.pagopa.cie.cie.transmitLogic
import it.pagopa.cie.findActivity

internal class NfcImpl private constructor() : BaseNfcImpl() {
    private var adapter: NfcAdapter? = null
    private lateinit var context: Context
    private var isoDep: IsoDep? = null

    constructor(context: Context, readingInterface: NfcReading) : this() {
        this.context = context
        this.readingInterface = readingInterface
        this.adapter = NfcAdapter.getDefaultAdapter(context)
    }

    override fun connect(isoDepTimeout: Int, actionDone: () -> Unit) {
        val activity = context.findActivity()
        try {
            adapter?.enableReaderMode(
                activity, {
                    if (isoDep == null)
                        isoDep = IsoDep.get(it)
                    isoDep?.timeout = isoDepTimeout
                    if (isoDep?.isConnected != true)
                        isoDep?.connect()
                    if (isoDep?.isConnected == true) {
                        isoDep?.timeout = 5000
                        actionDone.invoke()
                    } else {
                        disconnect()
                        readingInterface.error("no connection to nfc tag..")
                    }
                }, NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
        } catch (e: UnsupportedOperationException) {
            disconnect()
            CieLogger.e("NFC", e.toString())
        }
    }

    override val readCie: ReadCie
        get() = ReadCie(object : OnTransmit {
            override fun error(why: String) {
                disconnect()
                readingInterface.error(why)
            }

            override fun sendCommand(apdu: ByteArray, message: String): ApduResponse {
                readingInterface.onTransmit(message)
                val resp = isoDep?.transceive(apdu)!!
                val (filteredByteArray, temp) = resp.transmitLogic()
                return ApduResponse(filteredByteArray, temp)
            }
        }, readingInterface)

    override fun disconnect() {
        val activity = context.findActivity()
        adapter?.disableReaderMode(activity)
        isoDep?.close()
        isoDep = null
    }
}