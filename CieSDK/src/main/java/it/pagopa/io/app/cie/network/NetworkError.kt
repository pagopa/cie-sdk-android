package it.pagopa.io.app.cie.network

enum class NetworkError(var msg: String? = null) {
    NO_INTERNET_CONNECTION,
    CERTIFICATE_EXPIRED,
    CERTIFICATE_REVOKED,
    AUTHENTICATION_ERROR,
    NOT_VALID_SERVER_CODE,
    NO_SERVER_CODE,
    GENERAL_ERROR
}