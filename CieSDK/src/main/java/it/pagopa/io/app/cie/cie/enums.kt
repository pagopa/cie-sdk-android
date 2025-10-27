package it.pagopa.io.app.cie.cie

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
    ASN_1_NOT_VALID,
    NIS_NO_CHALLENGE_SIGNED,
    SELECT_FILE_EXCEPTION,
    RESPONSE_EXCEPTION,
    ENCRYPTED_NONCE_NOT_FOUND
}

open class CieSdkException(private val nfcError: NfcError) : Exception() {
    fun getError() = nfcError
}

enum class NfcEvent(
    val numerator: Int = 1,
    val numeratorForKindOf: Int = 1,
    val numeratorForNis: Int = 1,
    val numeratorForPace: Int = 1,
    val numeratorForNisAndPace: Int = 1
) {
    ON_TAG_DISCOVERED(numerator = 0),
    ON_TAG_DISCOVERED_NOT_CIE,
    CONNECTED(numerator = 1),
    SELECT_IAS_SERVICE_ID(numerator = 2),
    SELECT_CIE_SERVICE_ID(numerator = 3),
    SELECT_READ_FILE_SERVICE_ID(numerator = 4),
    READ_FILE_SERVICE_ID_RESPONSE(numerator = 5),
    SELECT_EMPTY(numeratorForNisAndPace = 11),
    SELECT_IAS(
        numerator = 6,
        numeratorForKindOf = 2,
        numeratorForNis = 0,
        numeratorForNisAndPace = 13
    ),
    SELECT_CIE(
        numerator = 7,
        numeratorForKindOf = 3,
        numeratorForNis = 1,
        numeratorForNisAndPace = 14
    ),
    DH_INIT_GET_G(numerator = 8),
    DH_INIT_GET_P(numerator = 9),
    DH_INIT_GET_Q(numerator = 10),
    SELECT_FOR_READ_FILE(
        numerator = 11,
        numeratorForKindOf = 5,
        numeratorForNis = 4,
        numeratorForNisAndPace = 17
    ),
    READ_FILE(
        numerator = 12,
        numeratorForKindOf = 6,
        numeratorForNis = 5,
        numeratorForNisAndPace = 18
    ),
    READ_SOD_SELECT(numeratorForNis = 4, numeratorForNisAndPace = 17),
    READ_SOD(numeratorForNis = 5, numeratorForNisAndPace = 18),
    INIT_EXTERNAL_AUTHENTICATION(numerator = 13),
    SET_MSE(numerator = 14, numeratorForPace = 4, numeratorForNisAndPace = 4),
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
    SELECT_ROOT(numeratorForKindOf = 4),
    READ_NIS(numeratorForNis = 2, numeratorForNisAndPace = 15),
    READ_PUBLIC_KEY(numeratorForNis = 3, numeratorForNisAndPace = 16),
    READ_SOD_PACE(numeratorForPace = 12, numeratorForNisAndPace = 12),
    SETTING_NIS_AUTH(numeratorForNis = 5, numeratorForNisAndPace = 18),
    NIS_AUTHENTICATION(numeratorForNis = 6, numeratorForNisAndPace = 19),
    SELECT_AID(numeratorForPace = 0, numeratorForNisAndPace = 0),
    SELECT_PACE(numeratorForPace = 1, numeratorForNisAndPace = 1),
    SELECT_EF_CARDACCESS(numeratorForPace = 2, numeratorForNisAndPace = 2),
    READ_BINARY(numeratorForPace = 3, numeratorForNisAndPace = 3),
    GENERAL_AUTHENTICATE_STEP0(numeratorForPace = 5, numeratorForNisAndPace = 5),
    GENERAL_AUTHENTICATE_STEP1(numeratorForPace = 6, numeratorForNisAndPace = 6),
    GENERAL_AUTHENTICATE_STEP2(numeratorForPace = 7, numeratorForNisAndPace = 7),
    GENERAL_AUTHENTICATE_STEP3(numeratorForPace = 8, numeratorForNisAndPace = 8),
    SELECT_PACE_SM(numeratorForPace = 9, numeratorForNisAndPace = 9),
    READING_DG1(numeratorForPace = 10, numeratorForNisAndPace = 10),
    READING_DG11(numeratorForPace = 11, numeratorForNisAndPace = 11);

    companion object {
        /**It's plus one because of we have to wait for Network call which will be real 100%*/
        val totalNumeratorEvent = NfcEvent.entries.maxOf { it.numerator } + 1
        val totalKindOfNumeratorEvent = NfcEvent.entries.maxOf { it.numeratorForKindOf }
        val totalNisOfNumeratorEvent = NfcEvent.entries.maxOf { it.numeratorForNis }
        val totalPaceOfNumeratorEvent = NfcEvent.entries.maxOf { it.numeratorForPace }
        val totalNisAndPaceOfNumeratorEvent = NfcEvent.entries.maxOf { it.numeratorForNisAndPace }
    }
}
