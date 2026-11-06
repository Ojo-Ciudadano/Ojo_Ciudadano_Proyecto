package cl.uct.ojociudadano.ui.notifications

import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.NotificationRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
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
class NotificationsViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initial load moves from loading to success preserving the data layer order`() = runTest {
        val repository = Repository().apply {
            rows = listOf(
                notification(id = "newest", createdAt = "2026-11-04T12:00:00Z"),
                notification(id = "middle", createdAt = "2026-11-03T12:00:00Z"),
                notification(id = "oldest", createdAt = "2026-11-02T12:00:00Z")
            )
        }
        val model = NotificationsViewModel(repository)
        assertEquals(NotificationsUiState.Loading, model.state.value)
        runCurrent()
        assertEquals(NotificationsUiState.Success, model.state.value)
        // El backend ordena por fecha descendente; la pantalla no debe reordenar.
        assertEquals(repository.rows, model.notifications.value)
        assertEquals(listOf("newest", "middle", "oldest"), model.notifications.value.map { it.id })
        assertEquals(1, repository.calls)
    }

    @Test fun `empty response is a valid empty state and not an error`() = runTest {
        val repository = Repository().apply { rows = emptyList() }
        val model = NotificationsViewModel(repository)
        runCurrent()
        assertEquals(NotificationsUiState.Empty, model.state.value)
        assertTrue(model.notifications.value.isEmpty())
    }

    @Test fun `content keeps message date and incident reference available to the screen`() = runTest {
        val repository = Repository().apply {
            rows = listOf(
                notification(
                    id = "notice",
                    message = "Tu incidente fue asignado a un administrador.",
                    createdAt = "2026-11-03T08:30:00Z",
                    incidentId = "d0000000-0000-4000-8000-000000000004",
                    isRead = false
                )
            )
        }
        val model = NotificationsViewModel(repository)
        runCurrent()
        val row = model.notifications.value.single()
        assertEquals("Tu incidente fue asignado a un administrador.", row.message)
        assertEquals("2026-11-03T08:30:00Z", row.createdAt)
        assertEquals("d0000000-0000-4000-8000-000000000004", row.incidentId)
        assertFalse(row.isRead)
    }

    @Test fun `backend error is represented in a controlled way and retry recovers`() = runTest {
        val repository = Repository().apply {
            failure = ApiException.Http(503, emptyList(), null, "/api/v1/notificaciones")
        }
        val model = NotificationsViewModel(repository)
        runCurrent()
        val error = model.state.value as NotificationsUiState.Error
        assertTrue(error.message.contains("503"))
        assertTrue(error.canRetry)

        repository.failure = null
        repository.rows = listOf(notification(id = "recovered"))
        model.retry()
        runCurrent()
        assertEquals(NotificationsUiState.Success, model.state.value)
        assertEquals(listOf("recovered"), model.notifications.value.map { it.id })
        assertEquals(2, repository.calls)
    }

    @Test fun `network failure offers retry because the connection may return`() = runTest {
        val repository = Repository().apply { failure = ApiException.Network(IOException("offline")) }
        val model = NotificationsViewModel(repository)
        runCurrent()
        val error = model.state.value as NotificationsUiState.Error
        assertTrue(error.message.contains("conectar"))
        assertTrue(error.canRetry)
    }

    @Test fun `expired session is terminal and never invites a useless retry`() = runTest {
        val repository = Repository().apply { failure = SessionRequiredException() }
        val model = NotificationsViewModel(repository)
        runCurrent()
        val error = model.state.value as NotificationsUiState.Error
        assertEquals(SessionRequiredException().message.orEmpty(), error.message)
        assertFalse(error.canRetry)
    }

    @Test fun `unauthorized response is terminal for the current session`() = runTest {
        val repository = Repository().apply { failure = ApiException.Http(401, emptyList(), null, null) }
        val model = NotificationsViewModel(repository)
        runCurrent()
        assertFalse((model.state.value as NotificationsUiState.Error).canRetry)
    }

    @Test fun `a second load while the query is pending is ignored`() = runTest {
        val repository = Repository()
        val gate = CompletableDeferred<Unit>()
        repository.pending = gate
        repository.rows = listOf(notification(id = "only"))
        val model = NotificationsViewModel(repository)
        runCurrent()
        model.load()
        model.load()
        runCurrent()
        assertEquals(1, repository.calls)
        gate.complete(Unit)
        runCurrent()
        assertEquals(NotificationsUiState.Success, model.state.value)
        assertEquals(1, repository.calls)
    }

    @Test fun `reloading after returning to the screen reflects the newest response`() = runTest {
        val repository = Repository().apply { rows = listOf(notification(id = "before")) }
        val model = NotificationsViewModel(repository)
        runCurrent()
        assertEquals(listOf("before"), model.notifications.value.map { it.id })

        repository.rows = listOf(notification(id = "after-newest"), notification(id = "after-oldest"))
        model.load()
        runCurrent()
        assertEquals(NotificationsUiState.Success, model.state.value)
        assertEquals(listOf("after-newest", "after-oldest"), model.notifications.value.map { it.id })
        assertEquals(2, repository.calls)
    }

    @Test fun `unexpected parsing failure stays recoverable`() = runTest {
        val repository = Repository().apply { failure = ApiException.Unexpected(IllegalStateException("bad json")) }
        val model = NotificationsViewModel(repository)
        runCurrent()
        val error = model.state.value as NotificationsUiState.Error
        assertTrue(error.canRetry)
        assertTrue(error.message.isNotBlank())
    }

    private class Repository : NotificationRepository {
        var rows: List<Notification> = emptyList()
        var failure: Throwable? = null
        var pending: CompletableDeferred<Unit>? = null
        var calls = 0

        override suspend fun getNotifications(): List<Notification> {
            calls++
            pending?.await()
            failure?.let { throw it }
            return rows
        }
    }

    private fun notification(
        id: String,
        message: String = "Cambió el estado de tu incidente.",
        createdAt: String = "2026-11-02T10:00:00Z",
        incidentId: String = "d0000000-0000-4000-8000-000000000004",
        isRead: Boolean = true
    ) = Notification(
        id = id,
        userId = "a0000000-0000-4000-8000-000000000001",
        incidentId = incidentId,
        message = message,
        isRead = isRead,
        createdAt = createdAt
    )
}
