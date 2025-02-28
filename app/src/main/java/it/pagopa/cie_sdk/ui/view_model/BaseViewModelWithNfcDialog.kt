package it.pagopa.cie_sdk.ui.view_model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import it.pagopa.cie.CieSDK

abstract class BaseViewModelWithNfcDialog(private val cieSdk: CieSDK) : ViewModel() {
    var showDialog = mutableStateOf(false)
    var dialogMessage = mutableStateOf("")
    var errorMessage = mutableStateOf("")
    var successMessage = mutableStateOf("")
    fun onDialogDismiss(){
        errorMessage.value = ""
        successMessage.value = ""
        dialogMessage.value = ""
    }
    abstract fun readCie()
    fun stopNfc() {
        cieSdk.stopNFCListening()
    }
}