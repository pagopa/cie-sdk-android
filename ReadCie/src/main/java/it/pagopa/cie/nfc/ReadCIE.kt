package it.pagopa.cie.nfc

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReadCIE(
    private val context: Context,
    challenge: String? = null
) : BaseReadCie(challenge) {
    private var implementation: NfcImpl? = null
    override suspend fun workNfc(
        isoDepTimeout: Int,
        challenge: String,
        readingInterface: NfcReading
    ) {
        withContext(Dispatchers.Default) {
            implementation = NfcImpl(context, readingInterface)
            try {
                implementation!!.transmit(isoDepTimeout, challenge)
            } catch (e: Exception) {
                readingInterface.error(e.message.orEmpty())
            }
        }
    }

    override fun disconnect() {
        implementation?.disconnect()
    }
}