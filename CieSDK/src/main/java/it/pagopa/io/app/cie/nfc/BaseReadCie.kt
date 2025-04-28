package it.pagopa.io.app.cie.nfc

import it.pagopa.io.app.cie.CieLogger
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.NfcEvent
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
        fun <T> backResource(action: FunInterfaceResource<T>)
    }

    fun read(
        scope: CoroutineScope,
        isoDepTimeout: Int,
        nfcListener: NfcEvents,
        readingInterface: ReadingCieInterface
    ) {
        scope.launch {
            workNfc(isoDepTimeout, pin.orEmpty(), object : NfcReading {
                override fun onTransmit(message: NfcEvent) {
                    CieLogger.i("message from CIE", message.name)
                    nfcListener.event(message)
                    if (message == NfcEvent.CONNECTED)
                        readingInterface.onTransmit(true)
                }

                override fun <T> read(element: T) {
                    readingInterface.backResource(FunInterfaceResource.success(element as ByteArray))
                }

                override fun error(error: NfcError) {
                    nfcListener.error(error)
                    if (error == NfcError.NOT_A_CIE)
                        nfcListener.event(NfcEvent.ON_TAG_DISCOVERED_NOT_CIE)
                    readingInterface.backResource<Any>(FunInterfaceResource.error(error))
                }
            }) {
                nfcListener.event(NfcEvent.ON_TAG_DISCOVERED)
            }
        }
    }

    fun readCieAtr(
        scope: CoroutineScope,
        isoDepTimeout: Int,
        nfcListener: NfcEvents,
        readingInterface: ReadingCieInterface
    ) {
        scope.launch {
            workNfcForCieAtr(isoDepTimeout, object : NfcReading {
                override fun onTransmit(message: NfcEvent) {
                    CieLogger.i("message from CIE", message.name)
                    nfcListener.event(message)
                    if (message == NfcEvent.CONNECTED)
                        readingInterface.onTransmit(true)
                }

                override fun <T> read(element: T) {
                    readingInterface.backResource(FunInterfaceResource.success(element as ByteArray))
                }

                override fun error(error: NfcError) {
                    nfcListener.error(error)
                    if (error == NfcError.NOT_A_CIE)
                        nfcListener.event(NfcEvent.ON_TAG_DISCOVERED_NOT_CIE)
                    readingInterface.backResource<Any>(FunInterfaceResource.error(error))
                }
            }) {
                nfcListener.event(NfcEvent.ON_TAG_DISCOVERED)
            }
        }
    }

    internal abstract suspend fun workNfc(
        isoDepTimeout: Int,
        pin: String,
        readingInterface: NfcReading,
        onTagDiscovered: () -> Unit
    )

    internal abstract suspend fun workNfcForCieAtr(
        isoDepTimeout: Int,
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