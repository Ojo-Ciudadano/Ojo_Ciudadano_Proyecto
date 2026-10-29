package cl.uct.ojociudadano.ui.registration

import cl.uct.ojociudadano.data.model.RegisteredUser
import cl.uct.ojociudadano.data.model.RegistrationInput
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.AuthRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val input = RegistrationInput("Ana", "ana@example.invalid", "123456")
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun initialState() { assertEquals(RegistrationUiState.Idle, RegistrationViewModel(Repository()).uiState.value) }
    @Test fun invalidFormNeverCallsRepository() {
        val repository = Repository()
        val vm = RegistrationViewModel(repository)
        vm.register(RegistrationInput("", "", ""))
        assertTrue(vm.uiState.value is RegistrationUiState.Invalid)
        assertEquals(0, repository.calls)
    }
    @Test fun loadingPreventsDuplicatesAndSuccessNeverLogsIn() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = Repository { release.await(); user }
        val vm = RegistrationViewModel(repository)
        vm.register(input)
        assertEquals(RegistrationUiState.Loading, vm.uiState.value)
        vm.register(input)
        assertEquals(1, repository.calls)
        release.complete(Unit)
        assertEquals(RegistrationUiState.Success, vm.uiState.value)
        vm.register(input)
        assertEquals(1, repository.calls)
        assertEquals(0, repository.loginCalls)
    }
    @Test fun failureAndManualRecovery() = runTest {
        var fail = true
        val repository = Repository { if (fail) throw IOException("private data") else user }
        val vm = RegistrationViewModel(repository)
        vm.register(input)
        assertTrue(vm.uiState.value is RegistrationUiState.Error)
        assertEquals(1, repository.calls) // No automatic retry.
        fail = false
        vm.register(input)
        assertEquals(RegistrationUiState.Success, vm.uiState.value)
    }
    @Test fun duplicateEmailIsAttachedToEmailField() {
        val vm = errorVm(http(409))
        assertNotNull((vm.uiState.value as RegistrationUiState.Error).fields.email)
    }
    @Test fun validationIdentifiesFieldsWithoutExposingServerText() {
        val vm = errorVm(http(400, listOf("email invalid secret", "password too short", "nombre invalid", "telefono invalid")))
        val error = vm.uiState.value as RegistrationUiState.Error
        assertNotNull(error.fields.name)
        assertNotNull(error.fields.email)
        assertNotNull(error.fields.password)
        assertNotNull(error.fields.phone)
        assertFalse(error.message.contains("secret"))
    }
    @Test fun errorsAreSafeAndRecoverable() {
        listOf(http(422), http(503), http(401), ApiException.Network(IOException("secret")), IllegalStateException("secret")).forEach {
            val state = errorVm(it).uiState.value as RegistrationUiState.Error
            assertFalse(state.message.contains("secret"))
        }
    }
    @Test fun cancellationIsNotAnError() {
        val vm = RegistrationViewModel(Repository { throw CancellationException() })
        vm.register(input)
        assertFalse(vm.uiState.value is RegistrationUiState.Error)
    }
    private fun errorVm(error: Exception) = RegistrationViewModel(Repository { throw error }).also { it.register(input) }
    private fun http(code: Int, messages: List<String> = emptyList()) = ApiException.Http(code, messages, null, null)
    private class Repository(val action: suspend () -> RegisteredUser = { user }) : AuthRepository {
        var calls = 0
        var loginCalls = 0
        override suspend fun register(input: RegistrationInput): RegisteredUser { calls++; return action() }
        override suspend fun login(email: String, password: String): AuthenticatedSession { loginCalls++; error("Registration must not log in") }
    }
    companion object { private val user = RegisteredUser("id", "Ana", "ana@example.invalid") }
}
