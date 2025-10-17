package it.pagopa.io.app.cie_example.ui.view_model

import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.NfcEvents
import it.pagopa.io.app.cie.pace.PaceCallback
import it.pagopa.io.app.cie.pace.MRTDResponse

class PaceViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk) {
    var eMRTDResponse = mutableStateOf<MRTDResponse?>(null)
    val can = mutableStateOf("")

    override fun readCie() {
        this.cieSdk.startDoPace(
            can = can.value,
            isoDepTimeout = 10000,
            object : NfcEvents {
                override fun error(error: NfcError) {
                    this@PaceViewModel.onError(error)
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                    progressValue.floatValue =
                        (event.numeratorForPace.toFloat() / NfcEvent.totalPaceOfNumeratorEvent.toFloat())
                }
            }, object : PaceCallback {
                override fun onSuccess(eMRTDResponse: MRTDResponse) {
                    dialogMessage.value = "ALL OK!!"
                    this@PaceViewModel.eMRTDResponse.value = eMRTDResponse
                    CieLogger.i("PACE READ", eMRTDResponse.toTerminalString())
                    stopNfc()
                }

                override fun onError(error: NfcError) {
                    this@PaceViewModel.onError(error)
                }
            })
    }

    fun resetMainUi() {
        eMRTDResponse.value = null
        showDialog.value = false
    }
}