package it.pagopa.cie.nfc

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import it.pagopa.cie.cie.ApduResponse
import it.pagopa.cie.cie.NfcError
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

    override fun connect(isoDepTimeout: Int,
                         onTagDiscovered: () -> Unit,
                         actionDone: () -> Unit) {
        val activity = context.findActivity()
        try {
            adapter?.enableReaderMode(
                activity, {
                    onTagDiscovered.invoke()
                    if (isoDep == null)
                        isoDep = IsoDep.get(it)
                    isoDep?.timeout = isoDepTimeout
                    if (isoDep?.isConnected != true)
                        isoDep?.connect()
                    if (isoDep?.isConnected == true) {
                        if (!isoDep!!.isExtendedLengthApduSupported)
                            readingInterface.error(NfcError.EXTENDED_APDU_NOT_SUPPORTED)
                        else
                            actionDone.invoke()
                    } else {
                        disconnect()
                        readingInterface.error(NfcError.FAIL_TO_CONNECT_WITH_TAG)
                    }
                }, NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
        } catch (throwable: Throwable) {
            readingInterface.error(
                when (throwable) {
                    is TagLostException -> NfcError.TAG_LOST
                    else -> NfcError.GENERAL_EXCEPTION.apply {
                        this.msg = throwable.message.orEmpty()
                    }
                }
            )
            disconnect()
        }
    }

    override val readCie: ReadCie
        get() = ReadCie(object : OnTransmit {
            override fun error(error: NfcError) {
                readingInterface.error(error)
            }

            override fun sendCommand(apdu: ByteArray, message: String): ApduResponse {
                readingInterface.onTransmit(message)
                val resp = isoDep?.transceive(apdu)!!
                val (filteredByteArray, temp) = resp.transmitLogic()
                return ApduResponse(filteredByteArray, temp)
            }
        }, readingInterface)

    override fun disconnect() {
        try {
            val activity = context.findActivity()
            adapter?.disableReaderMode(activity)
            isoDep?.close()
            isoDep = null
        } catch (_: Exception) {
            readingInterface.error(NfcError.STOP_NFC_ERROR)
        }
    }
}