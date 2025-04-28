package it.pagopa.io.app.cie.network

interface NetworkCallback {
    fun onSuccess(url: String)
    fun onError(networkError: NetworkError)
}