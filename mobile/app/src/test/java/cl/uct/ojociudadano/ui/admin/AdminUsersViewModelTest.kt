package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModelStore
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.error.ApiException
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
        val model = AdminUsersViewModel(Repository { page, limit, _ -> Paginated(emptyList(), page, limit, 0, 0) })
        runCurrent()
        assertEquals(UsersLoadState.Ready, model.state.value.status)
        assertTrue(model.state.value.result!!.items.isEmpty())
        assertFalse(model.state.value.canGoBack)
        assertFalse(model.state.value.canGoForward)
    }

    @Test fun `late uncancellable response cannot overwrite latest filter`() = runTest {
        val old = CompletableDeferred<Paginated<UserProfile>>()
        val model = AdminUsersViewModel(Repository { page, limit, role ->
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
        val model = AdminUsersViewModel(Repository { _, _, _ -> pending.await() })
        val store = ViewModelStore().apply { put("users", model) }
        runCurrent()
        store.clear()
        pending.complete(Paginated(listOf(user), 1, 10, 1, 1))
        runCurrent()
        assertEquals(UsersLoadState.Loading, model.state.value.status)
    }

    @Test fun `dismissing confirmation preserves the displayed role without sending a change`() = runTest {
        val repository = Repository()
        val model = AdminUsersViewModel(repository)
        runCurrent()

        model.requestRoleChange(user.id)
        assertEquals(RoleChangeConfirmation(user.id, user.name, UserRole.CIUDADANO), model.state.value.roleChange)
        assertTrue(repository.roleCalls.isEmpty())
        model.dismissRoleChange()
        model.confirmRoleChange()
        runCurrent()

        assertNull(model.state.value.roleChange)
        assertTrue(repository.roleCalls.isEmpty())
        assertEquals(user.roles, model.state.value.result!!.items.single().roles)
    }

    @Test fun `rapid confirmations send one change and only apply roles returned by the server`() = runTest {
        val pending = CompletableDeferred<List<String>>()
        val repository = Repository().apply { update = { pending.await() } }
        val model = AdminUsersViewModel(repository)
        runCurrent()

        model.requestRoleChange(user.id)
        model.confirmRoleChange()
        model.confirmRoleChange()
        model.requestRoleChange(user.id)
        runCurrent()
        assertEquals(listOf(user.id to UserRole.CIUDADANO), repository.roleCalls)
        assertNull(model.state.value.roleChange)
        assertEquals(setOf(user.id), model.state.value.changingRoleIds)
        assertEquals(UsersLoadState.Ready, model.state.value.status)
        assertEquals(user.roles, model.state.value.result!!.items.single().roles)

        pending.complete(listOf("CIUDADANO"))
        runCurrent()
        assertTrue(model.state.value.changingRoleIds.isEmpty())
        assertEquals(listOf("CIUDADANO"), model.state.value.result!!.items.single().roles)
    }

    @Test fun `a failed change keeps the role and requires a new confirmation to retry`() = runTest {
        val message = "No puedes quitarte tu propio rol de administrador."
        val repository = Repository().apply {
            update = { throw ApiException.InsufficientPermissions(listOf(message)) }
        }
        val model = AdminUsersViewModel(repository)
        runCurrent()
        model.requestRoleChange(user.id)
        model.confirmRoleChange()
        runCurrent()
        assertEquals(user.roles, model.state.value.result!!.items.single().roles)
        assertEquals(message, model.state.value.roleChangeError)
        assertTrue(model.state.value.changingRoleIds.isEmpty())

        repository.update = { listOf("CIUDADANO") }
        model.confirmRoleChange()
        model.requestRoleChange(user.id)
        runCurrent()
        assertEquals(1, repository.roleCalls.size)
        model.confirmRoleChange()
        runCurrent()
        assertEquals(2, repository.roleCalls.size)
        assertEquals(listOf("CIUDADANO"), model.state.value.result!!.items.single().roles)
        assertNull(model.state.value.roleChangeError)
    }

    @Test fun `a change under a role filter reloads the page and recovers an empty last page`() = runTest {
        val otherUsers = List(10) { user.copy(id = "other-$it") }
        var hasRole = true
        val repository = Repository { page, limit, _ ->
            val rows = if (page == 1) otherUsers else listOf(user).filter { hasRole }
            Paginated(rows, page, limit, if (hasRole) 11 else 10, if (hasRole) 2 else 1)
        }.apply {
            update = {
                hasRole = false
                listOf("CIUDADANO")
            }
        }
        val model = AdminUsersViewModel(repository)
        runCurrent()
        model.selectFilter(UserRoleFilter.ADMIN)
        runCurrent()
        model.nextPage()
        runCurrent()
        assertEquals(user, model.state.value.result!!.items.single())

        model.requestRoleChange(user.id)
        model.confirmRoleChange()
        runCurrent()
        assertEquals(
            listOf(Triple(2, 10, UserRole.ADMIN), Triple(1, 10, UserRole.ADMIN)),
            repository.calls.takeLast(2)
        )
        val state = model.state.value
        assertEquals(UserRoleFilter.ADMIN, state.filter)
        assertEquals(Paginated(otherUsers, 1, 10, 10, 1), state.result)
        assertEquals(1, state.page)
        assertFalse(state.canGoForward)
    }

    @Test fun `a refresh after a role change discards an in-flight read for the selected filter`() = runTest {
        val pendingUpdate = CompletableDeferred<List<String>>()
        val stalePage = CompletableDeferred<Paginated<UserProfile>>()
        val updatedUser = user.copy(roles = listOf("CIUDADANO"))
        var readCount = 0
        val repository = Repository { page, limit, filter ->
            when {
                filter == null -> Paginated(listOf(user), page, limit, 1, 1)
                readCount++ == 0 -> withContext(NonCancellable) { stalePage.await() }
                else -> Paginated(listOf(updatedUser), page, limit, 1, 1)
            }
        }.apply { update = { pendingUpdate.await() } }
        val model = AdminUsersViewModel(repository)
        runCurrent()
        model.requestRoleChange(user.id)
        model.confirmRoleChange()
        model.selectFilter(UserRoleFilter.CITIZEN)
        runCurrent()

        pendingUpdate.complete(updatedUser.roles)
        runCurrent()
        stalePage.complete(Paginated(emptyList(), 1, 10, 0, 0))
        runCurrent()
        assertEquals(UserRoleFilter.CITIZEN, model.state.value.filter)
        assertEquals(updatedUser, model.state.value.result!!.items.single())
    }

    private class Repository(
        private val list: suspend (Int, Int, UserRole?) -> Paginated<UserProfile> =
            { page, limit, _ -> Paginated(listOf(user), page, limit, 21, 3) }
    ) : UserRepository {
        val calls = mutableListOf<Triple<Int, Int, UserRole?>>()
        val roleCalls = mutableListOf<Pair<String, UserRole>>()
        var failure: Exception? = null
        var update: suspend (UserRole) -> List<String> = { error("Unexpected role change") }

        override suspend fun getUsers(page: Int, limit: Int, role: UserRole?): Paginated<UserProfile> {
            calls += Triple(page, limit, role)
            failure?.let { throw it }
            return list(page, limit, role)
        }

        override suspend fun updateUserRole(userId: String, role: UserRole): List<String> {
            roleCalls += userId to role
            return update(role)
        }
    }
    private companion object {
        val user = UserProfile("id", "Nombre", "test@example.invalid", null, null, listOf("ADMIN"))
    }
}
