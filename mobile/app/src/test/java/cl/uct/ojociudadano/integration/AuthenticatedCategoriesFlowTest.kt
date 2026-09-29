package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthenticatedCategoriesFlowTest {

    @Test
    fun `login persists JWT and categories request sends it through interceptor`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginResponse))
        server.enqueue(MockResponse().setResponseCode(200).setBody(categoriesResponse))
        server.start()

        try {
            val sessionStorage = FakeSessionStorage()
            val apiService = RetrofitClient.createApiService(
                baseUrl = server.url("/").toString(),
                tokenProvider = sessionStorage
            )
            val authRepository = RemoteAuthRepository(apiService, sessionStorage)
            val categoryRepository = RemoteCategoryRepository(apiService)

            authRepository.login("ciudadano@ojociudadano.cl", "password-not-stored")
            val categories = categoryRepository.getCategories()

            val loginRequest = server.takeRequest()
            val categoriesRequest = server.takeRequest()

            assertEquals("/api/v1/auth/login", loginRequest.path)
            assertNull(loginRequest.getHeader("Authorization"))
            assertEquals("/api/v1/categorias", categoriesRequest.path)
            assertEquals("Bearer jwt-from-login", categoriesRequest.getHeader("Authorization"))
            assertEquals("Alumbrado público", categories.single().name)
            assertEquals("jwt-from-login", sessionStorage.currentToken())
        } finally {
            server.shutdown()
        }
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
        val loginResponse = """
            {
              "access_token": "jwt-from-login",
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
    }
}
