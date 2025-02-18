package it.pagopa.cie.network

import it.pagopa.cie.CieLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLProtocolException

internal class IdpNetworkCall private constructor() {
    private var idpCustomUrl: String? = null
    private lateinit var deepLinkInfo: DeepLinkInfo
    private lateinit var callback: NetworkCallback

    /**It calls IDP service
     * @param certificate certificate signed by CIE*/
    infix fun callWith(certificate: ByteArray) {
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

    fun withDeepLinkInfo(deepLinkInfo: DeepLinkInfo) = apply {
        this.deepLinkInfo = deepLinkInfo
    }

    fun withIdpCustomUrl(idpCustomUrl: String?) = apply {
        this.idpCustomUrl = idpCustomUrl
    }

    fun withCallback(callback: NetworkCallback) = apply {
        this.callback = callback
    }

    companion object {
        fun withDeepLinkInfo(deepLinkInfo: DeepLinkInfo) = IdpNetworkCall().apply {
            this.deepLinkInfo = deepLinkInfo
        }

        fun withIdpCustomUrl(idpCustomUrl: String?) = IdpNetworkCall().apply {
            this.idpCustomUrl = idpCustomUrl
        }
    }
}