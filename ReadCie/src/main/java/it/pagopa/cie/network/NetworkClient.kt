package it.pagopa.cie.network

import it.pagopa.cie.BuildConfig
import it.pagopa.cie.CieLogger
import it.pagopa.cie.cie.key_store.CieProvider
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.Security
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal class NetworkClient(
    private val certificate: ByteArray,
    private val idpCustomUrl: String? = null
) {
    init {
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, null, null)
    }

    private fun okhttpInitializer(): OkHttpClient {
        Security.removeProvider(CieProvider.PROVIDER)
        val cieProvider = CieProvider()
        Security.insertProviderAt(cieProvider, 1)
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        val cieKeyStore: KeyStore = KeyStore.getInstance(CieProvider.PROVIDER)
        cieKeyStore.load(ByteArrayInputStream(certificate), null)
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(cieKeyStore, null)
        val keyManagers = kmf.keyManagers

        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
            throw IllegalStateException(
                "Unexpected default trust managers:" + trustManagers.contentToString()
            )
        }
        val trustManager = trustManagers[0] as X509TrustManager
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(keyManagers, null, null)
        return builder.sslSocketFactory(sslContext.socketFactory, trustManager).build()
    }

    private val okHttpClient: OkHttpClient by lazy { okhttpInitializer() }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(idpCustomUrl ?: BuildConfig.BASE_URL_IDP)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level =
            if (CieLogger.enabled) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        interceptor
    }

    val idpService: IdpService by lazy { retrofit.create(IdpService::class.java) }
}