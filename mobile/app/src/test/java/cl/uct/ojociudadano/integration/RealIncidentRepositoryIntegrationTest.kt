package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.RemoteAuthRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.TestSessionStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RealIncidentRepositoryIntegrationTest {

    @Test
    fun `authenticated user incidents are filtered and contained in general query`() = runBlocking {
        assumeTrue(System.getenv("RUN_API_INTEGRATION_TESTS") == "true")
        val email = System.getenv("AUTH_EMAIL").orEmpty()
        val password = System.getenv("AUTH_PASSWORD").orEmpty()
        assumeTrue(email.isNotBlank() && password.isNotBlank())
        val baseUrl = System.getenv("API_BASE_URL") ?: RetrofitClient.DEFAULT_BASE_URL
        val session = RemoteAuthRepository(RetrofitClient.createApiService(baseUrl))
            .login(email, password)
        val manager = DefaultSessionManager(TestSessionStorage())
        manager.establish(session)
        val repository = RemoteIncidentRepository(
            RetrofitClient.createApiService(baseUrl, manager, manager::invalidateIfCurrent),
            manager
        )

        val allIncidents = repository.getIncidents()
        val userIncidents = repository.getCurrentUserIncidents()
        val allIds = allIncidents.map { it.id }.toSet()

        assertTrue(userIncidents.all { it.reporterUserId == session.userId })
        assertTrue(userIncidents.all { it.id in allIds })
    }
}
