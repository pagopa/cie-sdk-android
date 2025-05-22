package it.pagopa.io.app.cie.cie.validity_check

import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.io.ByteArrayInputStream
import java.util.Date

/**Class useful to handle Certificate Control*/
class CieCertificateControl(private val certificate: ByteArray) {
    /**Call this method to check if a certificate is valid as now*/
    fun isCertificateValid(): Boolean {
        val certFactory = CertificateFactory.getInstance("X.509")
        val certInputStream = ByteArrayInputStream(certificate)
        val cert = certFactory.generateCertificate(certInputStream) as X509Certificate
        val now = Date()
        return now >= cert.notBefore && now <= cert.notAfter
    }
}