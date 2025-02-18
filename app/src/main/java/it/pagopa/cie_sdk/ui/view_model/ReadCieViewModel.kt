package it.pagopa.cie_sdk.ui.view_model

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
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
        try {
            cieSdk.setPin(pin)
            cieSdk.startReading(10000, object : NfcEvents {
                override fun onTransmit(message: String) {
                    dialogMessage.value = message
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