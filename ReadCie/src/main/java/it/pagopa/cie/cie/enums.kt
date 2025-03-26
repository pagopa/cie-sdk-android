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
    SELECT_ROOT_EXCEPTION,
    GENERAL_EXCEPTION,
    ASN_1_NOT_RIGHT_LENGTH,
    ASN_1_NOT_VALID
}

open class CieSdkException(private val nfcError: NfcError) : Exception() {
    fun getError() = nfcError
}

enum class NfcEvent(
    val numerator: Int = 1,
    val numeratorForKindOf: Int = 1
) {
    ON_TAG_DISCOVERED(numerator = 0),
    ON_TAG_DISCOVERED_NOT_CIE,
    CONNECTED(numerator = 1),
    SELECT_IAS_SERVICE_ID(numerator = 2),
    SELECT_CIE_SERVICE_ID(numerator = 3),
    SELECT_READ_FILE_SERVICE_ID(numerator = 4),
    READ_FILE_SERVICE_ID_RESPONSE(numerator = 5),
    SELECT_IAS(numerator = 6, numeratorForKindOf = 2),
    SELECT_CIE(numerator = 7, numeratorForKindOf = 3),
    DH_INIT_GET_G(numerator = 8),
    DH_INIT_GET_P(numerator = 9),
    DH_INIT_GET_Q(numerator = 10),
    SELECT_FOR_READ_FILE(numerator = 11, numeratorForKindOf = 5),
    READ_FILE(numerator = 12, numeratorForKindOf = 6),
    INIT_EXTERNAL_AUTHENTICATION(numerator = 13),
    SET_MSE(numerator = 14),
    D_H_KEY_EXCHANGE_GET_DATA(numerator = 15),
    SIGN1_SELECT(numerator = 16),
    SIGN1_VERIFY_CERT(numerator = 17),
    SET_CHALLENGE_RESPONSE(numerator = 18),
    GET_CHALLENGE_RESPONSE(numerator = 19),
    EXTERNAL_AUTHENTICATION(numerator = 20),
    INTERNAL_AUTHENTICATION(numerator = 21),
    GIVE_RANDOM(numerator = 22),
    VERIFY_PIN(numerator = 23),
    READ_FILE_SM(numerator = 24),
    SIGN(numerator = 25),
    SIGN_WITH_CIPHER(numerator = 26),
    SELECT_ROOT(numeratorForKindOf = 4);

    companion object {
        /**It's plus one because of we have to wait for Network call which will be real 100%*/
        val totalNumeratorEvent = NfcEvent.entries.maxOf { it.numerator } + 1
        val totalKindOfNumeratorEvent = NfcEvent.entries.maxOf { it.numeratorForKindOf }
    }
}
