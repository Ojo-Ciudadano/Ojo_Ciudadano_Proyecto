package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.data.remote.auth.BearerTokenInterceptor
import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    const val DEFAULT_BASE_URL = "https://ojociudadano.dev.censei.cl/"

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
        onUnauthorizedRefreshToken: (() -> String?)? = null
    ): Retrofit {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)

        val httpClient = OkHttpClient.Builder()
            .apply {
                tokenProvider?.let {
                    addInterceptor(
                        BearerTokenInterceptor(
                            tokenProvider = it,
                            onUnauthorizedRefreshToken = onUnauthorizedRefreshToken
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
        onUnauthorizedRefreshToken: (() -> String?)? = null
    ): ApiService {
        return create(baseUrl, tokenProvider, onUnauthorizedRefreshToken)
            .create(ApiService::class.java)
    }
}
