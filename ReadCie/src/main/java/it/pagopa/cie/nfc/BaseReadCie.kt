package it.pagopa.cie.nfc

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.NisAuthenticated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseReadCie(
    private val challenge: String? = null
) {
    interface ReadingCieInterface {
        fun onTransmit(value: Boolean)
        fun backResource(action: FunInterfaceResource<NisAuthenticated?>)
    }

    fun read(scope: CoroutineScope, readingInterface: ReadingCieInterface) {
        scope.launch {
            workNfc(challenge.orEmpty(), object : NfcReading {
                override fun onTransmit(message: String) {
                    CieLogger.i("message from CIE", message)
                    if (message == "connected")
                        readingInterface.onTransmit(true)
                }

                override fun <T> read(element: T) {
                    readingInterface.backResource(FunInterfaceResource.success(element as? NisAuthenticated))
                }

                override fun error(why: String) {
                    readingInterface.backResource(FunInterfaceResource.error(why))
                }
            })
        }
    }

    abstract suspend fun workNfc(
        challenge: String,
        readingInterface: NfcReading
    )

    data class FunInterfaceResource<out T>(val status: FunInterfaceStatus, val data: T?, val msg: String = "") {
        companion object {
            fun <T> success(data: T): FunInterfaceResource<T> = FunInterfaceResource(FunInterfaceStatus.SUCCESS, data)
            fun <T> error(msg: String): FunInterfaceResource<T> = FunInterfaceResource(FunInterfaceStatus.ERROR, null, msg)
        }
    }

    abstract fun disconnect()
    enum class FunInterfaceStatus {
        SUCCESS,
        ERROR
    }
}