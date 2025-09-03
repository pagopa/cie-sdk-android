package it.pagopa.io.app.cie_example.ui.view_model

import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.CieAtrCallback
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
import it.pagopa.io.app.cie.nfc.NfcEvents
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.cie_type.Atr
import it.pagopa.io.app.cie_example.ui.model.LazyButtonModel

class CieSdkMethodsViewModel(
    private val cieSdk: CieSDK
) : BaseViewModelWithNfcDialog(cieSdk) {
    var nfcSettingsVisible = mutableStateOf(false)
    var startCieAuth = mutableStateOf(false)
    var hasNfcCtrlOk = mutableStateOf<Boolean?>(null)
    var hasNfcEnabledCtrlOk = mutableStateOf<Boolean?>(null)
    var readyForCieAuthCtrlOk = mutableStateOf<Boolean?>(null)
    private fun initLazyButtons(
        onInitCieAuth: () -> Unit,
        onInitNisAuth: () -> Unit,
        onInitPaceProtocol: () -> Unit
    ) = listOf<LazyButtonModel>(
        LazyButtonModel(
            R.string.has_nfc,
            ctrlOk = hasNfcCtrlOk.value,
            onClick = {
                hasNfcCtrlOk.value = cieSdk.hasNfcFeature()
            }),
        LazyButtonModel(
            R.string.has_nfc_enabled,
            ctrlOk = hasNfcEnabledCtrlOk.value,
            onClick = {
                if (!cieSdk.isNfcAvailable() && cieSdk.hasNfcFeature()) {
                    nfcSettingsVisible.value = true
                }
                hasNfcEnabledCtrlOk.value = cieSdk.isNfcAvailable()
            }),
        LazyButtonModel(
            R.string.open_nfc_settings,
            isVisible = nfcSettingsVisible.value,
            onClick = {
                this.cieSdk.openNfcSettings()
            }),
        LazyButtonModel(
            R.string.ready_for_cie_auth,
            ctrlOk = readyForCieAuthCtrlOk.value,
            onClick = {
                readyForCieAuthCtrlOk.value = cieSdk.isCieAuthenticationSupported()
                startCieAuth.value = cieSdk.isCieAuthenticationSupported()
            }),
        LazyButtonModel(
            R.string.read_cie_type,
            isVisible = startCieAuth.value,
            onClick = {
                this.showDialog.value = true
            }
        ),
        LazyButtonModel(
            R.string.start_cie_auth,
            isVisible = startCieAuth.value,
            onClick = onInitCieAuth
        ),
        LazyButtonModel(
            R.string.start_nis_auth,
            isVisible = startCieAuth.value,
            onClick = onInitNisAuth
        ),
        LazyButtonModel(
            R.string.reading_pace,
            isVisible = startCieAuth.value,
            onClick = onInitPaceProtocol
        )
    )

    fun provideLazyButtons(
        onInitCieAuth: () -> Unit,
        onInitNisAuth: () -> Unit,
        onInitPaceProtocol: () -> Unit
    ) = initLazyButtons(onInitCieAuth, onInitNisAuth, onInitPaceProtocol).filter {
        it.isVisible
    }

    override fun readCie() {
        cieSdk.startReadingCieAtr(
            10000,
            object : NfcEvents {
                override fun error(error: NfcError) {
                    this@CieSdkMethodsViewModel.onError(error)
                }

                override fun event(event: NfcEvent) {
                    dialogMessage.value = event.name
                    progressValue.floatValue =
                        (event.numeratorForKindOf.toFloat() / NfcEvent.totalKindOfNumeratorEvent.toFloat()).toFloat()
                }
            }, object : CieAtrCallback {
                override fun onSuccess(atr: ByteArray) {
                    dialogMessage.value = "ALL OK!!"
                    successMessage.value =
                        "Atr B64 is: ${Base64.encodeToString(atr, Base64.DEFAULT)}"
                    errorMessage.value = "Cie type is:\n ${Atr(atr).getCieType().name}"
                    stopNfc()
                }

                override fun onError(error: NfcError) {
                    this@CieSdkMethodsViewModel.onError(error)
                }
            }
        )
    }
}