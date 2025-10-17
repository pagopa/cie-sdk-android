package it.pagopa.io.app.cie.nis

data class InternalAuthenticationResponse(
    val nis: String,
    val kpubIntServ: String,
    val sod: String,
    val challengeSigned: String
) {
    fun toStringUi(): String {
        return "Internal Authentication Response:\n nis: $nis;\n sod: $sod;\n kpubIntServ: $kpubIntServ;\n challengeSigned: $challengeSigned"
    }
}