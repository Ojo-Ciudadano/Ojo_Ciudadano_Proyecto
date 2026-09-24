package cl.uct.ojociudadano.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class ApiServiceContractTest {

    @Test
    fun `getIncidents uses incidents endpoint and optional user query`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "getIncidents" }

        val get = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("api/v1/incidentes", get.value)

        val query = method.parameterAnnotations[0]
            .filterIsInstance<Query>()
            .single()

        assertEquals("usuarioReportaId", query.value)
    }

    @Test
    fun `getIncidentById uses incident id path`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "getIncidentById" }

        val get = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("api/v1/incidentes/{id}", get.value)

        val path = method.parameterAnnotations[0]
            .filterIsInstance<Path>()
            .single()

        assertEquals("id", path.value)
    }

    @Test
    fun `getCategories uses categories endpoint`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "getCategories" }

        val get = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("api/v1/categorias", get.value)
    }

    @Test
    fun `createIncident uses incidents endpoint with request body`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "createIncident" }

        val post = requireNotNull(method.getAnnotation(POST::class.java))

        assertEquals("api/v1/incidentes", post.value)

        val hasBody = method.parameterAnnotations[0]
            .any { it is Body }

        assertTrue(hasBody)
    }
}
