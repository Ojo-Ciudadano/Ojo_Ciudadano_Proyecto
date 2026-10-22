package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RealCategoryRepositoryIntegrationTest {

    @Test
    fun `getCategories obtains real categories through API Gateway`() = runBlocking {
        assumeTrue(
            "La prueba requiere RUN_API_INTEGRATION_TESTS=true",
            System.getenv("RUN_API_INTEGRATION_TESTS") == "true"
        )

        val email = System.getenv("AUTH_EMAIL").orEmpty()
        val password = System.getenv("AUTH_PASSWORD").orEmpty()

        assumeTrue(
            "La prueba requiere AUTH_EMAIL y AUTH_PASSWORD",
            email.isNotBlank() && password.isNotBlank()
        )

        val baseUrl = System.getenv("API_BASE_URL")
            ?: RetrofitClient.DEFAULT_BASE_URL

        val sessionStorage = InMemorySessionStorage()
        val apiService = RetrofitClient.createApiService(baseUrl, sessionStorage)
        val authRepository = RemoteAuthRepository(apiService)
        val repository = RemoteCategoryRepository(apiService)

        sessionStorage.save(authRepository.login(email, password))
        val categories = repository.getCategories()

        assertEquals(8, categories.size)

        assertTrue(
            categories.any {
                it.name == "Alumbrado público" &&
                    it.basePriority == 2 &&
                    !it.isCriticalEmergency
            }
        )

        assertTrue(
            categories.any {
                it.name == "Incendio" &&
                    it.basePriority == 5 &&
                    it.isCriticalEmergency &&
                    it.officialContactChannel == "Bomberos 132"
            }
        )
    }

    private class InMemorySessionStorage : SessionStorage {

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
}
