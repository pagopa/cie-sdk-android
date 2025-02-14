package it.pagopa.cie.nfc

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReadCIE(
    private val context: Context,
    pin: String? = null
) : BaseReadCie(pin) {
    private var implementation: NfcImpl? = null
    override suspend fun workNfc(
        isoDepTimeout: Int,
        pin: String,
        readingInterface: NfcReading
    ) {
        withContext(Dispatchers.Default) {
            implementation = NfcImpl(context, readingInterface)
            try {
                implementation!!.transmit(isoDepTimeout, pin)
            } catch (e: Exception) {
                readingInterface.error(e.message.orEmpty())
            }
        }
    }

    override fun disconnect() {
        implementation?.disconnect()
    }
}