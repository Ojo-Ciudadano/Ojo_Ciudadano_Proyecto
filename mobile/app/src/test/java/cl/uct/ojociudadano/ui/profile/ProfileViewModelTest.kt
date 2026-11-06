package cl.uct.ojociudadano.ui.profile

import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.ProfileRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.testSession
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val citizenId = "a0000000-0000-4000-8000-000000000001"
    private val otherCitizenId = "b0000000-0000-4000-8000-000000000002"

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun manager(userId: String = citizenId): DefaultSessionManager =
        DefaultSessionManager(TestSessionStorage(testSession(userId)), Dispatchers.Unconfined)

    private suspend fun authenticated(userId: String = citizenId): DefaultSessionManager =
        manager(userId).also { it.restore() }

    @Test fun `initial load moves from loading to success with the profile`() = runTest {
        val repository = Repository().apply { profile = profile(citizenId) }
        val model = ProfileViewModel(repository, authenticated())

        assertEquals(ProfileUiState.Loading, model.state.value)
        runCurrent()

        val state = model.state.value
        assertTrue(state is ProfileUiState.Success)
        assertEquals(profile(citizenId), (state as ProfileUiState.Success).profile)
        assertEquals(1, repository.calls)
    }

    @Test fun `server failure is retryable and retry recovers`() = runTest {
        val repository = Repository().apply {
            failure = ApiException.Http(503, listOf("Servicio no disponible"), null, null)
        }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        val error = model.state.value as ProfileUiState.Error
        assertTrue(error.canRetry)
        assertTrue(error.message.contains("503"))

        repository.failure = null
        repository.profile = profile(citizenId)
        model.retry()
        runCurrent()

        assertEquals(ProfileUiState.Success(profile(citizenId)), model.state.value)
        assertEquals(2, repository.calls)
    }

    @Test fun `expired session is terminal and is not offered for retry`() = runTest {
        val repository = Repository().apply {
            failure = ApiException.Http(401, listOf("No autorizado"), null, null)
        }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        val error = model.state.value as ProfileUiState.Error
        assertFalse(error.canRetry)
        assertTrue(error.message.contains("sesión"))
        assertEquals(1, repository.calls)
    }

    @Test fun `missing profile is terminal`() = runTest {
        val repository = Repository().apply {
            failure = ApiException.Http(404, listOf("Usuario no encontrado."), null, null)
        }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        val error = model.state.value as ProfileUiState.Error
        assertFalse(error.canRetry)
        assertTrue(error.message.contains("perfil"))
    }

    @Test fun `changed session is terminal instead of showing another user's data`() = runTest {
        val repository = Repository().apply { failure = SessionRequiredException() }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        val error = model.state.value as ProfileUiState.Error
        assertFalse(error.canRetry)
        assertTrue(error.message.contains("sesión"))
    }

    @Test fun `network failure is retryable`() = runTest {
        val repository = Repository().apply {
            failure = ApiException.Network(IOException("offline"))
        }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        assertTrue((model.state.value as ProfileUiState.Error).canRetry)
    }

    @Test fun `unexpected failure is retryable and never leaks internals`() = runTest {
        val repository = Repository().apply { failure = ApiException.Unexpected(IllegalStateException("boom")) }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        val error = model.state.value as ProfileUiState.Error
        assertTrue(error.canRetry)
        assertFalse(error.message.contains("boom"))
    }

    @Test fun `switching account discards the previous profile before loading the new one`() = runTest {
        val repository = Repository().apply { profile = profile(citizenId) }
        val sessionManager = authenticated()
        val model = ProfileViewModel(repository, sessionManager)
        runCurrent()
        assertEquals(ProfileUiState.Success(profile(citizenId)), model.state.value)

        // La nueva consulta queda pendiente para observar el estado intermedio.
        val pending = CompletableDeferred<Unit>()
        repository.pending = pending
        repository.profile = profile(otherCitizenId)
        sessionManager.establish(testSession(otherCitizenId))
        runCurrent()

        // Mientras carga el perfil nuevo no queda rastro del anterior.
        assertEquals(ProfileUiState.Loading, model.state.value)

        pending.complete(Unit)
        runCurrent()

        val state = model.state.value as ProfileUiState.Success
        assertEquals(otherCitizenId, state.profile.id)
        assertEquals(2, repository.calls)
    }

    @Test fun `logout clears the profile and stops querying`() = runTest {
        val repository = Repository().apply { profile = profile(citizenId) }
        val sessionManager = authenticated()
        val model = ProfileViewModel(repository, sessionManager)
        runCurrent()

        sessionManager.logout()
        runCurrent()

        assertEquals(ProfileUiState.Loading, model.state.value)
        assertEquals(1, repository.calls)
    }

    @Test fun `a duplicated load does not create a second request`() = runTest {
        val pending = CompletableDeferred<Unit>()
        val repository = Repository().apply { this.pending = pending }
        val model = ProfileViewModel(repository, authenticated())
        runCurrent()

        model.load()
        model.retry()
        runCurrent()

        assertEquals(1, repository.calls)

        pending.complete(Unit)
        runCurrent()
        assertTrue(model.state.value is ProfileUiState.Success)
    }

    private class Repository : ProfileRepository {
        var profile: UserProfile = profile("a0000000-0000-4000-8000-000000000001")
        var failure: Throwable? = null
        var pending: CompletableDeferred<Unit>? = null
        var calls = 0

        override suspend fun getProfile(): UserProfile {
            calls++
            pending?.await()
            failure?.let { throw it }
            return profile
        }
    }

}

private fun profile(userId: String) = UserProfile(
    id = userId,
    name = "Ciudadano $userId",
    email = "$userId@example.invalid",
    phone = "+56911111111",
    registeredAt = "2026-10-01T00:00:00.000Z",
    roles = listOf("CIUDADANO")
)
