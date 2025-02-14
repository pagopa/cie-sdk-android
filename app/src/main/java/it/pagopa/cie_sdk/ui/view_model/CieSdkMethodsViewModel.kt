package it.pagopa.cie_sdk.ui.view_model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import it.pagopa.cie.CieSDK
import it.pagopa.cie_sdk.R
import it.pagopa.cie_sdk.ui.model.LazyButtonModel

class CieSdkMethodsViewModel(
    private val cieSdk: CieSDK
) : ViewModel() {
    var nfcSettingsVisible = mutableStateOf(false)
    var startCieAuth = mutableStateOf(false)
    var hasNfcCtrlOk = mutableStateOf<Boolean?>(null)
    var hasNfcEnabledCtrlOk = mutableStateOf<Boolean?>(null)
    var readyForCieAuthCtrlOk = mutableStateOf<Boolean?>(null)
    private fun initLazyButtons(onInitCieAuth:()-> Unit) = listOf<LazyButtonModel>(
        LazyButtonModel(R.string.has_nfc,
            ctrlOk = hasNfcCtrlOk.value,
            onClick = {
                hasNfcCtrlOk.value = cieSdk.hasNfcFeature()
            }),
        LazyButtonModel(R.string.has_nfc_enabled,
            ctrlOk = hasNfcEnabledCtrlOk.value,
            onClick = {
                if (!cieSdk.isNfcAvailable() && cieSdk.hasNfcFeature()) {
                    nfcSettingsVisible.value = true
                }
                hasNfcEnabledCtrlOk.value = cieSdk.isNfcAvailable()
            }),
        LazyButtonModel(R.string.open_nfc_settings,
            isVisible = nfcSettingsVisible.value,
            onClick = {
                this.cieSdk.openNfcSettings()
            }),
        LazyButtonModel(R.string.ready_for_cie_auth,
            ctrlOk = readyForCieAuthCtrlOk.value,
            onClick = {
                readyForCieAuthCtrlOk.value = cieSdk.isCieAuthenticationSupported()
                startCieAuth.value = cieSdk.isCieAuthenticationSupported()
            }),
        LazyButtonModel(R.string.start_cie_auth,
            isVisible = startCieAuth.value,
            onClick = onInitCieAuth
        )
    )

    fun provideLazyButtons(onInitCieAuth:()-> Unit) = initLazyButtons(onInitCieAuth).filter {
        it.isVisible
    }
}