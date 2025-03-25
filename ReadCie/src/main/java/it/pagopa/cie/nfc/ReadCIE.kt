package it.pagopa.cie.nfc

import android.content.Context
import it.pagopa.cie.cie.NfcError
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
        readingInterface: NfcReading,
        onTagDiscovered: () -> Unit
    ) {
        withContext(Dispatchers.Default) {
            implementation = NfcImpl(context, readingInterface)
            try {
                implementation!!.transmit(isoDepTimeout, pin, onTagDiscovered)
            } catch (e: Exception) {
                readingInterface.error(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = e.message.orEmpty()
                })
            }
        }
    }

    override suspend fun workNfcForCieAtr(
        isoDepTimeout: Int,
        readingInterface: NfcReading,
        onTagDiscovered: () -> Unit
    ) {
        withContext(Dispatchers.Default) {
            implementation = NfcImpl(context, readingInterface)
            try {
                implementation!!.readCieAtr(isoDepTimeout, onTagDiscovered)
            } catch (e: Exception) {
                readingInterface.error(NfcError.GENERAL_EXCEPTION.apply {
                    this.msg = e.message.orEmpty()
                })
            }
        }
    }

    override fun disconnect() {
        implementation?.disconnect()
    }
}