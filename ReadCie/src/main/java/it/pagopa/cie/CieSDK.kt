package it.pagopa.cie

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import it.pagopa.cie.nfc.BaseReadCie
import it.pagopa.cie.nfc.ReadCIE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class CieSDK private constructor() {
    private var context: Context? = null

    /**It checks if device has NFC feature*/
    fun hasNfcFeature() = NfcAdapter.getDefaultAdapter(context) != null

    /**It checks if device has NFC enabled or not*/
    fun isNfcAvailable() = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    /**It checks if device has NFC feature and it's enabled or not*/
    fun isCieAuthenticationSupported() = this.hasNfcFeature() && this.isNfcAvailable()

    /**It opens NFC Settings*/
    fun openNfcSettings() {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        context?.startActivity(intent)
    }

    /**It starts reading CIE*/
    fun startReading(readingInterface: BaseReadCie.ReadingCieInterface) {
        val activity = context?.findActivity()
        if (activity == null) return
        val scope = CoroutineScope(Dispatchers.IO)
        ReadCIE(activity, "challenge").read(scope, readingInterface)
    }

    companion object {
        fun withContext(context: Context?) = CieSDK().apply {
            this@apply.context = context
        }
    }
}