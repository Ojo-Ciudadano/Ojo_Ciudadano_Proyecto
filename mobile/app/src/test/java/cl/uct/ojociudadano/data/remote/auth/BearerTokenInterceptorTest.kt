package cl.uct.ojociudadano.data.remote.auth

import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.data.session.TokenProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BearerTokenInterceptorTest {

    @Test
    fun `adds bearer token to request when a token is available`() {
        executeRequest(token = "jwt-value") { request ->
            assertEquals("Bearer jwt-value", request.getHeader("Authorization"))
        }
    }

    @Test
    fun `does not add authorization header when no token is available`() {
        executeRequest(token = null) { request ->
            assertNull(request.getHeader("Authorization"))
        }
    }

    @Test
    fun `clears session and performs single controlled retry on 401`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.start()

        try {
            val storage = FakeSessionStorage("stale-local-jwt")
            var refreshCalls = 0
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    BearerTokenInterceptor(
                        tokenProvider = storage,
                        onUnauthorizedRefreshToken = {
                            refreshCalls += 1
                            storage.save(sampleSession("fresh-uct-jwt"))
                            "fresh-uct-jwt"
                        }
                    )
                )
                .build()

            val request = Request.Builder()
                .url(server.url("/api/v1/categorias"))
                .build()

            client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
            }

            val firstAttempt = server.takeRequest()
            val retryAttempt = server.takeRequest()

            assertEquals("Bearer stale-local-jwt", firstAttempt.getHeader("Authorization"))
            assertEquals("Bearer fresh-uct-jwt", retryAttempt.getHeader("Authorization"))
            assertEquals(1, refreshCalls)
            assertEquals("fresh-uct-jwt", storage.currentToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `clears session and returns 401 without retry when refresh fails`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        try {
            val storage = FakeSessionStorage("stale-local-jwt")
            var refreshCalls = 0
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    BearerTokenInterceptor(
                        tokenProvider = storage,
                        onUnauthorizedRefreshToken = {
                            refreshCalls += 1
                            null
                        }
                    )
                )
                .build()

            val request = Request.Builder()
                .url(server.url("/api/v1/categorias"))
                .build()

            client.newCall(request).execute().use { response ->
                assertEquals(401, response.code)
            }

            assertEquals(1, server.requestCount)
            assertEquals(1, refreshCalls)
            assertNull(storage.currentToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `does not loop on double 401 and leaves session cleared`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        try {
            val storage = FakeSessionStorage("stale-local-jwt")
            var refreshCalls = 0
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    BearerTokenInterceptor(
                        tokenProvider = storage,
                        onUnauthorizedRefreshToken = {
                            refreshCalls += 1
                            storage.save(sampleSession("still-invalid-jwt"))
                            "still-invalid-jwt"
                        }
                    )
                )
                .build()

            val request = Request.Builder()
                .url(server.url("/api/v1/categorias"))
                .build()

            client.newCall(request).execute().use { response ->
                assertEquals(401, response.code)
            }

            assertEquals(2, server.requestCount)
            assertEquals(1, refreshCalls)
            assertNull(storage.currentToken())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `concurrent 401 responses perform only one token refresh`() {
        val server = MockWebServer()
        val bothInitialRequestsArrived = CountDownLatch(2)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.getHeader("Authorization") == "Bearer stale-local-jwt") {
                    bothInitialRequestsArrived.countDown()
                    bothInitialRequestsArrived.await(5, TimeUnit.SECONDS)
                    MockResponse().setResponseCode(401)
                } else if (request.getHeader("Authorization") == "Bearer fresh-uct-jwt") {
                    MockResponse().setResponseCode(200).setBody("[]")
                } else {
                    MockResponse().setResponseCode(401)
                }
            }
        }
        server.start()

        val executor = Executors.newFixedThreadPool(2)
        try {
            val storage = FakeSessionStorage("stale-local-jwt")
            val refreshCalls = AtomicInteger(0)
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    BearerTokenInterceptor(
                        tokenProvider = storage,
                        onUnauthorizedRefreshToken = {
                            refreshCalls.incrementAndGet()
                            storage.save(sampleSession("fresh-uct-jwt"))
                            "fresh-uct-jwt"
                        }
                    )
                )
                .build()

            val callTask = {
                val request = Request.Builder()
                    .url(server.url("/api/v1/categorias"))
                    .build()
                client.newCall(request).execute().use { it.code }
            }

            val future1 = executor.submit(callTask)
            val future2 = executor.submit(callTask)

            assertEquals(200, future1.get(5, TimeUnit.SECONDS))
            assertEquals(200, future2.get(5, TimeUnit.SECONDS))
            assertEquals(1, refreshCalls.get())
            assertEquals("fresh-uct-jwt", storage.currentToken())
            assertTrue(server.requestCount == 4)
        } finally {
            executor.shutdownNow()
            server.shutdown()
        }
    }

    private fun executeRequest(
        token: String?,
        assertions: (RecordedRequest) -> Unit
    ) {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()

        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(BearerTokenInterceptor(FakeTokenProvider(token)))
                .build()
            val request = Request.Builder()
                .url(server.url("/api/v1/categorias"))
                .build()

            client.newCall(request).execute().close()

            assertions(server.takeRequest())
        } finally {
            server.shutdown()
        }
    }

    private class FakeTokenProvider(
        private val token: String?
    ) : TokenProvider {

        override fun currentToken(): String? = token
    }

    private class FakeSessionStorage(
        initialToken: String?
    ) : SessionStorage {

        @Volatile
        private var session: AuthenticatedSession? = initialToken?.let(::sampleSession)

        override fun currentToken(): String? = session?.accessToken

        override fun currentSession(): AuthenticatedSession? = session

        override fun save(session: AuthenticatedSession) {
            this.session = session
        }

        override fun clear() {
            session = null
        }
    }

    private companion object {
        fun sampleSession(token: String) = AuthenticatedSession(
            accessToken = token,
            tokenType = "Bearer",
            userId = "user-id",
            email = "dev@ojociudadano.cl",
            name = "Dev",
            roles = listOf("CIUDADANO")
        )
    }
}
