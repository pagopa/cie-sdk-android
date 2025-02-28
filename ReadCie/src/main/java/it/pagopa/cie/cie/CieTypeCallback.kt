package it.pagopa.cie.cie

interface CieTypeCallback {
    fun onSuccess(type: CieType)
    fun onError(error: NfcError)
}