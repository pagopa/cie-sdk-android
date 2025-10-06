package it.pagopa.io.app.cie_example.ui.view_model

import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.NfcEvents
import it.pagopa.io.app.cie.nis.InternalAuthenticationResponse
import it.pagopa.io.app.cie.nis.NisCallback

class ReadNisViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk) {
    val challenge = mutableStateOf("")
    override fun readCie() {
        this.cieSdk.startReadingNis(
            challenge = challenge.value,
            isoDepTimeout = 10000,
            object : NfcEvents {
                override fun error(error: NfcError) {
                    this@ReadNisViewModel.onError(error)
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                    progressValue.floatValue =
                        (event.numeratorForNis.toFloat() / NfcEvent.totalNisOfNumeratorEvent.toFloat())
                }
            }, object : NisCallback {
                override fun onSuccess(nisAuth: InternalAuthenticationResponse) {
                    dialogMessage.value = "ALL OK!!"
                    successMessage.value = "Nis is: ${nisAuth.toStringUi()}"
                    stopNfc()
                }

                override fun onError(error: NfcError) {
                    this@ReadNisViewModel.onError(error)
                }
            })
    }
}