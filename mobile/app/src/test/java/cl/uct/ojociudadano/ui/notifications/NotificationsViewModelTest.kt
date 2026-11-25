package cl.uct.ojociudadano.ui.notifications

import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.NotificationRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    @Test fun `empty state becomes success when the repository publishes a new notification`() = runTest {
        val repository = Repository()
        val model = NotificationsViewModel(repository)
        runCurrent()
        assertEquals(NotificationsUiState.Empty, model.state.value)

        // El scheduler (#282) publica sin pasar por load(); la pantalla debe seguirlo.
        val incoming = notification(id = "n1", isRead = false)
        repository.publish(listOf(incoming))
        runCurrent()

        assertEquals(NotificationsUiState.Success, model.state.value)
        assertEquals(listOf(incoming), model.notifications.value)
        assertEquals(1, model.unreadCount.value)
    }

    @Test fun `a publication cannot replace a terminal error`() = runTest {
        val repository = Repository().apply { failure = ApiException.Network(IOException("offline")) }
        val model = NotificationsViewModel(repository)
        runCurrent()
        assertTrue(model.state.value is NotificationsUiState.Error)

        repository.publish(listOf(notification(id = "n1")))
        runCurrent()

        assertTrue(model.state.value is NotificationsUiState.Error)
    }

    @Test fun `opening an unread notification marks it as read`() = runTest {
        val repository = Repository().apply { rows = listOf(notification(id = "n1", isRead = false)) }
        val model = NotificationsViewModel(repository)
        runCurrent()

        model.openNotification(repository.rows.single())
        runCurrent()

        assertEquals(listOf("n1"), repository.markedIds)
        assertTrue(model.notifications.value.single().isRead)
    }

    @Test fun `opening an already read notification does not call the backend`() = runTest {
        val repository = Repository().apply { rows = listOf(notification(id = "n1", isRead = true)) }
        val model = NotificationsViewModel(repository)
        runCurrent()

        model.openNotification(repository.rows.single())
        runCurrent()

        assertTrue(repository.markedIds.isEmpty())
    }

    @Test fun `a rejected read operation stays silent for the navigation`() = runTest {
        val repository = Repository().apply {
            rows = listOf(notification(id = "n1", isRead = false))
            markFailure = ApiException.Network(IOException("offline"))
        }
        val model = NotificationsViewModel(repository)
        runCurrent()

        model.openNotification(repository.rows.single())
        runCurrent()

        // La navegación no se bloquea ni se convierte en un error de pantalla.
        assertEquals(NotificationsUiState.Success, model.state.value)
        assertFalse(model.notifications.value.single().isRead)
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

    @Test fun `refresh deduplicates retains content on error and allows another gesture`() = runTest {
        val repository = Repository().apply { rows = listOf(notification("before")) }
        val model = NotificationsViewModel(repository)
        runCurrent()
        val pending = CompletableDeferred<Unit>()
        repository.pending = pending
        repeat(4) { model.refresh() }
        runCurrent()
        assertEquals(2, repository.calls)
        assertTrue(model.isRefreshing.value)
        assertEquals(listOf("before"), model.notifications.value.map { it.id })
        repository.failure = IOException("offline")
        pending.complete(Unit)
        runCurrent()
        assertFalse(model.isRefreshing.value)
        assertTrue(model.state.value is NotificationsUiState.Error)
        assertEquals(listOf("before"), model.notifications.value.map { it.id })
        repository.pending = null
        repository.failure = null
        repository.rows = emptyList()
        model.refresh()
        runCurrent()
        assertEquals(NotificationsUiState.Empty, model.state.value)
        assertFalse(model.isRefreshing.value)
    }

    private class Repository : NotificationRepository {
        var rows: List<Notification> = emptyList()
        var failure: Throwable? = null
        var pending: CompletableDeferred<Unit>? = null
        var calls = 0

        private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
        private val _unreadCount = MutableStateFlow(0)

        override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
        override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

        override suspend fun refresh(): List<Notification> {
            calls++
            pending?.await()
            failure?.let { throw it }
            _notifications.value = rows
            _unreadCount.value = rows.count { !it.isRead }
            return rows
        }

        var markedIds = mutableListOf<String>()
        var markFailure: Throwable? = null

        override suspend fun markAsRead(id: String) {
            markFailure?.let { throw it }
            markedIds += id
            _notifications.value = _notifications.value.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
            _unreadCount.value = _notifications.value.count { !it.isRead }
        }

        override suspend fun markAllAsRead(): Int = error("Not used by this test")

        /** Simula una publicación del ciclo periódico de #282. */
        fun publish(rows: List<Notification>) {
            _notifications.value = rows
            _unreadCount.value = rows.count { !it.isRead }
        }

        override fun clear() {
            _notifications.value = emptyList()
            _unreadCount.value = 0
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
