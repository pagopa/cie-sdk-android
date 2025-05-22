package it.pagopa.io.app.cie

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import android.util.Base64
import androidx.core.net.toUri
import it.pagopa.io.app.cie.cie.CieAtrCallback
import it.pagopa.io.app.cie.cie.CieSdkException
import it.pagopa.io.app.cie.cie.NfcError
import it.pagopa.io.app.cie.cie.commands.ciePinRegex
import it.pagopa.io.app.cie.cie.validity_check.CieCertificateControl
import it.pagopa.io.app.cie.network.DeepLinkInfo
import it.pagopa.io.app.cie.network.IdpNetworkCall
import it.pagopa.io.app.cie.network.NetworkCallback
import it.pagopa.io.app.cie.nfc.BaseReadCie
import it.pagopa.io.app.cie.nfc.BaseReadCie.FunInterfaceStatus
import it.pagopa.io.app.cie.nfc.NfcEvents
import it.pagopa.io.app.cie.nfc.ReadCIE
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
                    if (CieLogger.enabled) {
                        val b64 = Base64.encodeToString(action.data as ByteArray, Base64.DEFAULT)
                        CieLogger.i(tag, "CALLING REPOSITORY with $b64")
                    }
                    idpNetworkCall.withCallback(callback) callWith action.data as ByteArray
                } else {
                    CieLogger.e(
                        tag,
                        "PROCESS FINISHED WITH ERROR: ${action.nfcError?.msg ?: action.nfcError?.name}"
                    )
                }
            }
        })
    }

    /**It starts reading CIE Atr to read CIE TYPE
     * @param isoDepTimeout  Timeout to set on nfc reader
     * @param nfcListener [NfcEvents]
     * @param callback [CieAtrCallback]
     * @throws Exception if context is not initialized*/
    @Throws(Exception::class)
    fun startReadingCieAtr(
        isoDepTimeout: Int,
        nfcListener: NfcEvents,
        callback: CieAtrCallback
    ) {
        if (this.context == null)
            throw Exception("Context not initialized well, is null..")
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job + SupervisorJob())
        readCie = ReadCIE(this.context!!)
        readCie?.readCieAtr(
            scope,
            isoDepTimeout,
            nfcListener,
            object : BaseReadCie.ReadingCieInterface {
                override fun onTransmit(value: Boolean) {}
                override fun <T> backResource(action: BaseReadCie.FunInterfaceResource<T>) {
                    if (action.status == FunInterfaceStatus.SUCCESS) {
                        val cieCertificate = action.data as ByteArray
                        if (CieLogger.enabled) {
                            val b64 = Base64.encodeToString(cieCertificate, Base64.DEFAULT)
                            CieLogger.i(tag, "Cie Type found $b64")
                        }
                        if (CieCertificateControl(cieCertificate).isCertificateValid())
                            callback.onSuccess(cieCertificate)
                        else
                            callback.onError(NfcError.CIE_CERTIFICATE_NOT_VALID)
                    } else
                        callback.onError(action.nfcError ?: NfcError.GENERAL_EXCEPTION)
                    job.cancel()
                }
            }
        )
    }

    /**It stops NFC*/
    fun stopNFCListening() {
        readCie?.disconnect()
    }

    /**it applies the url found by web_view which contains OpenApp to header params
     * with [DeepLinkInfo] class*/
    fun withUrl(url: String) = apply {
        val appLinkData = url.toUri()
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

    /**It sets idp Url for network call for MTLS, if this method is not called, the url applied will
     *be which one in buildConfig parameter in sdk's gradle.
     * @param idpUrl the custom url to apply*/
    fun withCustomIdpUrl(idpUrl: String?) = apply {
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