package it.pagopa.io.app.cie

import it.pagopa.io.app.cie.cie.NfcError

/**Interface to add when doing PACE and NIS flow
 * @property [onSuccess]
 * @property [onError]*/
interface NisAndPaceCallback {
    /**@param intAuthMRTDResponse [IntAuthMRTDResponse]*/
    fun onSuccess(intAuthMRTDResponse: IntAuthMRTDResponse)

    /**@param error [NfcError]*/
    fun onError(error: NfcError)
}