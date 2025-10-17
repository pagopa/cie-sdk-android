# CIE SDK

This projects is made to do CIE authentication and read cie manufacturer. </br>

Exposed class is CieSDK class. This class has 10 exposed methods:

1. hasNfcFeature: It checks if device has NFC feature.
2. isNfcAvailable: It checks if device has NFC enabled or not.
3. isCieAuthenticationSupported: true if device has NFC feature and it's enabled.
4. openNfcSettings: It opens device NFC settings.
5. setPin: It sets PIN to use from SDK.
6. startReading: It starts reading CIE for authentication.
7. startReadingCieAtr: It starts reading CIE Atr to read CIE TYPE.
8. startReadingNis: It starts reading NIS for Internal Authentication.
9. startDoPace: It starts reading CIE to perform Pace flow and giving back MRTDResponse.
10. startNisAndPace: It starts reading CIE to perform Nis and Pace flow and giving back IntAuthMRTDResponse.
11. stopNFCListening: It stops NFC.
12. withUrl: it applies the url found by web_view which contains OpenApp to header params.
13. withCustomIdpUrl: It sets idp Url for network call for MTLS, if this method is not called, the url applied will be which one in buildConfig parameter in sdk's gradle.

To construct the class you must call .withContext(context:Context) static method as the example below:
```kotlin
val cieSdk = CieSDK.withContext(ctx)
```

While reading cie you will have two interfaces to know in which point we are:
```kotlin
interface NfcEvents {
    fun error(error: NfcError)
    fun event(event: NfcEvent)
}
```
```kotlin
interface NetworkCallback {
    fun onSuccess(url: String)
    fun onError(networkError: NetworkError)
}
```
NfcEvents has two methods:
1. error: kind of NfcError.
2. event: kind of NfcEvent.

NfcError:
```kotlin
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
```
msg will advise the sdk user with a string message about the error. </br>
numberOfAttempts is only about CIE pin, user has 3 attempts else he/she should reset pin using PUK.</br>
NfcEvent:
```kotlin
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
    SELECT_EMPTY(numeratorForNis = 0),
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
    SELECT_FOR_READ_FILE(numerator = 11, numeratorForKindOf = 5),
    READ_FILE(numerator = 12, numeratorForKindOf = 6),
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
    READ_SOD(numeratorForNis = 4, numeratorForNisAndPace = 17),
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
```

These above are all the events while reading CIE. This enum has still two companion object val to assist who is using sdk to show a progress bar.
That's why all of these events have one of or both numerator and numeratorKindOf property.

