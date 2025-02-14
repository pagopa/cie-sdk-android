package it.pagopa.cie_sdk.ui.view_model

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            CieLogger.i("WebView shouldInterceptRequest", "${request?.url}")
            return super.shouldInterceptRequest(view, request)
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            CieLogger.i("WebView onLoadResource", url.orEmpty())
            super.onLoadResource(view, url)
        }

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
        pin: String,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        cieSdk.setPin(pin)
        try {
            cieSdk.startReading(10000, object : NfcReading {
                override fun <T> read(element: T) {}
                override fun onTransmit(message: String) {
                    onMessage.invoke(message)
                }

                override fun error(why: String) {
                    onError.invoke(why)
                }
            }, object : NetworkCallback {
                override fun onSuccess(url: String) {
                    onSuccess.invoke(url)
                }

                override fun onEvent(event: Event) {
                    onMessage.invoke(event.event.toString())
                }

                override fun onError(error: Throwable) {
                    onError.invoke(error.message.orEmpty())
                }
            })
        } catch (e: Exception) {
            onError.invoke(e.message.orEmpty())
        }
    }

    fun clear() {
        pin.value = ""
        showDialog.value = false
        errorMessage.value = ""
        successMessage.value = ""
        dialogMessage.value = ""
    }
}