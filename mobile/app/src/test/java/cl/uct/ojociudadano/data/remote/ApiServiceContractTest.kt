package cl.uct.ojociudadano.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

class ApiServiceContractTest {

    @Test
    fun `login uses auth login endpoint with request body`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "login" }

        val post = requireNotNull(method.getAnnotation(POST::class.java))

        assertEquals("api/v1/auth/login", post.value)
        assertTrue(method.parameterAnnotations[0].any { it is Body })
    }

    @Test
    fun `getIncidents uses incidents endpoint and optional filter queries`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "getIncidents" }

        val get = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("api/v1/incidentes", get.value)

        val queryValues = method.parameterAnnotations
            .mapNotNull { params -> params.filterIsInstance<Query>().singleOrNull()?.value }

        assertEquals(
            listOf("usuarioReportaId", "estadoId", "categoriaId", "comuna"),
            queryValues
        )
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

    @Test
    fun `updatePriority uses incident priority endpoint with body and session tag`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "updatePriority" }

        val patch = requireNotNull(method.getAnnotation(PATCH::class.java))

        assertEquals("api/v1/incidentes/{id}/prioridad", patch.value)

        val path = method.parameterAnnotations[0]
            .filterIsInstance<Path>()
            .single()

        assertEquals("id", path.value)
        assertTrue(method.parameterAnnotations[1].any { it is Body })
        assertTrue(method.parameterAnnotations[2].any { it is Tag })
    }

    @Test
    fun `getNotifications uses notifications endpoint with session tag`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "getNotifications" }

        val get = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("api/v1/notificaciones", get.value)

        assertTrue(method.parameterAnnotations[0].any { it is Tag })
    }

    @Test
    fun `getMyProfile uses the me endpoint with session tag and no user id`() {
        val method = ApiService::class.java.declaredMethods
            .single { it.name == "getMyProfile" }

        val get = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("api/v1/usuarios/me", get.value)
        // La identidad viaja en el JWT: el endpoint no acepta un id arbitrario.
        assertTrue(
            method.parameterAnnotations.flatten().none { it is Path || it is Query }
        )
        assertTrue(method.parameterAnnotations[0].any { it is Tag })
    }
}
