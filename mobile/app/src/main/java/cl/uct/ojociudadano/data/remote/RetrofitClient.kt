package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.data.remote.auth.BearerTokenInterceptor
import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    fun create(
        baseUrl: String,
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

        val httpClient = OkHttpClient.Builder()
            .apply {
                tokenProvider?.let { addInterceptor(BearerTokenInterceptor(it)) }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createApiService(
        baseUrl: String,
        tokenProvider: TokenProvider? = null
    ): ApiService {
        return create(baseUrl, tokenProvider).create(ApiService::class.java)
    }
}
