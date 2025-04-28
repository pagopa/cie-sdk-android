package it.pagopa.io.app.cie.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface IdpService {
    @FormUrlEncoded
    @POST(idpUrl)
    suspend fun callIdp(@FieldMap(encoded = true) values: Map<String, String>): Response<ResponseBody>
}