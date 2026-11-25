package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModelStore
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.repository.UserRepository
import cl.uct.ojociudadano.data.session.UserRole
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminUsersViewModelTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `all omits role and follows server pages preserving filter`() = runTest {
        val repository = Repository()
        val model = AdminUsersViewModel(repository)
        assertEquals(UsersLoadState.Loading, model.state.value.status)
        runCurrent()
        assertEquals(Triple(1, 10, null), repository.calls.single())
        model.previousPage()
        assertEquals(1, repository.calls.size)
        model.selectFilter(UserRoleFilter.ADMIN)
        runCurrent()
        model.nextPage()
        runCurrent()
        assertEquals(Triple(2, 10, UserRole.ADMIN), repository.calls.last())
        model.nextPage()
        runCurrent()
        assertFalse(model.state.value.canGoForward)
        model.nextPage()
        runCurrent()
        assertEquals(3, model.state.value.page)
        model.previousPage()
        runCurrent()
        assertEquals(2, model.state.value.page)
        model.selectFilter(UserRoleFilter.CITIZEN)
        runCurrent()
        assertEquals(Triple(1, 10, UserRole.CIUDADANO), repository.calls.last())
        model.selectFilter(UserRoleFilter.ALL)
        runCurrent()
        assertNull(repository.calls.last().third)
    }

    @Test fun `retry keeps failed page and filter and clears old rows`() = runTest {
        val repository = Repository()
        val model = AdminUsersViewModel(repository)
        runCurrent()
        model.selectFilter(UserRoleFilter.ADMIN)
        runCurrent()
        repository.failure = IOException("private host")
        model.nextPage()
        runCurrent()
        assertTrue(model.state.value.status is UsersLoadState.Error)
        assertNull(model.state.value.result)
        assertFalse((model.state.value.status as UsersLoadState.Error).message.contains("private host"))
        repository.failure = null
        model.retry()
        runCurrent()
        assertEquals(listOf(Triple(2, 10, UserRole.ADMIN), Triple(2, 10, UserRole.ADMIN)), repository.calls.takeLast(2))
        assertEquals(UsersLoadState.Ready, model.state.value.status)
        assertEquals(2, model.state.value.page)
    }

    @Test fun `empty results preserve metadata and disable unavailable pages`() = runTest {
        val model = AdminUsersViewModel(UserRepository { page, limit, _ -> Paginated(emptyList(), page, limit, 0, 0) })
        runCurrent()
        assertEquals(UsersLoadState.Ready, model.state.value.status)
        assertTrue(model.state.value.result!!.items.isEmpty())
        assertFalse(model.state.value.canGoBack)
        assertFalse(model.state.value.canGoForward)
    }

    @Test fun `late uncancellable response cannot overwrite latest filter`() = runTest {
        val old = CompletableDeferred<Paginated<UserProfile>>()
        val model = AdminUsersViewModel(UserRepository { page, limit, role ->
            if (role == null) withContext(NonCancellable) { old.await() }
            else Paginated(listOf(user.copy(id = "latest")), page, limit, 1, 1)
        })
        runCurrent()
        model.selectFilter(UserRoleFilter.ADMIN)
        model.selectFilter(UserRoleFilter.CITIZEN)
        assertEquals(UserRoleFilter.CITIZEN, model.state.value.filter)
        assertNull(model.state.value.result)
        runCurrent()
        old.complete(Paginated(listOf(user), 1, 10, 1, 1))
        runCurrent()
        assertEquals(UserRoleFilter.CITIZEN, model.state.value.filter)
        assertEquals("latest", model.state.value.result!!.items.single().id)
    }

    @Test fun `clearing viewmodel cancels work without publishing error`() = runTest {
        val pending = CompletableDeferred<Paginated<UserProfile>>()
        val model = AdminUsersViewModel(UserRepository { _, _, _ -> pending.await() })
        val store = ViewModelStore().apply { put("users", model) }
        runCurrent()
        store.clear()
        pending.complete(Paginated(listOf(user), 1, 10, 1, 1))
        runCurrent()
        assertEquals(UsersLoadState.Loading, model.state.value.status)
    }

    private class Repository : UserRepository {
        val calls = mutableListOf<Triple<Int, Int, UserRole?>>()
        var failure: Exception? = null
        override suspend fun getUsers(page: Int, limit: Int, role: UserRole?): Paginated<UserProfile> {
            calls += Triple(page, limit, role)
            failure?.let { throw it }
            return Paginated(listOf(user), page, limit, 21, 3)
        }
    }
    private companion object {
        val user = UserProfile("id", "Nombre", "test@example.invalid", null, null, listOf("ADMIN"))
    }
}
