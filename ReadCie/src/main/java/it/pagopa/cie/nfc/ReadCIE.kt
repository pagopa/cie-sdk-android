package it.pagopa.cie.nfc

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReadCIE(private val ownerActivity: Activity, challenge: String? = null) :
    BaseReadCie(challenge) {
    private var implementation: NfcImpl? = null
    override suspend fun workNfc(challenge: String, readingInterface: NfcReading) {
        withContext(Dispatchers.Default) {
            implementation = NfcImpl(ownerActivity, readingInterface)
            try {
                implementation!!.transmit(challenge)
            } catch (e: Exception) {
                readingInterface.error(e.message.orEmpty())
            }
        }
    }

    override fun disconnect() {
        implementation?.disconnect()
    }
}