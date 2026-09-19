package cl.uct.ojociudadano.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    fun create(baseUrl: String): Retrofit {
        require(baseUrl.isNotBlank()) {
            "La URL base del API Gateway no puede estar vacía"
        }

        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "La URL base del API Gateway debe utilizar HTTP o HTTPS"
        }

        require(baseUrl.endsWith("/")) {
            "La URL base del API Gateway debe terminar en '/'"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
