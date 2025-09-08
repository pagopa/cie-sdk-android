package it.pagopa.io.app.cie_example.ui.view_model

import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.NfcEvents
import it.pagopa.io.app.cie.pace.PaceCallback
import it.pagopa.io.app.cie.pace.PaceRead

class PaceViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk) {
    var paceRead = mutableStateOf<PaceRead?>(null)
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
                override fun onSuccess(paceRead: PaceRead) {
                    dialogMessage.value = "ALL OK!!"
                    this@PaceViewModel.paceRead.value = paceRead
                    CieLogger.i("PACE READ", paceRead.toTerminalString())
                    stopNfc()
                }

                override fun onError(error: NfcError) {
                    this@PaceViewModel.onError(error)
                }
            })
    }

    fun resetMainUi() {
        paceRead.value = null
        showDialog.value = false
    }
}