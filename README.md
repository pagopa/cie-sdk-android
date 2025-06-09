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
8. stopNFCListening: It stops NFC.
9. withUrl: it applies the url found by web_view which contains OpenApp to header params.
10. withCustomIdpUrl: It sets idp Url for network call for MTLS, if this method is not called, the url applied will be which one in buildConfig parameter in sdk's gradle.

To construct the class you must call .withContext(context:Context) static method as the example below:
``` kotlin
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
    ASN_1_NOT_VALID
}
```
msg will advise the sdk user with a string message about the error. </br>
numberOfAttempts is only about CIE pin, user has 3 attempts else he/she should reset pin using PUK.</br>
NfcEvent:
```kotlin
enum class NfcEvent(
    val numerator: Int = 1,
    val numeratorForKindOf: Int = 1
) {
    ON_TAG_DISCOVERED(numerator = 0),// A TAG WAS DISCOVERED
    ON_TAG_DISCOVERED_NOT_CIE,// A TAG WAS DISCOVERED BUT WAS NOT A CIE
    CONNECTED(numerator = 1),// A TAG WAS DISCOVERED, WAS A CIE AND WE ARE CONNECTED
    SELECT_IAS_SERVICE_ID(numerator = 2),// IT SELECTS INTERNAL AUTHENTICATION SERVICE FOR SERVICE ID
    SELECT_CIE_SERVICE_ID(numerator = 3),// IT SELECTS CIE SERVICE ID
    SELECT_READ_FILE_SERVICE_ID(numerator = 4),// IT SELECTS READ FILE SERVICE ID
    READ_FILE_SERVICE_ID_RESPONSE(numerator = 5),// IT READS FILE SERVICE ID RESPONSE
    SELECT_IAS(numerator = 6, numeratorForKindOf = 2),// IT SELECTS INTERNAL AUTHENTICATION SERVICE
    SELECT_CIE(numerator = 7, numeratorForKindOf = 3),// IT SELECTS CIE application
    DH_INIT_GET_G(numerator = 8),// IT GETS G FOR INIT DIFFIE HELLMAN
    DH_INIT_GET_P(numerator = 9),// IT GETS P FOR INIT DIFFIE HELLMAN
    DH_INIT_GET_Q(numerator = 10),// IT GETS Q FOR INIT DIFFIE HELLMAN
    SELECT_FOR_READ_FILE(numerator = 11, numeratorForKindOf = 5),// IT SELECTS FOR READING A FILE
    READ_FILE(numerator = 12, numeratorForKindOf = 6),// IT READS A FILE
    INIT_EXTERNAL_AUTHENTICATION(numerator = 13),// IT INIT THE EXTERNAL AUTHENTICATION
    SET_MSE(numerator = 14),// IT SETS MSE
    D_H_KEY_EXCHANGE_GET_DATA(numerator = 15),// DIFFIE HELLMAN EXCHANGING DATA
    SIGN1_SELECT(numerator = 16),// IT SELECTS SIGN1 MESSAGE
    SIGN1_VERIFY_CERT(numerator = 17),// IT VERIFIES SIGN1 CERTIFICATE
    SET_CHALLENGE_RESPONSE(numerator = 18),// IT SETS CHALLENGE RESPONSE
    GET_CHALLENGE_RESPONSE(numerator = 19),// IT GETS CHALLENGE RESPONSE
    EXTERNAL_AUTHENTICATION(numerator = 20),// DOING EXTERNAL AUTHENTICATION
    INTERNAL_AUTHENTICATION(numerator = 21),// DOING INTERNAL AUTHENTICATION
    GIVE_RANDOM(numerator = 22),// GIVES RANDOM
    VERIFY_PIN(numerator = 23),// IT VERIFIES PIN
    READ_FILE_SM(numerator = 24),// IT READS FILE SECURE MESSAGE
    SIGN(numerator = 25),// IT SIGNS
    SIGN_WITH_CIPHER(numerator = 26),// IT SIGNS WITH CIPHER
    SELECT_ROOT(numeratorForKindOf = 4);// IT SELECTS ROOT

    companion object {
        /**It's plus one because of we have to wait for Network call which will be real 100%*/
        val totalNumeratorEvent = NfcEvent.entries.maxOf { it.numerator } + 1
        val totalKindOfNumeratorEvent = NfcEvent.entries.maxOf { it.numeratorForKindOf }
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

        override fun onError(error: NetworkError) {
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

## Permissions

To use this package you need to declare the following permission into your `AndroidManifest.xml`.
More info in the [official Android documentation](https://developer.android.com/develop/connectivity/nfc/nfc):

```xml
    <!-- Required to access NFC hardware -->
    <uses-permission android:name="android.permission.NFC" />
```