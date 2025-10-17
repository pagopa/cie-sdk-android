package it.pagopa.io.app.cie.pace

internal enum class KeyAgreementAlgorithm {
    DH, ECDH
}

internal enum class PaceOID(val objIdentifier: String) {
    ID_BSI("0.4.0.127.0.7"),
    ID_PACE(ID_BSI.objIdentifier + ".2.2.4"),
    ID_PACE_DH_GM(ID_PACE.objIdentifier + ".1"),
    ID_PACE_DH_GM_3DES_CBC_CBC(ID_PACE_DH_GM.objIdentifier + ".1"), // 0.4.0.127.0.7.2.2.4.1.1
    ID_PACE_DH_GM_AES_CBC_CMAC_128(ID_PACE_DH_GM.objIdentifier + ".2"), // 0.4.0.127.0.7.2.2.4.1.2
    ID_PACE_DH_GM_AES_CBC_CMAC_192(ID_PACE_DH_GM.objIdentifier + ".3"), // 0.4.0.127.0.7.2.2.4.1.3
    ID_PACE_DH_GM_AES_CBC_CMAC_256(ID_PACE_DH_GM.objIdentifier + ".4"), // 0.4.0.127.0.7.2.2.4.1.4
    ID_PACE_ECDH_GM(ID_PACE.objIdentifier + ".2"),
    ID_PACE_ECDH_GM_3DES_CBC_CBC(ID_PACE_ECDH_GM.objIdentifier + ".1"), // 0.4.0.127.0.7.2.2.4.2.1
    ID_PACE_ECDH_GM_AES_CBC_CMAC_128(ID_PACE_ECDH_GM.objIdentifier + ".2"), // 0.4.0.127.0.7.2.2.4.2.2
    ID_PACE_ECDH_GM_AES_CBC_CMAC_192(ID_PACE_ECDH_GM.objIdentifier + ".3"), // 0.4.0.127.0.7.2.2.4.2.3
    ID_PACE_ECDH_GM_AES_CBC_CMAC_256(ID_PACE_ECDH_GM.objIdentifier + ".4"); // 0.4.0.127.0.7.2.2.4.2.4

    fun kindOfObjId(): Pair<PACECipherAlgorithms, PACEDigestAlgorithms>? {
        return when (this) {
            ID_PACE_DH_GM_3DES_CBC_CBC, ID_PACE_ECDH_GM_3DES_CBC_CBC -> PACECipherAlgorithms.DESede to PACEDigestAlgorithms.SHA1
            ID_PACE_DH_GM_AES_CBC_CMAC_128, ID_PACE_ECDH_GM_AES_CBC_CMAC_128 -> PACECipherAlgorithms.AES to PACEDigestAlgorithms.SHA1
            ID_PACE_DH_GM_AES_CBC_CMAC_192, ID_PACE_DH_GM_AES_CBC_CMAC_256, ID_PACE_ECDH_GM_AES_CBC_CMAC_192, ID_PACE_ECDH_GM_AES_CBC_CMAC_256 -> PACECipherAlgorithms.AES to PACEDigestAlgorithms.SHA256
            else -> null
        }
    }

    fun keyLength(): Int? {
        return when (this) {
            ID_PACE_DH_GM_3DES_CBC_CBC, ID_PACE_ECDH_GM_3DES_CBC_CBC -> 112
            ID_PACE_DH_GM_AES_CBC_CMAC_128, ID_PACE_ECDH_GM_AES_CBC_CMAC_128 -> 128
            ID_PACE_DH_GM_AES_CBC_CMAC_192, ID_PACE_ECDH_GM_AES_CBC_CMAC_192 -> 192
            ID_PACE_DH_GM_AES_CBC_CMAC_256, ID_PACE_ECDH_GM_AES_CBC_CMAC_256 -> 256
            else -> null
        }
    }

    fun keyAgreementAlgorithm(): KeyAgreementAlgorithm? {
        return when (this) {
            ID_PACE_DH_GM_3DES_CBC_CBC, ID_PACE_DH_GM_AES_CBC_CMAC_128, ID_PACE_DH_GM_AES_CBC_CMAC_192, ID_PACE_DH_GM_AES_CBC_CMAC_256 -> KeyAgreementAlgorithm.DH
            ID_PACE_ECDH_GM_3DES_CBC_CBC, ID_PACE_ECDH_GM_AES_CBC_CMAC_128, ID_PACE_ECDH_GM_AES_CBC_CMAC_192, ID_PACE_ECDH_GM_AES_CBC_CMAC_256 -> KeyAgreementAlgorithm.ECDH
            else -> null
        }
    }

    companion object {
        @JvmSynthetic
        operator fun invoke(objIdentifier: String): PaceOID? {
            return entries.find { it.objIdentifier == objIdentifier }
        }
    }
}