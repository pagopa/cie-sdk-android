package it.pagopa.io.app.cie_example.ui.view_model

import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.IntAuthMRTDResponse
import it.pagopa.io.app.cie.NisAndPaceCallback
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.NfcEvents

class NisAndPaceViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk) {
    var intAuthMRTDResponseRead = mutableStateOf<IntAuthMRTDResponse?>(null)
    val can = mutableStateOf("")
    val challenge = mutableStateOf("")

    override fun readCie() {
        this.cieSdk.startNisAndPace(
            challenge = challenge.value,
            can = can.value,
            isoDepTimeout = 10000,
            object : NfcEvents {
                override fun error(error: NfcError) {
                    this@NisAndPaceViewModel.onError(error)
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                    progressValue.floatValue =
                        (event.numeratorForNisAndPace.toFloat() / NfcEvent.totalNisAndPaceOfNumeratorEvent.toFloat())
                }
            }, object : NisAndPaceCallback {
                override fun onSuccess(intAuthMRTDResponse: IntAuthMRTDResponse) {
                    dialogMessage.value = "ALL OK!!"
                    this@NisAndPaceViewModel.intAuthMRTDResponseRead.value = intAuthMRTDResponse
                    CieLogger.i("Nis and pace read", intAuthMRTDResponse.toTerminalString())
                    stopNfc()
                }

                override fun onError(error: NfcError) {
                    this@NisAndPaceViewModel.onError(error)
                }
            })
    }

    fun resetMainUi() {
        intAuthMRTDResponseRead.value = null
        showDialog.value = false
    }
}