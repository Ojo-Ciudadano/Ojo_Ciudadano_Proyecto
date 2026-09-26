package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.RemoteCategoryRepository
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

        val baseUrl = System.getenv("API_BASE_URL")
            ?: "http://localhost:3000/"

        val apiService = RetrofitClient.createApiService(baseUrl)
        val repository = RemoteCategoryRepository(apiService)

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
}
