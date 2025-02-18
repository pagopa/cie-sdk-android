package it.pagopa.cie.cie

enum class NfcError(var numberOfAttempts: Int? = null, var msg: String? = null) {
    NOT_A_CIE,
    PIN_REGEX_NOT_VALID,
    PIN_BLOCKED,
    WRONG_PIN,
    APDU_ERROR,
    VERIFY_SM_DATA_OBJECT_LENGTH,
    VERIFY_SM_MAC_LENGTH,
    VERIFY_SM_NOT_SAME_MAC,
    NOT_EXPECTED_SM_TAG,
    CHIP_AUTH_ERROR,
    EXTENDED_APDU_NOT_SUPPORTED,
    FAIL_TO_CONNECT_WITH_TAG,
    TAG_LOST,
    STOP_NFC_ERROR,
    GENERAL_EXCEPTION
}

class CieSdkException(private val nfcError: NfcError) : Exception() {
    fun getError() = nfcError
}