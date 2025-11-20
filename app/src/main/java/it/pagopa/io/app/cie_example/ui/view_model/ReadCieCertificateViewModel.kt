package it.pagopa.io.app.cie_example.ui.view_model

import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.CertificateData
import it.pagopa.io.app.cie.cie.CieCertificateDataCallback
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.NfcEvents

class ReadCieCertificateViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk = cieSdk) {
    var pin = mutableStateOf("")
    private fun readCie(
        pin: String
    ) {
        try {
            cieSdk.setPin(pin)
            cieSdk.startReadingCertificate(10000, object : NfcEvents {
                override fun error(error: NfcError) {
                    this@ReadCieCertificateViewModel.onError(error)
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                    progressValue.floatValue =
                        (event.numerator.toFloat() / NfcEvent.totalNumeratorEvent.toFloat())
                }
            }, object : CieCertificateDataCallback {
                override fun onSuccess(data: CertificateData) {
                    dialogMessage.value = "ALL OK!!"
                    errorMessage.value = ""
                    progressValue.floatValue = 1f
                    successMessage.value = data.toString()
                    stopNfc()
                }

                override fun onError(error: NfcError) {
                    this@ReadCieCertificateViewModel.onError(error = error)
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