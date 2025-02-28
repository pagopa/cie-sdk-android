package it.pagopa.cie_sdk.ui.view_model

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateOf
import it.pagopa.cie.CieLogger
import it.pagopa.cie.CieSDK
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent
import it.pagopa.cie.network.NetworkCallback
import it.pagopa.cie.network.NetworkError
import it.pagopa.cie.nfc.NfcEvents

class ReadCieViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk) {
    var webViewUrl =
        mutableStateOf("https://app-backend.io.italia.it/login?entityID=xx_servizicie&authLevel=SpidL3")
    var webViewLoader = mutableStateOf(true)
    var pin = mutableStateOf("")
    var shouldShowUI = mutableStateOf(false)

    inner class WebViewClientWithRedirect : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url
            CieLogger.i("WebView shouldOverrideUrlLoading", "${request?.url}")
            if (url != null && url.toString().contains("OpenApp")) {
                this@ReadCieViewModel.cieSdk.withUrl(url.toString())
                this@ReadCieViewModel.shouldShowUI.value = true
                return true
            }
            return false
        }
    }

    private fun readCie(
        pin: String
    ) {
        try {
            cieSdk.setPin(pin)
            cieSdk.startReading(10000, object : NfcEvents {
                override fun onTransmit(message: NfcEvent) {
                    dialogMessage.value = message.name
                }

                override fun error(error: NfcError) {
                    errorMessage.value = error.msg ?: error.name
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                }
            }, object : NetworkCallback {
                override fun onSuccess(url: String) {
                    dialogMessage.value = "ALL OK!!"
                    errorMessage.value = ""
                    successMessage.value = url
                    stopNfc()
                    showDialog.value = false
                    shouldShowUI.value = false
                    webViewUrl.value = url
                    webViewLoader.value = false
                }

                override fun onError(error: NetworkError) {
                    errorMessage.value = error.msg ?: error.name
                    stopNfc()
                }
            })
        } catch (e: Exception) {
            if (e is CieSdkException)
                errorMessage.value = e.getError().msg ?: e.getError().name
            else
                errorMessage.value = e.message.orEmpty()
        }
    }

    fun clear() {
        pin.value = ""
        showDialog.value = false
        errorMessage.value = ""
        successMessage.value = ""
        dialogMessage.value = ""
    }

    override fun readCie() {
        this.readCie(this.pin.value)
    }
}