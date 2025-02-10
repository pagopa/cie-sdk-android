package it.pagopa.cie_sdk

import android.app.Application
import com.google.android.gms.security.ProviderInstaller
import javax.net.ssl.SSLContext

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        ProviderInstaller.installIfNeeded(applicationContext)
        val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, null, null)
        sslContext.createSSLEngine()
    }
}