## Example usage
Authentication:
```kotlin
try {
    cieSdk.setPin(pin)
    cieSdk.startReading(isoDepTimeout= 10000, object : NfcEvents {
        override fun error(error: NfcError) {
            //ERROR OCCURRED!!
        }

        override fun event(event: NfcEvent) {
            //EVENT OCCURRED!!
        }
    }, object : NetworkCallback {
        override fun onSuccess(url: String) {
            //ALL SUCCESS
        }

        override fun onError(networkError: NetworkError) {
            //NETWORK ERROR!!
        }
    })
} catch (e: Exception) {
    if (e is CieSdkException) {
        //EXCEPTION BUT MANAGED ERROR!!
    } else {
        //GENERAL EXCEPTION!!
    }
}
```
ATR reading:
```kotlin
 cieSdk.startReadingCieAtr(
    isoDepTimeout= 10000,
    object : NfcEvents {
        override fun error(error: NfcError) {
            //ERROR OCCURRED!!
        }

        override fun event(event: NfcEvent) {
            //EVENT OCCURRED!!
        }
    }, object : CieAtrCallback {
        override fun onSuccess(atr: ByteArray) {
            //ALL SUCCESS
        }

        override fun onError(error: NfcError) {
            //ERROR OCCURRED!!
        }
    }
)
```
NIS (Internal authentication) reading:
```kotlin
 cieSdk.startReadingNis(
    challenge = challenge.value,
    isoDepTimeout = 10000,
    object : NfcEvents {
        override fun error(error: NfcError) {
            //ERROR OCCURRED!!
        }

        override fun event(event: NfcEvent) {
            //EVENT OCCURRED!!
        }
    }, object : NisCallback {
        override fun onSuccess(nisAuth: InternalAuthenticationResponse) {
            //ALL SUCCESS
        }

        override fun onError(error: NfcError) {
            //ERROR OCCURRED!!
        }
    })
```
where InternalAuthenticationResponse:
```kotlin
data class InternalAuthenticationResponse(
    val nis: String,
    val kpubIntServ: String,
    val sod: String,
    val challengeSigned: String
) {
    fun toStringUi(): String {
        return "Internal Authentication Response:\n nis: $nis;\n sod: $sod"
    }
}
```
PACE flow:
```kotlin
 cieSdk.startDoPace(
    can = can.value,
    isoDepTimeout = 10000,
    object : NfcEvents {
        override fun error(error: NfcError) {
            //ERROR OCCURRED!!
        }

        override fun event(event: NfcEvent) {
            //EVENT OCCURRED!!
        }
    }, object : PaceCallback {
        override fun onSuccess(eMRTDResponse: MRTDResponse) {
            //ALL SUCCESS
        }

        override fun onError(error: NfcError) {
            //ERROR OCCURRED!!
        }
    })
```
where MRTDResponse:
```kotlin
data class MRTDResponse(val dg1: ByteArray, val dg11: ByteArray, val sod: ByteArray) {
    private fun hexDg(): Triple<String, String, String> {
        val dg1 = Utils.bytesToString(this.dg1)
        val dg11 = Utils.bytesToString(this.dg11)
        val sod = Utils.bytesToString(this.sod)
        return Triple(dg1, dg11, sod)
    }

    override fun toString(): String {
        val (dg1, dg11, sod) = hexDg()
        return "dg1:\n${dg1}\ndg11:\n${dg11}\nsod:\n${sod}"
    }

    fun toTerminalString(): String {
        val (dg1, dg11, sod) = hexDg()
        return "dg1:\n\t${dg1}\ndg11:\n\t${dg11}\nsod:\n\t${sod}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MRTDResponse

        if (!dg1.contentEquals(other.dg1)) return false
        if (!dg11.contentEquals(other.dg11)) return false
        if (!sod.contentEquals(other.sod)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dg1.contentHashCode()
        result = 31 * result + dg11.contentHashCode()
        result = 31 * result + sod.contentHashCode()
        return result
    }
}
```
Nis and PACE:
```kotlin
cieSdk.startNisAndPace(
    challenge = challenge.value,
    can = can.value,
    isoDepTimeout = 10000,
    object : NfcEvents {
        override fun error(error: NfcError) {
            //ERROR OCCURRED!!
        }

        override fun event(event: NfcEvent) {
            //EVENT OCCURRED!!
        }
    }, object : NisAndPaceCallback {
        override fun onSuccess(intAuthMRTDResponse: IntAuthMRTDResponse) {
            //ALL SUCCESS
        }

        override fun onError(error: NfcError) {
            //ERROR OCCURRED!!
        }
    })
```
where IntAuthMRTDResponse:
```kotlin
data class IntAuthMRTDResponse(
    val internalAuthentication: InternalAuthenticationResponse,
    val mrtd: MRTDResponse
) {
    override fun toString(): String {
        return "INT_AUTH:\n$internalAuthentication\neMRTD:\n$mrtd"
    }

    fun toTerminalString(): String {
        return "INT_AUTH:\n\t$internalAuthentication\neMRTD:\n\t$mrtd"
    }
}
```
## Permissions

To use this package you need to declare the following permission into your `AndroidManifest.xml`.
More info in the [official Android documentation](https://developer.android.com/develop/connectivity/nfc/nfc):

```xml
    <!-- Required to access NFC hardware -->
    <uses-permission android:name="android.permission.NFC" />
```