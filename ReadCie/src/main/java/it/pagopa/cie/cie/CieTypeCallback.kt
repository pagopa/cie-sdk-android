package it.pagopa.cie.cie

/**Interface to add when reading [CieType]
 * @property [onSuccess]
 * @property [onError]*/
interface CieTypeCallback {
    /**@param type [CieType]*/
    fun onSuccess(type: CieType)
    /**@param error [NfcError]*/
    fun onError(error: NfcError)
}