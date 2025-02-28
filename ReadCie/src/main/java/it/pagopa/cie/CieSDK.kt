package it.pagopa.cie

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.provider.Settings
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.CieType
import it.pagopa.cie.cie.CieTypeCallback
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.cie.commands.ciePinRegex
import it.pagopa.cie.network.DeepLinkInfo
import it.pagopa.cie.network.IdpNetworkCall
import it.pagopa.cie.network.NetworkCallback
import it.pagopa.cie.nfc.BaseReadCie
import it.pagopa.cie.nfc.BaseReadCie.FunInterfaceStatus
import it.pagopa.cie.nfc.NfcEvents
import it.pagopa.cie.nfc.ReadCIE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class CieSDK private constructor() {
    private var context: Context? = null
    private var readCie: ReadCIE? = null
    private lateinit var ciePin: String
    private lateinit var idpNetworkCall: IdpNetworkCall
    private val tag = this.javaClass.name

    /**It checks if device has NFC feature*/
    fun hasNfcFeature() = NfcAdapter.getDefaultAdapter(context) != null

    /**It checks if device has NFC enabled or not*/
    fun isNfcAvailable() = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    /**It checks if device has NFC feature and it's enabled or not*/
    fun isCieAuthenticationSupported() = this.hasNfcFeature() && this.isNfcAvailable()

    /**It opens NFC Settings*/
    fun openNfcSettings() {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        context?.startActivity(intent)
    }

    /**It sets PIN to use from SDK
     * @throws CieSdkException if pin doesn't match regex for CIE PIN*/
    @Throws(CieSdkException::class)
    fun setPin(pin: String) {
        if (!ciePinRegex.matches(pin))
            throw CieSdkException(NfcError.PIN_REGEX_NOT_VALID)
        this.ciePin = pin
    }

    /**It starts reading CIE
     * @param isoDepTimeout : Timeout to set on nfc reader
     * @param callback : [NetworkCallback]
     * @throws Exception if setPin has not been called before*/
    @Throws(Exception::class)
    fun startReading(
        isoDepTimeout: Int,
        nfcListener: NfcEvents,
        callback: NetworkCallback
    ) {
        if (!::ciePin.isInitialized)
            throw Exception("You must call setPin before start Reading CIE")
        if (this.context == null)
            throw Exception("Context not initialized well, is null..")
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job + SupervisorJob())
        readCie = ReadCIE(
            this.context!!,
            ciePin
        )
        if (this@CieSDK::idpNetworkCall.isInitialized)
            idpNetworkCall.withReadCieJob(job)
        else
            throw Exception("You never call withUrl method to initialize the header!!")
        readCie?.read(scope, isoDepTimeout, nfcListener, object : BaseReadCie.ReadingCieInterface {
            override fun onTransmit(value: Boolean) {}
            override fun <T> backResource(action: BaseReadCie.FunInterfaceResource<T>) {
                if (action.status == FunInterfaceStatus.SUCCESS) {
                    CieLogger.i(tag, "CALLING REPOSITORY with ${action.data}")
                    idpNetworkCall.withCallback(callback) callWith action.data!! as ByteArray
                } else {
                    CieLogger.e(
                        tag,
                        "PROCESS FINISHED WITH ERROR: ${action.nfcError?.msg ?: action.nfcError?.name}"
                    )
                }
            }
        })
    }


    /**It starts reading CIE Type
     * @param isoDepTimeout  Timeout to set on nfc reader
     * @param nfcListener [NfcEvents]
     * @param callback [CieTypeCallback]
     * @throws Exception if context is not initialized*/
    @Throws(Exception::class)
    fun startReadingCieType(
        isoDepTimeout: Int,
        nfcListener: NfcEvents,
        callback: CieTypeCallback
    ) {
        if (this.context == null)
            throw Exception("Context not initialized well, is null..")
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job + SupervisorJob())
        readCie = ReadCIE(this.context!!)
        readCie?.readCieType(
            scope,
            isoDepTimeout,
            nfcListener,
            object : BaseReadCie.ReadingCieInterface {
                override fun onTransmit(value: Boolean) {}
                override fun <T> backResource(action: BaseReadCie.FunInterfaceResource<T>) {
                    if (action.status == FunInterfaceStatus.SUCCESS) {
                        CieLogger.i(tag, "CALLING REPOSITORY with ${action.data}")
                        callback.onSuccess(action.data!! as CieType)
                    } else {
                        callback.onError(action.nfcError ?: NfcError.GENERAL_EXCEPTION)
                    }
                    job.cancel()
                }
            })
    }

    /**It stops NFC*/
    fun stopNFCListening() {
        readCie?.disconnect()
    }

    /**it applies the url found by web_view which contains OpenApp to header params
     * with [DeepLinkInfo] class*/
    fun withUrl(url: String) = apply {
        val appLinkData = Uri.parse(url)
        val deepLinkInfo = DeepLinkInfo(
            value = appLinkData.getQueryParameter(DeepLinkInfo.KEY_VALUE),
            name = appLinkData.getQueryParameter(DeepLinkInfo.KEY_NAME),
            authnRequest = appLinkData.getQueryParameter(DeepLinkInfo.KEY_AUTHN_REQUEST_STRING),
            nextUrl = appLinkData.getQueryParameter(DeepLinkInfo.KEY_NEXT_UTL),
            opText = appLinkData.getQueryParameter(DeepLinkInfo.KEY_OP_TEXT),
            host = appLinkData.host,
            logo = appLinkData.getQueryParameter(DeepLinkInfo.KEY_LOGO)
        )
        if (!this::idpNetworkCall.isInitialized)
            idpNetworkCall = IdpNetworkCall.withDeepLinkInfo(deepLinkInfo)
        else
            idpNetworkCall.withDeepLinkInfo(deepLinkInfo)
    }

    fun setCustomIdpUrl(idpUrl: String?) {
        if (!this::idpNetworkCall.isInitialized)
            idpNetworkCall = IdpNetworkCall.withIdpCustomUrl(idpUrl)
        else
            idpNetworkCall.withIdpCustomUrl(idpUrl)
    }

    companion object {
        fun withContext(context: Context?) = CieSDK().apply {
            this@apply.context = context
        }
    }
}