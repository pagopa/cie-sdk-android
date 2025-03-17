package it.pagopa.cie.cie

/**Interface to add when reading cie ATR
 * @property [onSuccess]
 * @property [onError]*/
interface CieAtrCallback {
    /**@param atr [ByteArray]*/
    fun onSuccess(atr: ByteArray)
    /**@param error [NfcError]*/
    fun onError(error: NfcError)
}