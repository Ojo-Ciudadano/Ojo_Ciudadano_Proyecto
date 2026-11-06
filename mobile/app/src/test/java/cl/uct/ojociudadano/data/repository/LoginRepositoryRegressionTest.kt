package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginRepositoryRegressionTest {
    private val server = MockWebServer()
    private lateinit var repository: RemoteAuthRepository

    @Before
    fun setUp() {
        server.start()
        repository = RemoteAuthRepository(
            RetrofitClient.createApiService(server.url("/api/v1/").toString())
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `login sends the contract once and maps citizen and administrator responses`() = runTest {
        for (role in listOf("CIUDADANO", "ADMIN")) {
            server.enqueue(MockResponse().setBody(response(role)))
            val session = repository.login("persona@example.invalid", " password ")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/auth/login", request.path)
            assertNull(request.getHeader("Authorization"))
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(setOf("email", "password"), json.keySet())
            assertEquals("persona@example.invalid", json["email"].asString)
            assertEquals(" password ", json["password"].asString)
            assertEquals("returned-token", session.accessToken)
            assertEquals("Bearer", session.tokenType)
            assertEquals("user-id", session.userId)
            assertEquals("Persona", session.name)
            assertEquals("persona@example.invalid", session.email)
            assertEquals(listOf(role), session.roles)
        }
    }

    @Test
    fun `HTML and incomplete success bodies are response errors rather than connectivity failures`() = runTest {
        val valid = response("CIUDADANO")
        val invalidBodies = listOf(
            "<html><body>Ojo Ciudadano</body></html>",
            "{}",
            "[]",
            valid.replace("\"access_token\":\"returned-token\",", ""),
            valid.replace("\"token_type\":\"Bearer\",", ""),
            valid.replace("\"id\":\"user-id\",", ""),
            valid.replace("\"nombre\":\"Persona\",", ""),
            valid.replace("\"email\":\"persona@example.invalid\",", ""),
            valid.replace("\"returned-token\"", "null"),
            valid.replace("[\"CIUDADANO\"]", "[null]")
        )
        for (body in invalidBodies) {
            server.enqueue(MockResponse().setBody(body))
            val error = runCatching { repository.login("persona@example.invalid", "password") }.exceptionOrNull()
            assertTrue("Se esperaba error de respuesta, recibido ${error?.javaClass?.simpleName}", error is ApiException.Unexpected)
        }
    }

    @Test
    fun `401 405 and 502 preserve their HTTP status even when the body is HTML`() = runTest {
        for (status in listOf(401, 405, 502)) {
            server.enqueue(MockResponse().setResponseCode(status).setBody("<html>error</html>"))
            val error = runCatching { repository.login("persona@example.invalid", "password") }.exceptionOrNull()
            assertTrue(error is ApiException.Http)
            assertEquals(status, (error as ApiException.Http).statusCode)
        }
    }

    private fun response(role: String) = """
        {"access_token":"returned-token","token_type":"Bearer",
         "usuario":{"id":"user-id","nombre":"Persona","email":"persona@example.invalid","roles":["$role"]}}
    """.trimIndent()
}
