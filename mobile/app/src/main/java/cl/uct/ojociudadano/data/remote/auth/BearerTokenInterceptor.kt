package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val tokenProvider: TokenProvider,
    private val onUnauthorizedRefreshToken: (() -> String?)? = null
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.currentToken()?.trim()

        val initialRequest = if (
            !token.isNullOrEmpty() && request.header(AUTHORIZATION_HEADER) == null
        ) {
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(initialRequest)

        if (
            response.code == HTTP_UNAUTHORIZED &&
            !request.url.encodedPath.endsWith("/auth/login") &&
            request.header(RETRY_HEADER) == null
        ) {
            if (tokenProvider is SessionStorage) {
                tokenProvider.clear()
            }

            val refreshedToken = onUnauthorizedRefreshToken?.invoke()?.trim()
            if (!refreshedToken.isNullOrEmpty()) {
                response.close()
                val retriedRequest = request.newBuilder()
                    .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$refreshedToken")
                    .header(RETRY_HEADER, "true")
                    .build()
                return chain.proceed(retriedRequest)
            }
        }

        return response
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val RETRY_HEADER = "X-Auth-Retry"
        const val HTTP_UNAUTHORIZED = 401
    }
}
