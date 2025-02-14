package it.pagopa.cie.nfc

import it.pagopa.cie.CieLogger
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
        nfcListener: NfcReading,
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
                    nfcListener.read(element)
                    readingInterface.backResource(FunInterfaceResource.success(element as ByteArray))
                }

                override fun error(why: String) {
                    nfcListener.error(why)
                    readingInterface.backResource(FunInterfaceResource.error(why))
                }
            })
        }
    }

    abstract suspend fun workNfc(
        isoDepTimeout: Int,
        challenge: String,
        readingInterface: NfcReading
    )

    data class FunInterfaceResource<out T>(
        val status: FunInterfaceStatus,
        val data: T?,
        val msg: String = ""
    ) {
        companion object {
            fun <T> success(data: T): FunInterfaceResource<T> =
                FunInterfaceResource(FunInterfaceStatus.SUCCESS, data)

            fun <T> error(msg: String): FunInterfaceResource<T> =
                FunInterfaceResource(FunInterfaceStatus.ERROR, null, msg)
        }
    }

    abstract fun disconnect()
    enum class FunInterfaceStatus {
        SUCCESS,
        ERROR
    }
}