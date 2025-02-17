package it.pagopa.cie.network

interface NetworkCallback {
    fun onSuccess(url: String)
    fun onError(networkError: NetworkError)
}