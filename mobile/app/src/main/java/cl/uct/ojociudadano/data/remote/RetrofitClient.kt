package cl.uct.ojociudadano.data.remote

import android.annotation.SuppressLint
import cl.uct.ojociudadano.data.remote.auth.BearerTokenInterceptor
import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {

    const val DEFAULT_BASE_URL = "https://ojociudadano.dev.censei.cl/"

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager", "BadHostnameVerifier")
    private fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { hostname, _ ->
                    hostname == "ojociudadano.dev.censei.cl" ||
                            hostname == "localhost" ||
                            hostname == "10.0.2.2" ||
                            hostname == "127.0.0.1"
                }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun create(
        baseUrl: String = DEFAULT_BASE_URL,
        tokenProvider: TokenProvider? = null
    ): Retrofit {
        require(baseUrl.isNotBlank()) {
            "La URL base del API Gateway no puede estar vacía"
        }

        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "La URL base del API Gateway debe utilizar HTTP o HTTPS"
        }

        require(baseUrl.endsWith("/")) {
            "La URL base del API Gateway debe terminar en '/'"
        }

        // Evita duplicar /api/v1/api/v1/ ya que ApiService incluye "api/v1/" en sus rutas
        val normalizedBaseUrl = baseUrl.removeSuffix("api/v1/")

        val httpClient = getUnsafeOkHttpClientBuilder()
            .apply {
                tokenProvider?.let { addInterceptor(BearerTokenInterceptor(it)) }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createApiService(
        baseUrl: String = DEFAULT_BASE_URL,
        tokenProvider: TokenProvider? = null
    ): ApiService {
        return create(baseUrl, tokenProvider).create(ApiService::class.java)
    }
}
