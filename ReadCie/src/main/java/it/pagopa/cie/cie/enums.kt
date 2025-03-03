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
    GENERAL_EXCEPTION
}

class CieSdkException(private val nfcError: NfcError) : Exception() {
    fun getError() = nfcError
}

enum class NfcEvent(
    val numerator: Int = 1,
    val numeratorForKindOf: Int = 1
) {
    ON_TAG_DISCOVERED(numerator = 0),
    ON_TAG_DISCOVERED_NOT_CIE,
    CONNECTED(numerator = 1),
    SELECT_ID_1(numerator = 2),
    SELECT_ID_2(numerator = 3),
    SELECT_ID_3(numerator = 4),
    SELECT_ID_GET_RESPONSE(numerator = 5),
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

/**Returns Cie Manufacturer*/
enum class CieType(val atr: ByteArray) {
    NXP(
        byteArrayOf(
            0x80.toByte(),
            0x00,
            0x43,
            0x01,
            0xB8.toByte(),
            0x46,
            0x04,
            0x10,
            0x10,
            0x10,
            0x10,
            0x47,
            0x03,
            0x94.toByte(),
            0x01,
            0x80.toByte()
        )
    ),
    GEMALTO(
        byteArrayOf(
            0x49, 0x61, 0x73, 0x45, 0x63, 0x63, 0x52, 0x6F, 0x6F, 0x74
        )
    ),
    GEMALTO_2(
        byteArrayOf(
            0x47, 0x03, 0x94.toByte(), 0x41, 0xC0.toByte()
        )
    ),
    ACTALIS(
        byteArrayOf(
            0x80.toByte(),
            0x00,
            0x43,
            0x01,
            0xB8.toByte(),
            0x46,
            0x04,
            0x10,
            0x10,
            0x10,
            0x10,
            0x47,
            0x03,
            0x94.toByte(),
            0x01,
            0xE0.toByte(),
            0x7F,
            0x66,
            0x08,
            0x02,
            0x02,
            0x04,
            0xD6.toByte(),
            0x02,
            0x02,
            0x07,
            0xE3.toByte(),
            0xE0.toByte(),
            0x10,
            0x02,
            0x02,
            0x01,
            0x04,
            0x02,
            0x02,
            0x00,
            0xE6.toByte(),
            0x02,
            0x02,
            0x00,
            0xE6.toByte(),
            0x02.toByte(),
            0x02,
            0x00,
            0xE6.toByte(),
            0x78,
            0x08,
            0x06,
            0x06,
            0x2B,
            0x81.toByte(),
            0x22,
            0xF8.toByte(),
            0x78,
            0x02
        )
    ),
    ST(
        byteArrayOf(
            0x80.toByte(),
            0x00,
            0x43,
            0x01,
            0xB9.toByte(),
            0x46,
            0x04,
            0x00,
            0x00,
            0x00,
            0x10,
            0x47,
            0x03,
            0x94.toByte(),
            0x01,
            0x81.toByte(),
            0x4F,
            0x0C,
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x95.toByte(),
            0x00,
            0x00,
            0x00,
            0x00,
            0x8A.toByte(),
            0x00,
            0x01,
            0xE0.toByte(),
            0x10,
            0x02,
            0x02,
            0x00,
            0xFF.toByte(),
            0x02,
            0x02,
            0x00,
            0xFF.toByte(),
            0x02,
            0x02,
            0x01,
            0x00,
            0x02,
            0x02,
            0x01,
            0x00,
            0x78,
            0x08,
            0x06,
            0x06,
            0x2B,
            0x81.toByte(),
            0x22,
            0xF8.toByte(),
            0x78,
            0x02
        )
    ),
    UNKNOWN(byteArrayOf())
}