package it.pagopa.cie

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.provider.Settings
import android.util.Base64
import it.pagopa.cie.network.DeepLinkInfo
import it.pagopa.cie.network.Event
import it.pagopa.cie.network.EventCertificate
import it.pagopa.cie.network.EventError
import it.pagopa.cie.network.NetworkCallback
import it.pagopa.cie.network.Repository
import it.pagopa.cie.network.authnRequest
import it.pagopa.cie.network.generaCodice
import it.pagopa.cie.nfc.BaseReadCie
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
     * @throws IllegalArgumentException if pin doesn't match regex for CIE PIN*/
    @Throws(IllegalArgumentException::class)
    fun setPin(pin: String) {
        if (!ciePinRegex.matches(pin))
            throw IllegalArgumentException("pin is not matching regex..")
        this.ciePin = pin
    }

    /**It starts reading CIE
     * @param readingInterface :[BaseReadCie.ReadingCieInterface]
     * @throws Exception if setPin has not been called before*/
    @Throws(Exception::class)
    fun startReading(isoDepTimeout: Int, readingInterface: BaseReadCie.ReadingCieInterface) {
        if (!::ciePin.isInitialized)
            throw Exception("You must call setPin before start Reading CIE")
        if (this.context == null) return
        val scope = CoroutineScope(Dispatchers.IO)
        readCie = ReadCIE(
            this.context!!,
            Base64.encodeToString(byteArrayOf(0x00, 0x00), Base64.DEFAULT)
        )
        readCie?.read(scope, isoDepTimeout, readingInterface)
    }

    /**It stops NFC*/
    fun stopNFCListening() {
        readCie?.disconnect()
    }


    fun call(certificate: ByteArray, idpCustomUrl: String? = null, callback: NetworkCallback) {
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
                                    if (!Regex("^[0-9]{16}$").matches(serverCode)) {
                                        callback.onEvent(Event(EventError.GENERAL_ERROR))
                                    }
                                    val url =
                                        "${deepLinkInfo.nextUrl}?${deepLinkInfo.name}=${deepLinkInfo.value}&login=1&codice=$serverCode"
                                    callback.onSuccess(url)
                                } else {
                                    CieLogger.e(callTag, "Missing server code")
                                    callback.onEvent(Event(EventError.AUTHENTICATION_ERROR))
                                }
                            }
                        } else {
                            CieLogger.e(callTag, "RESPONSE NOT SUCCESSFULL")
                            callback.onEvent(Event(EventError.AUTHENTICATION_ERROR))
                        }
                    }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException, is UnknownHostException -> {
                        CieLogger.e(callTag, "SocketTimeoutException or UnknownHostException")
                        callback.onEvent(Event(EventError.ON_NO_INTERNET_CONNECTION))

                    }

                    is SSLProtocolException -> {

                        CieLogger.e(callTag, "SSLProtocolException")
                        e.message?.let {
                            when {
                                it.contains("SSLV3_ALERT_CERTIFICATE_EXPIRED") -> callback.onEvent(
                                    Event(
                                        EventCertificate.CERTIFICATE_EXPIRED
                                    )
                                )

                                it.contains("SSLV3_ALERT_CERTIFICATE_REVOKED") -> callback.onEvent(
                                    Event(
                                        EventCertificate.CERTIFICATE_REVOKED
                                    )
                                )

                                else -> callback.onError(e)
                            }
                        }

                    }

                    else -> callback.onError(e)
                }
            }
        }
    }

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

    companion object {
        fun withContext(context: Context?) = CieSDK().apply {
            this@apply.context = context
        }
    }
}