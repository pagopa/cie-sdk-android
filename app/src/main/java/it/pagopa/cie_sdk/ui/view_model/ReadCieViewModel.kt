package it.pagopa.cie_sdk.ui.view_model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import it.pagopa.cie.CieSDK

class ReadCieViewModel(
    private val cieSdk: CieSDK
) : ViewModel() {
    var pin = mutableStateOf("")
}