package it.pagopa.io.app.cie.nis

data class NisAuthenticated(
    val nis: String,
    val kpubIntServ: String,
    val haskKpubIntServ: String,
    val sod: String,
    val challengeSigned: String
) {
    override fun toString(): String {
        return "NisAuthenticated:\n nis: $nis;\n sod: $sod"
    }
}