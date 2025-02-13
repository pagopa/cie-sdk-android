package it.pagopa.cie.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.Response

internal class Repository(
    private val certificate: ByteArray,
    private val idpCustomUrl: String?
) {
    fun callIdp(values: Map<String, String>): Flow<Response<ResponseBody>> =
        flow {
            emit(NetworkClient(certificate, idpCustomUrl).idpService.callIdp(values))
        }
}