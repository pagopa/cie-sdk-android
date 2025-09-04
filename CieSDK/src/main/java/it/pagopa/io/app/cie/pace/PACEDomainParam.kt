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

    fun toCurveOid(): String {
        return when (this) {
            // DH parameters - RFC 5114 OIDs
            PARAM_ID_GFP_1024_160 -> "1.2.840.10046.3.1.2" // dhpublicnumber
            PARAM_ID_GFP_2048_224 -> "1.2.840.10046.3.1.2"
            PARAM_ID_GFP_2048_256 -> "1.2.840.10046.3.1.2"

            // NIST EC curves
            PARAM_ID_ECP_NIST_P192_R1 -> "1.2.840.10045.3.1.1"
            PARAM_ID_ECP_NIST_P224_R1 -> "1.3.132.0.33"
            PARAM_ID_ECP_NIST_P256_R1 -> "1.2.840.10045.3.1.7"
            PARAM_ID_ECP_NIST_P384_R1 -> "1.3.132.0.34"
            PARAM_ID_ECP_NIST_P521_R1 -> "1.3.132.0.35"

            // Brainpool EC curves
            PARAM_ID_ECP_BRAINPOOL_P192_R1 -> "1.3.36.3.3.2.8.1.1.3"
            PARAM_ID_ECP_BRAINPOOL_P224_R1 -> "1.3.36.3.3.2.8.1.1.5"
            PARAM_ID_ECP_BRAINPOOL_P256_R1 -> "1.3.36.3.3.2.8.1.1.7"
            PARAM_ID_ECP_BRAINPOOL_P320_R1 -> "1.3.36.3.3.2.8.1.1.9"
            PARAM_ID_ECP_BRAINPOOL_P384_R1 -> "1.3.36.3.3.2.8.1.1.11"
            PARAM_ID_ECP_BRAINPOOL_P512_R1 -> "1.3.36.3.3.2.8.1.1.13"
        }
    }

    companion object {
        fun fromId(id: Int): PACEDomainParam? = entries.find { it.id == id }
    }
}