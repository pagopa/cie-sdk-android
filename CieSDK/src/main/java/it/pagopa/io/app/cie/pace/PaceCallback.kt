package it.pagopa.io.app.cie.pace

import it.pagopa.io.app.cie.cie.NfcError

/**Interface to add when do PACE flow
 * @property [onSuccess]
 * @property [onError]*/
interface PaceCallback {
    /**@param eMRTDResponse [MRTDResponse]*/
    fun onSuccess(eMRTDResponse: MRTDResponse)

    /**@param error [NfcError]*/
    fun onError(error: NfcError)
}