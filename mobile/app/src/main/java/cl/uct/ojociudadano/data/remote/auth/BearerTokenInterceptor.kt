package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val tokenProvider: TokenProvider,
    private val onUnauthorizedRefreshToken: (() -> String?)? = null
) : Interceptor {

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestToken = tokenProvider.currentToken()?.trim()

        val initialRequest = if (
            !requestToken.isNullOrEmpty() && request.header(AUTHORIZATION_HEADER) == null
        ) {
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$requestToken")
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
            val refreshedToken = synchronized(refreshLock) {
                val currentStoredToken = tokenProvider.currentToken()?.trim()
                if (!currentStoredToken.isNullOrEmpty() && currentStoredToken != requestToken) {
                    currentStoredToken
                } else {
                    if (tokenProvider is SessionStorage) {
                        tokenProvider.clear()
                    }
                    onUnauthorizedRefreshToken?.invoke()?.trim()?.takeIf { it.isNotEmpty() }
                }
            }

            if (!refreshedToken.isNullOrEmpty()) {
                response.close()
                val retriedRequest = request.newBuilder()
                    .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$refreshedToken")
                    .header(RETRY_HEADER, "true")
                    .build()
                val retryResponse = chain.proceed(retriedRequest)
                if (retryResponse.code == HTTP_UNAUTHORIZED && tokenProvider is SessionStorage) {
                    synchronized(refreshLock) {
                        if (tokenProvider.currentToken()?.trim() == refreshedToken) {
                            tokenProvider.clear()
                        }
                    }
                }
                return retryResponse
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
