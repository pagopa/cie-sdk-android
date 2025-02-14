package it.pagopa.cie_sdk.ui.view_model

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import it.pagopa.cie.CieLogger
import it.pagopa.cie.CieSDK
import it.pagopa.cie.network.Event
import it.pagopa.cie.network.NetworkCallback
import it.pagopa.cie.nfc.NfcReading

class ReadCieViewModel(
    private val cieSdk: CieSDK
) : ViewModel() {
    var pin = mutableStateOf("")
    var showDialog = mutableStateOf(false)
    var dialogMessage = mutableStateOf("")
    var errorMessage = mutableStateOf("")
    var successMessage = mutableStateOf("")
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

    fun readCie(
        pin: String
    ) {
        cieSdk.setPin(pin)
        try {
            cieSdk.startReading(10000, object : NfcReading {
                override fun <T> read(element: T) {}
                override fun onTransmit(message: String) {
                    dialogMessage.value = message
                }

                override fun error(why: String) {
                    errorMessage.value = why
                }
            }, object : NetworkCallback {
                override fun onSuccess(url: String) {
                    dialogMessage.value = "ALL OK!!"
                    errorMessage.value = ""
                    successMessage.value = url
                    stopNfc()
                }

                override fun onEvent(event: Event) {
                    dialogMessage.value = event.event.toString()
                }

                override fun onError(error: Throwable) {
                    errorMessage.value = error.message.orEmpty()
                    stopNfc()
                }
            })
        } catch (e: Exception) {
            errorMessage.value = e.message.orEmpty()
        }
    }

    fun stopNfc() {
        cieSdk.stopNFCListening()
    }

    fun clearMessages() {
        errorMessage.value = ""
        successMessage.value = ""
        dialogMessage.value = ""
    }

    fun clear() {
        pin.value = ""
        showDialog.value = false
        errorMessage.value = ""
        successMessage.value = ""
        dialogMessage.value = ""
    }
}