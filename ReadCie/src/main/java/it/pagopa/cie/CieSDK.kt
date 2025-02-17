package it.pagopa.cie

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.provider.Settings
import it.pagopa.cie.cie.CieSdkException
import it.pagopa.cie.cie.NfcError
import it.pagopa.cie.network.DeepLinkInfo
import it.pagopa.cie.network.NetworkCallback
import it.pagopa.cie.network.NetworkError
import it.pagopa.cie.network.Repository
import it.pagopa.cie.network.authnRequest
import it.pagopa.cie.network.generaCodice
import it.pagopa.cie.nfc.BaseReadCie
import it.pagopa.cie.nfc.BaseReadCie.FunInterfaceStatus
import it.pagopa.cie.nfc.NfcReading
import it.pagopa.cie.nfc.ReadCIE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLProtocolException

class CieSDK private constructor() {
    private var context: Context? = null
    private var readCie: ReadCIE? = null
    private lateinit var ciePin: String
    private val ciePinRegex = Regex("^[0-9]{8}$")
    private var deepLinkInfo: DeepLinkInfo = DeepLinkInfo()
    private var idpCustomUrl: String? = null
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
        nfcListener: NfcReading,
        callback: NetworkCallback
    ) {
        if (!::ciePin.isInitialized)
            throw Exception("You must call setPin before start Reading CIE")
        if (this.context == null) return
        val scope = CoroutineScope(Dispatchers.IO)
        readCie = ReadCIE(
            this.context!!,
            ciePin
        )
        readCie?.read(scope, isoDepTimeout, nfcListener, object : BaseReadCie.ReadingCieInterface {
            override fun onTransmit(value: Boolean) {}
            override fun backResource(action: BaseReadCie.FunInterfaceResource<ByteArray>) {
                if (action.status == FunInterfaceStatus.SUCCESS) {
                    CieLogger.i(tag, "CALLING REPOSITORY with ${action.data}")
                    this@CieSDK.call(action.data!!, callback)
                } else {
                    CieLogger.e(
                        tag,
                        "PROCESS FINISHED WITH ERROR: ${action.nfcError?.msg ?: action.nfcError?.name}"
                    )
                }
            }
        })
    }

    /**It stops NFC*/
    fun stopNFCListening() {
        readCie?.disconnect()
    }

    /**It calls IDP service*/
    private fun call(certificate: ByteArray, callback: NetworkCallback) {
        val callTag = "CALLING IDP"
        val repo: Repository = Repository(certificate, idpCustomUrl)
        val mapValues = hashMapOf<String, String>().apply {
            put(deepLinkInfo.name, deepLinkInfo.value)
            put(authnRequest, deepLinkInfo.authnRequest)
            put(generaCodice, "1")
        }
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                repo.callIdp(mapValues)
                    .flowOn(Dispatchers.IO)
                    .collectLatest { response ->
                        if (response.isSuccessful) {
                            CieLogger.i(callTag, "SUCCESS")
                            val responseBody = response.body()
                            CieLogger.i(callTag, "$responseBody")
                            if (responseBody != null) {
                                val responseString = responseBody.string()
                                val responseParts = responseString
                                    .split(":".toRegex())
                                    .dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                                if (responseParts.size >= 2) {
                                    val serverCode = responseParts[1]
                                    if (!Regex("^[0-9]{16}$").matches(serverCode))
                                        callback.onError(NetworkError.NOT_VALID_SERVER_CODE)
                                    val url =
                                        "${deepLinkInfo.nextUrl}?${deepLinkInfo.name}=${deepLinkInfo.value}&login=1&codice=$serverCode"
                                    callback.onSuccess(url)
                                } else {
                                    CieLogger.e(callTag, "Missing server code")
                                    callback.onError(NetworkError.NO_SERVER_CODE)
                                }
                            }
                        } else {
                            CieLogger.e(callTag, "RESPONSE NOT SUCCESSFULL")
                            callback.onError(NetworkError.AUTHENTICATION_ERROR)
                        }
                    }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException, is UnknownHostException -> {
                        CieLogger.e(callTag, "SocketTimeoutException or UnknownHostException")
                        callback.onError(NetworkError.NO_INTERNET_CONNECTION)
                    }

                    is SSLProtocolException -> {

                        CieLogger.e(callTag, "SSLProtocolException")
                        e.message?.let {
                            when {
                                it.contains("SSLV3_ALERT_CERTIFICATE_EXPIRED") -> callback.onError(
                                    NetworkError.CERTIFICATE_EXPIRED
                                )

                                it.contains("SSLV3_ALERT_CERTIFICATE_REVOKED") -> callback.onError(
                                    NetworkError.CERTIFICATE_REVOKED
                                )

                                else -> callback.onError(NetworkError.GENERAL_ERROR.apply {
                                    this.msg = it
                                })
                            }
                        }

                    }

                    else -> callback.onError(NetworkError.GENERAL_ERROR.apply {
                        this.msg = e.message.orEmpty()
                    })
                }
            }
        }
    }

    /**it applies the url found by web_view which contains OpenApp to header params
     * with [DeepLinkInfo] class*/
    fun withUrl(url: String) = apply {
        val appLinkData = Uri.parse(url)
        deepLinkInfo = DeepLinkInfo(
            value = appLinkData.getQueryParameter(DeepLinkInfo.KEY_VALUE),
            name = appLinkData.getQueryParameter(DeepLinkInfo.KEY_NAME),
            authnRequest = appLinkData.getQueryParameter(DeepLinkInfo.KEY_AUTHN_REQUEST_STRING),
            nextUrl = appLinkData.getQueryParameter(DeepLinkInfo.KEY_NEXT_UTL),
            opText = appLinkData.getQueryParameter(DeepLinkInfo.KEY_OP_TEXT),
            host = appLinkData.host,
            logo = appLinkData.getQueryParameter(DeepLinkInfo.KEY_LOGO)
        )
    }

    fun setCustomIdpUrl(idpUrl: String?) {
        this.idpCustomUrl = idpUrl
    }

    companion object {
        fun withContext(context: Context?) = CieSDK().apply {
            this@apply.context = context
        }
    }
}