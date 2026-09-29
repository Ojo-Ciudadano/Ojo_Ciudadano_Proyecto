package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.currentToken()?.trim()

        val authenticatedRequest = if (
            !token.isNullOrEmpty() && request.header(AUTHORIZATION_HEADER) == null
        ) {
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
