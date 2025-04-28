package it.pagopa.io.app.cie.cie.key_store

import android.os.Build
import java.security.Provider

internal class CieProvider : Provider(CieProvider::class.java.simpleName, 1.0, "Provider per cie") {

    companion object {
        const val PROVIDER = "CIE"
    }

    init {
        put("KeyStore.$PROVIDER", CieKeyStore::class.java.name)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            put("Signature.NONEwithRSA", CieSignatureImpl.None::class.java.name)
        } else {
            put("Cipher.RSA/ECB/PKCS1Padding", Cipher::class.java.name)
        }
    }
}