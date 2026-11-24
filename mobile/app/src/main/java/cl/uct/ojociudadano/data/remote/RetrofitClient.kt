package cl.uct.ojociudadano.data.remote

import cl.uct.ojociudadano.BuildConfig
import cl.uct.ojociudadano.data.remote.auth.BearerTokenInterceptor
import cl.uct.ojociudadano.data.session.TokenProvider
import cl.uct.ojociudadano.data.session.TokenSnapshot
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory
import cl.uct.ojociudadano.data.remote.auth.RegistrationRequestInterceptor

object RetrofitClient {

    // La app, la persistencia y las pruebas remotas comparten el mismo entorno configurado.
    const val DEFAULT_BASE_URL = BuildConfig.API_BASE_URL

    /**
     * Tiempos máximos de una petición de subida.
     *
     * Los valores por defecto de OkHttp (10 s en lectura y escritura) son
     * insuficientes para subir fotografías por la red del campus o de un
     * operador móvil: una imagen de varios MB tarda más en escribirse y la
     * petición se cancelaba a la mitad. La ampliación se aplica solo a la
     * subida de evidencias, no al resto de la API, para que una respuesta
     * perdida siga fallando en el tiempo habitual.
     */
    data class HttpTimeouts(
        val connectSeconds: Int = 15,
        val readSeconds: Int = 60,
        val writeSeconds: Int = 120
    )

    val defaultTimeouts = HttpTimeouts()

    /**
     * Amplía los tiempos de espera únicamente para `POST .../evidencias`.
     *
     * OkHttp no permite cambiar los tiempos de una petición concreta desde el
     * constructor del cliente, pero `Interceptor.Chain` sí expone variantes con
     * el tiempo sustituido.
     */
    class EvidenceUploadTimeoutInterceptor(
        private val timeouts: HttpTimeouts = defaultTimeouts
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (!isEvidenceUpload(request)) {
                return chain.proceed(request)
            }

            return chain
                .withConnectTimeout(timeouts.connectSeconds, TimeUnit.SECONDS)
                .withWriteTimeout(timeouts.writeSeconds, TimeUnit.SECONDS)
                .withReadTimeout(timeouts.readSeconds, TimeUnit.SECONDS)
                .proceed(request)
        }
    }

    /** Indica si la petición corresponde a la subida de una evidencia. */
    fun isEvidenceUpload(request: Request): Boolean =
        request.method.equals("POST", ignoreCase = true) &&
            request.url.encodedPath.trimEnd('/').endsWith("/evidencias")

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
            .addInterceptor(EvidenceUploadTimeoutInterceptor())
            .addInterceptor(RegistrationRequestInterceptor())
            .addInterceptor(StatusPatchInterceptor())
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
