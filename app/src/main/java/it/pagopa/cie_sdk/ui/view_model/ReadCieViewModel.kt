package it.pagopa.cie_sdk.ui.view_model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import it.pagopa.cie.CieLogger
import it.pagopa.cie.CieSDK
import it.pagopa.cie.cie.NisAuthenticated
import it.pagopa.cie.nfc.BaseReadCie

class ReadCieViewModel(
    private val cieSdk: CieSDK
) : ViewModel() {
    var pin = mutableStateOf("")
    var showDialog = mutableStateOf(false)
    var dialogMessage = mutableStateOf("")

    fun readCie(pin: String, onMessage: (String) -> Unit) {
        cieSdk.setPin(pin)
        cieSdk.startReading(10000, object : BaseReadCie.ReadingCieInterface {
            override fun onTransmit(value: Boolean) {
                CieLogger.i("READING CIE", "Transmitting :${value}")
            }

            override fun backResource(action: BaseReadCie.FunInterfaceResource<NisAuthenticated?>) {
                CieLogger.i("MESSAGE:", action.msg)
                CieLogger.i("DATA:", action.data?.toString().orEmpty())
                CieLogger.i("STATUS:", action.status.name)
                onMessage.invoke(action.status.name)
            }
        })
    }

    fun clear() {
        pin.value = ""
        showDialog.value = false
    }
}