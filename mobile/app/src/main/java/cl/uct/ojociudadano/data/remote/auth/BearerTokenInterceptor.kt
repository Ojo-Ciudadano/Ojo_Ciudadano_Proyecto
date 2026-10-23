package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.TokenProvider
import cl.uct.ojociudadano.data.session.TokenSnapshot
import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val tokenProvider: TokenProvider,
    private val onUnauthorized: ((rejectedToken: TokenSnapshot) -> Unit)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sessionTokenToAttach = tokenProvider.currentTokenSnapshot()
            ?.let { it.copy(value = it.value.trim()) }
            ?.takeIf { it.value.isNotEmpty() }
            ?.takeIf {
                request.header(AUTHORIZATION_HEADER) == null
            }

        val authenticatedRequest = if (sessionTokenToAttach != null) {
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX${sessionTokenToAttach.value}")
                .build()
        } else {
            request
        }

        val response = chain.proceed(authenticatedRequest)

        if (response.code == HTTP_UNAUTHORIZED && sessionTokenToAttach != null) {
            onUnauthorized?.invoke(sessionTokenToAttach)
        }

        return response
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val HTTP_UNAUTHORIZED = 401
    }
}
