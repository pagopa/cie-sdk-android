package it.pagopa.cie.nfc

import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.NfcEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseReadCie(
    private val pin: String? = null
) {
    /**Interface to receive events while NFC is working
     * @property onTransmit return true if we are transmitting
     * @property backResource return an interface which returns success or fail events*/
    interface ReadingCieInterface {
        fun onTransmit(value: Boolean)
        fun backResource(action: FunInterfaceResource<ByteArray>)
    }

    fun read(
        scope: CoroutineScope,
        isoDepTimeout: Int,
        nfcListener: NfcEvents,
        readingInterface: ReadingCieInterface
    ) {
        scope.launch {
            workNfc(isoDepTimeout, pin.orEmpty(), object : NfcReading {
                override fun onTransmit(message: String) {
                    CieLogger.i("message from CIE", message)
                    nfcListener.onTransmit(message)
                    if (message == "connected")
                        readingInterface.onTransmit(true)
                }

                override fun <T> read(element: T) {
                    readingInterface.backResource(FunInterfaceResource.success(element as ByteArray))
                }

                override fun error(error: NfcError) {
                    nfcListener.error(error)
                    if (error == NfcError.NOT_A_CIE)
                        nfcListener.event(NfcEvent.ON_TAG_DISCOVERED_NOT_CIE)
                    readingInterface.backResource(FunInterfaceResource.error(error))
                }
            }) {
                nfcListener.event(NfcEvent.ON_TAG_DISCOVERED)
            }
        }
    }

    internal abstract suspend fun workNfc(
        isoDepTimeout: Int,
        challenge: String,
        readingInterface: NfcReading,
        onTagDiscovered: () -> Unit
    )

    data class FunInterfaceResource<out T>(
        val status: FunInterfaceStatus,
        val data: T?,
        val nfcError: NfcError? = null
    ) {
        companion object {
            fun <T> success(data: T): FunInterfaceResource<T> =
                FunInterfaceResource(FunInterfaceStatus.SUCCESS, data)

            fun <T> error(error: NfcError): FunInterfaceResource<T> =
                FunInterfaceResource(FunInterfaceStatus.ERROR, null, error)
        }
    }

    abstract fun disconnect()
    enum class FunInterfaceStatus {
        SUCCESS,
        ERROR
    }
}