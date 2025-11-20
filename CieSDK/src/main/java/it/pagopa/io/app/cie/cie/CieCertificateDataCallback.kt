package it.pagopa.io.app.cie.cie

interface CieCertificateDataCallback {
    /**@param data [CertificateData]-> JSONObject representing data*/
    fun onSuccess(data: CertificateData)
    /**@param error [NfcError]*/
    fun onError(error: NfcError)
}