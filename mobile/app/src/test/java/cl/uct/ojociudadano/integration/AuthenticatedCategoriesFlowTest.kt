package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.SessionStorage
import java.util.Base64
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticatedCategoriesFlowTest {

    @Test
    fun `login establishes real session and protected request sends returned JWT`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginResponse))
        server.enqueue(MockResponse().setResponseCode(200).setBody(categoriesResponse))
        server.start()

        try {
            val storage = FakeSessionStorage()
            val sessionManager = DefaultSessionManager(storage)
            val publicApi = RetrofitClient.createApiService(server.url("/").toString())
            val protectedApi = RetrofitClient.createApiService(
                baseUrl = server.url("/").toString(),
                tokenProvider = sessionManager,
                onUnauthorized = sessionManager::invalidateIfCurrent
            )
            val authRepository = RemoteAuthRepository(publicApi)
            val categoryRepository = RemoteCategoryRepository(protectedApi)

            val session = authRepository.login(
                "ciudadano@ojociudadano.cl",
                "password-not-stored"
            )
            sessionManager.establish(session)
            val categories = categoryRepository.getCategories()

            val loginRequest = server.takeRequest()
            val categoriesRequest = server.takeRequest()
            assertEquals("/api/v1/auth/login", loginRequest.path)
            assertNull(loginRequest.getHeader("Authorization"))
            assertEquals("/api/v1/categorias", categoriesRequest.path)
            assertEquals(
                "Bearer $jwtFromLogin",
                categoriesRequest.getHeader("Authorization")
            )
            assertEquals("Alumbrado público", categories.single().name)
            assertEquals(jwtFromLogin, sessionManager.currentToken())
            assertEquals(session, storage.currentSession())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `public login client omits authorization even while session exists`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginResponse))
        server.start()

        try {
            val sessionManager = DefaultSessionManager(FakeSessionStorage())
            sessionManager.establish(sampleSession("existing-session"))
            val authRepository = RemoteAuthRepository(
                RetrofitClient.createApiService(server.url("/").toString())
            )

            authRepository.login("ciudadano@ojociudadano.cl", "password-not-stored")

            assertNull(server.takeRequest().getHeader("Authorization"))
            assertTrue(sessionManager.state.value is SessionState.Authenticated)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `protected request without session returns controlled 401 without empty header`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody(unauthorizedResponse))
        server.start()

        try {
            val sessionManager = DefaultSessionManager(FakeSessionStorage())
            sessionManager.restore()
            val repository = protectedRepository(server, sessionManager)

            val failure = runCatching { repository.getCategories() }.exceptionOrNull()

            assertTrue(failure is ApiException.Http)
            assertEquals(401, (failure as ApiException.Http).statusCode)
            assertNull(server.takeRequest().getHeader("Authorization"))
            assertEquals(SessionState.Unauthenticated, sessionManager.state.value)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `401 from protected endpoint invalidates matching session`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody(unauthorizedResponse))
        server.start()

        try {
            val storage = FakeSessionStorage()
            val sessionManager = DefaultSessionManager(storage)
            val session = sampleSession("rejected-session")
            sessionManager.establish(session)

            val failure = runCatching {
                protectedRepository(server, sessionManager).getCategories()
            }.exceptionOrNull()

            assertTrue(failure is ApiException.Http)
            assertEquals(401, (failure as ApiException.Http).statusCode)
            assertEquals(
                "Bearer ${session.accessToken}",
                server.takeRequest().getHeader("Authorization")
            )
            assertEquals(SessionState.Unauthenticated, sessionManager.state.value)
            assertNull(storage.currentSession())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `403 from protected endpoint preserves authenticated session`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403).setBody(forbiddenResponse))
        server.start()

        try {
            val storage = FakeSessionStorage()
            val sessionManager = DefaultSessionManager(storage)
            val session = sampleSession("forbidden-session")
            sessionManager.establish(session)

            val failure = runCatching {
                protectedRepository(server, sessionManager).getCategories()
            }.exceptionOrNull()

            assertTrue(failure is ApiException.Http)
            assertEquals(403, (failure as ApiException.Http).statusCode)
            assertEquals(SessionState.Authenticated(session), sessionManager.state.value)
            assertEquals(session, storage.currentSession())
        } finally {
            server.shutdown()
        }
    }

    private fun protectedRepository(
        server: MockWebServer,
        sessionManager: DefaultSessionManager
    ): RemoteCategoryRepository {
        val api = RetrofitClient.createApiService(
            baseUrl = server.url("/").toString(),
            tokenProvider = sessionManager,
            onUnauthorized = sessionManager::invalidateIfCurrent
        )
        return RemoteCategoryRepository(api)
    }

    private class FakeSessionStorage : SessionStorage {

        private var session: AuthenticatedSession? = null

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
        val jwtFromLogin = jwt("login-session")

        val loginResponse = """
            {
              "access_token": "$jwtFromLogin",
              "token_type": "Bearer",
              "usuario": {
                "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "nombre": "Ciudadano",
                "email": "ciudadano@ojociudadano.cl",
                "roles": ["CIUDADANO"]
              }
            }
        """.trimIndent()

        val categoriesResponse = """
            [
              {
                "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "nombre": "Alumbrado público",
                "descripcion": "Fallas de alumbrado",
                "prioridadBase": 2,
                "esEmergenciaCritica": false,
                "canalOficialContacto": null
              }
            ]
        """.trimIndent()

        val unauthorizedResponse = """
            {"statusCode":401,"message":"Token inválido o expirado"}
        """.trimIndent()

        val forbiddenResponse = """
            {"statusCode":403,"message":"No tienes permisos suficientes"}
        """.trimIndent()

        fun sampleSession(subject: String): AuthenticatedSession {
            return AuthenticatedSession(
                accessToken = jwt(subject),
                tokenType = "Bearer",
                userId = subject,
                email = "$subject@ojociudadano.cl",
                name = subject,
                roles = listOf("CIUDADANO")
            )
        }

        fun jwt(subject: String): String {
            val payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("""{"sub":"$subject","exp":4102444800}""".toByteArray())
            return "eyJhbGciOiJIUzI1NiJ9.$payload.signature-$subject"
        }
    }
}
