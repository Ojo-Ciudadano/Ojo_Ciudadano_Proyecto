package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.remote.auth.BearerTokenInterceptor
import cl.uct.ojociudadano.data.session.TokenProvider
import cl.uct.ojociudadano.data.session.TokenSnapshot
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import cl.uct.ojociudadano.data.remote.auth.RegistrationRequestInterceptor

object RetrofitClient {

    // La app, la persistencia y las pruebas remotas comparten el mismo entorno configurado.
    const val DEFAULT_BASE_URL = BuildConfig.API_BASE_URL

    fun normalizeBaseUrl(baseUrl: String): String {
        require(baseUrl.isNotBlank()) {
            "La URL base del API Gateway no puede estar vacía"
        }

        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "La URL base del API Gateway debe utilizar HTTP o HTTPS"
        }

        require(baseUrl.endsWith("/")) {
            "La URL base del API Gateway debe terminar en '/'"
        }

        return baseUrl.removeSuffix("api/v1/")
    }

    fun create(
        baseUrl: String = DEFAULT_BASE_URL,
        tokenProvider: TokenProvider? = null,
        onUnauthorized: ((rejectedToken: TokenSnapshot) -> Unit)? = null
    ): Retrofit {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)

        val httpClient = OkHttpClient.Builder()
            .configureEnvironmentTls(normalizedBaseUrl)
            .addInterceptor(RegistrationRequestInterceptor())
            .apply {
                tokenProvider?.let {
                    addInterceptor(
                        BearerTokenInterceptor(
                            tokenProvider = it,
                            onUnauthorized = onUnauthorized
                        )
                    )
                }
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
        tokenProvider: TokenProvider? = null,
        onUnauthorized: ((rejectedToken: TokenSnapshot) -> Unit)? = null
    ): ApiService {
        return create(baseUrl, tokenProvider, onUnauthorized)
            .create(ApiService::class.java)
    }
}
