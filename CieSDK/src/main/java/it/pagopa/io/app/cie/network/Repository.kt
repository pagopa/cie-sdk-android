package it.pagopa.io.app.cie.network

import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.Response

internal class Repository(
    private val certificate: ByteArray,
    private val idpCustomUrl: String?
) {
    fun callIdp(values: Map<String, String>) = flow<Response<ResponseBody>> {
        emit(NetworkClient(certificate, idpCustomUrl).idpService.callIdp(values))
    }
}