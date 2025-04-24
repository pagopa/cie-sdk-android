package it.pagopa.io.app.cie_example.ui.view_model

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.network.NetworkError

abstract class BaseViewModelWithNfcDialog(private val cieSdk: CieSDK) : ViewModel() {
    var showDialog = mutableStateOf(false)
    var dialogMessage = mutableStateOf("")
    var errorMessage = mutableStateOf("")
    var successMessage = mutableStateOf("")
    var showProgress = mutableStateOf(true)
    var progressValue = mutableFloatStateOf(0f)
    fun onDialogDismiss() {
        errorMessage.value = ""
        successMessage.value = ""
        dialogMessage.value = ""
        progressValue.floatValue = 0f
    }

    protected fun <T> onError(error: T) {
        if (error is NetworkError) {
            errorMessage.value = error.msg ?: error.name
            stopNfc()
            showProgress.value = false
        } else if (error is NfcError) {
            errorMessage.value = error.msg ?: error.name
            stopNfc()
            showProgress.value = false
        }
    }

    abstract fun readCie()
    fun stopNfc() {
        cieSdk.stopNFCListening()
    }
}