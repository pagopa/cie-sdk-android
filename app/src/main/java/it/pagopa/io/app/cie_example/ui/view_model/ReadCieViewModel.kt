package it.pagopa.io.app.cie_example.ui.view_model

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.network.NetworkCallback
import it.pagopa.io.app.cie.network.NetworkError
import it.pagopa.io.app.cie.nfc.NfcEvents

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
                override fun error(error: NfcError) {
                    this@ReadCieViewModel.onError(error)
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                    progressValue.floatValue =
                        (event.numerator.toFloat() / NfcEvent.totalNumeratorEvent.toFloat()).toFloat()
                }
            }, object : NetworkCallback {
                override fun onSuccess(url: String) {
                    dialogMessage.value = "ALL OK!!"
                    errorMessage.value = ""
                    progressValue.floatValue = 1f
                    successMessage.value = url
                    stopNfc()
                    showDialog.value = false
                    shouldShowUI.value = false
                    webViewUrl.value = url
                    webViewLoader.value = false
                }

                override fun onError(error: NetworkError) {
                    this@ReadCieViewModel.onError(error)
                }
            })
        } catch (e: Exception) {
            if (e is CieSdkException)
                errorMessage.value = e.getError().msg ?: e.getError().name
            else
                errorMessage.value = e.message.orEmpty()
        }
    }

    override fun readCie() {
        this.readCie(this.pin.value)
    }
}