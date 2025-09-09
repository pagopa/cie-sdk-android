package it.pagopa.io.app.cie.nis

import it.pagopa.io.app.cie.cie.NfcError

/**Interface to add when reading cie Nis
 * @property [onSuccess]
 * @property [onError]*/
interface NisCallback {
    /**@param nisAuth [InternalAuthenticationResponse]*/
    fun onSuccess(nisAuth: InternalAuthenticationResponse)

    /**@param error [NfcError]*/
    fun onError(error: NfcError)
}