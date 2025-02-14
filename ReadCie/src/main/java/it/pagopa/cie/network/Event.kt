package it.pagopa.cie.network

interface EventEnum

enum class EventTag : EventEnum {
    ON_TAG_DISCOVERED_NOT_CIE,
    ON_TAG_DISCOVERED,
    ON_TAG_LOST;
}

enum class EventCard : EventEnum {
    ON_CARD_PIN_LOCKED,
    ON_PIN_ERROR;
}

enum class EventCertificate : EventEnum {
    //certificate
    CERTIFICATE_EXPIRED,
    CERTIFICATE_REVOKED;
}

enum class EventSmartphone : EventEnum {
    EXTENDED_APDU_NOT_SUPPORTED
}

enum class EventError : EventEnum {
    AUTHENTICATION_ERROR,
    STOP_NFC_ERROR,
    START_NFC_ERROR,
    GENERAL_ERROR,
    PIN_INPUT_ERROR,
    ON_NO_INTERNET_CONNECTION;
}

data class Event(var event: EventEnum, var attemptsLeft: Int? = null, var url: String? = null)