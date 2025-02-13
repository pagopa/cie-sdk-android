package it.pagopa.cie.network

internal class DeepLinkInfo {
    lateinit var name: String
    lateinit var authnRequest: String
    lateinit var value: String
    lateinit var opText: String
    lateinit var nextUrl: String
    lateinit var host: String
    lateinit var logo: String

    companion object {
        const val KEY_VALUE = "value"
        const val KEY_AUTHN_REQUEST_STRING = "authnRequestString"
        const val KEY_NAME = "name"
        const val KEY_NEXT_UTL = "nextUrl"
        const val KEY_OP_TEXT = "OpText"
        const val KEY_LOGO = "imgUrl"
        internal operator fun invoke(
            name: String?,
            authnRequest: String?,
            value: String?,
            opText: String?,
            nextUrl: String?,
            host: String?,
            logo: String?
        ): DeepLinkInfo {
            return DeepLinkInfo().apply {
                this.name = name.orEmpty()
                this.authnRequest = authnRequest.orEmpty()
                this.value = value.orEmpty()
                this.opText = opText.orEmpty()
                this.nextUrl = nextUrl.orEmpty()
                this.host = host.orEmpty()
                this.logo = logo.orEmpty()
            }
        }
    }
}