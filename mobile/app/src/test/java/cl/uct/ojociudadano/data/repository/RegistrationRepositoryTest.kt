package cl.uct.ojociudadano.data.repository

import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.TokenProvider
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RegistrationRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteAuthRepository
    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        repository = RemoteAuthRepository(RetrofitClient.createApiService(server.url("/").toString()))
    }
    @After fun tearDown() { server.shutdown() }
    private fun input(phone: String = "") = RegistrationInput(" Ana ", " ana@example.invalid ", " 123456 ", phone)
    private fun success() = MockResponse().setResponseCode(201).setBody(
        """{"id":"user-id","nombre":"Ana","email":"ana@example.invalid","telefono":null,"fechaRegistro":"2026-10-28T00:00:00.000Z"}""")

    @Test fun requestAnd201ResponseUseRealContract() = runTest {
        server.enqueue(success())
        val user = repository.register(input(" +56 9 1234 5678 "))
        assertEquals("user-id", user.id)
        assertEquals("Ana", user.name)
        assertEquals("ana@example.invalid", user.email)
        val request = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS))
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/register", request.path)
        val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
        assertEquals(setOf("nombre", "email", "password", "telefono"), json.keySet())
        assertEquals("Ana", json["nombre"].asString)
        assertEquals("ana@example.invalid", json["email"].asString)
        assertEquals(" 123456 ", json["password"].asString)
        assertEquals("+56 9 1234 5678", json["telefono"].asString)
        assertNull(request.getHeader("Authorization"))
    }
    @Test fun emptyPhoneIsOmitted() = runTest {
        server.enqueue(success())
        repository.register(input("  "))
        val json = JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
        assertFalse(json.has("telefono"))
        assertFalse(json.has("roles"))
    }
    @Test fun duplicateEmailAndValidationErrorsPreserveCodesAndFormats() = runTest {
        listOf(409 to "\"Ya existe una cuenta con ese correo\"", 400 to "[\"email must be an email\"]", 422 to "[\"telefono invalid\"]").forEach { (code, message) ->
            server.enqueue(MockResponse().setResponseCode(code).setBody("""{"statusCode":$code,"message":$message,"error":"Bad Request","path":"/auth/register"}"""))
            val error = runCatching { repository.register(input()) }.exceptionOrNull() as ApiException.Http
            assertEquals(code, error.statusCode)
            assertTrue(error.serverMessages.isNotEmpty())
        }
    }
    @Test fun connectionLossIsReportedAndNeverAutomaticallyReplayed() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        server.enqueue(success())
        val error = runCatching { repository.register(input()) }.exceptionOrNull()
        assertTrue(error is ApiException.Network)
        assertEquals(1, server.requestCount)
    }
    @Test fun malformedAndIncompleteSuccessAndServerFailureAreControlled() = runTest {
        listOf("{}", "{\"id\":\"id\"}", "[]", "not json").forEach { body ->
            server.enqueue(MockResponse().setResponseCode(201).setBody(body))
            val error = runCatching { repository.register(input()) }.exceptionOrNull()
            assertTrue("body=$body error=${error?.javaClass?.simpleName} cause=${error?.cause?.javaClass?.simpleName}", error is ApiException.Unexpected)
        }
        server.enqueue(MockResponse().setResponseCode(503).setBody("upstream unavailable"))
        assertEquals(503, (runCatching { repository.register(input()) }.exceptionOrNull() as ApiException.Http).statusCode)
    }
    @Test fun registrationIsPublicEvenWithExistingTokenAnd401DoesNotInvalidateSession() = runTest {
        var invalidations = 0
        val provider = object : TokenProvider { override fun currentToken() = "existing-token" }
        val api = RetrofitClient.createApiService(server.url("/").toString(), provider) { invalidations++ }
        server.enqueue(MockResponse().setResponseCode(401))
        runCatching { RemoteAuthRepository(api).register(input()) }
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(0, invalidations)
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        api.getCategories()
        assertEquals("Bearer existing-token", server.takeRequest().getHeader("Authorization"))
    }
}
