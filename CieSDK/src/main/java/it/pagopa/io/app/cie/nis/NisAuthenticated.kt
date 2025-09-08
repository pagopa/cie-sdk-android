package it.pagopa.io.app.cie.nis

data class NisAuthenticated(
    val nis: String,
    val kpubIntServ: String,
    val sod: String,
    val challengeSigned: String
) {
    fun toStringUi(): String {
        return "NisAuthenticated:\n nis: $nis;\n sod: $sod"
    }
}