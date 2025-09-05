package it.pagopa.io.app.cie.pace

enum class PACEDomainParam(val id: Int) {
    // Standardized domain parameters. Based on Table 6.
    PARAM_ID_GFP_1024_160(0),
    PARAM_ID_GFP_2048_224(1),
    PARAM_ID_GFP_2048_256(2),
    PARAM_ID_ECP_NIST_P192_R1(8),
    PARAM_ID_ECP_BRAINPOOL_P192_R1(9),
    PARAM_ID_ECP_NIST_P224_R1(10),
    PARAM_ID_ECP_BRAINPOOL_P224_R1(11),
    PARAM_ID_ECP_NIST_P256_R1(12),
    PARAM_ID_ECP_BRAINPOOL_P256_R1(13),
    PARAM_ID_ECP_BRAINPOOL_P320_R1(14),
    PARAM_ID_ECP_NIST_P384_R1(15),
    PARAM_ID_ECP_BRAINPOOL_P384_R1(16),
    PARAM_ID_ECP_BRAINPOOL_P512_R1(17),
    PARAM_ID_ECP_NIST_P521_R1(18);

    /**
     * Restituisce il nome della curva compatibile con Android/BouncyCastle
     */
    fun toCurveName(): String {
        return when (this) {
            PARAM_ID_GFP_1024_160 -> "rfc5114_1024_160"
            PARAM_ID_GFP_2048_224 -> "rfc5114_2048_224"
            PARAM_ID_GFP_2048_256 -> "rfc5114_2048_256"

            // NIST prime curves
            PARAM_ID_ECP_NIST_P192_R1 -> "secp192r1"
            PARAM_ID_ECP_NIST_P224_R1 -> "secp224r1"
            PARAM_ID_ECP_NIST_P256_R1 -> "secp256r1"
            PARAM_ID_ECP_NIST_P384_R1 -> "secp384r1"
            PARAM_ID_ECP_NIST_P521_R1 -> "secp521r1"

            // Brainpool curves
            PARAM_ID_ECP_BRAINPOOL_P192_R1 -> "brainpoolP192r1"
            PARAM_ID_ECP_BRAINPOOL_P224_R1 -> "brainpoolP224r1"
            PARAM_ID_ECP_BRAINPOOL_P256_R1 -> "brainpoolP256r1"
            PARAM_ID_ECP_BRAINPOOL_P320_R1 -> "brainpoolP320r1"
            PARAM_ID_ECP_BRAINPOOL_P384_R1 -> "brainpoolP384r1"
            PARAM_ID_ECP_BRAINPOOL_P512_R1 -> "brainpoolP512r1"
        }
    }

    companion object {
        fun fromId(id: Int): PACEDomainParam? = entries.find { it.id == id }
    }
}