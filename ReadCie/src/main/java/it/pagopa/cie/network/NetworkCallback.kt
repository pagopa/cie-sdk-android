package it.pagopa.cie.network

interface NetworkCallback {
    fun onSuccess(url: String)
    fun onEvent(event: Event)
    fun onError(error: Throwable)
}