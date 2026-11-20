package cl.uct.ojociudadano.data.notifications

import cl.uct.ojociudadano.data.model.Notification
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.NotificationRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.SessionManager
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.testSessionManager
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSyncSchedulerTest {

    @Test
    fun `refreshes immediately when the session is authenticated and the app is in foreground`() = runTest {
        val repository = FakeRepository()
        val scheduler = scheduler(repository, testSessionManager(), MutableStateFlow(true))

        scheduler.start()
        runCurrent()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `periodic cycle keeps refreshing while active`() = runTest {
        val repository = FakeRepository()
        val scheduler = scheduler(repository, testSessionManager(), MutableStateFlow(true))

        scheduler.start()
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(3, repository.refreshCalls)
    }

    @Test
    fun `does not query without a session`() = runTest {
        val repository = FakeRepository()
        val scheduler = scheduler(repository, unauthenticatedManager(), MutableStateFlow(true))

        scheduler.start()
        runCurrent()
        advanceTimeBy(180_000)
        runCurrent()

        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun `stops querying when the app moves to the background`() = runTest {
        val repository = FakeRepository()
        val foreground = MutableStateFlow(true)
        val scheduler = scheduler(repository, testSessionManager(), foreground)

        scheduler.start()
        runCurrent()
        assertEquals(1, repository.refreshCalls)

        foreground.value = false
        runCurrent()
        advanceTimeBy(180_000)
        runCurrent()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `stops querying when the session closes`() = runTest {
        val repository = FakeRepository()
        val sessionManager = testSessionManager()
        val scheduler = scheduler(repository, sessionManager, MutableStateFlow(true))

        scheduler.start()
        runCurrent()
        assertEquals(1, repository.refreshCalls)

        sessionManager.logout()
        runCurrent()
        advanceTimeBy(180_000)
        runCurrent()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `network failures are silent and the cycle keeps going`() = runTest {
        val repository = FakeRepository().apply {
            failure = ApiException.Network(IOException("offline"))
        }
        val scheduler = scheduler(repository, testSessionManager(), MutableStateFlow(true))

        scheduler.start()
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()

        assertTrue(repository.refreshCalls >= 3)
    }

    @Test
    fun `stop cancels the cycle`() = runTest {
        val repository = FakeRepository()
        val scheduler = scheduler(repository, testSessionManager(), MutableStateFlow(true))

        scheduler.start()
        runCurrent()
        scheduler.stop()
        advanceTimeBy(180_000)
        runCurrent()

        assertEquals(1, repository.refreshCalls)
    }

    private fun kotlinx.coroutines.test.TestScope.scheduler(
        repository: NotificationRepository,
        sessionManager: SessionManager,
        foreground: MutableStateFlow<Boolean>
    ) = NotificationSyncScheduler(
        repository = repository,
        sessionManager = sessionManager,
        foreground = foreground,
        scope = backgroundScope,
        intervalMillis = 60_000
    )

    private suspend fun unauthenticatedManager(): SessionManager =
        DefaultSessionManager(TestSessionStorage(session = null), Dispatchers.Unconfined).also {
            // restore() deja el estado en Unauthenticated cuando no hay sesión.
            it.restore()
        }

    private class FakeRepository : NotificationRepository {
        var refreshCalls = 0
        var failure: Throwable? = null

        private val _notifications = MutableStateFlow<List<Notification>>(emptyList())

        override val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
        override val unreadCount: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun refresh(): List<Notification> {
            refreshCalls++
            failure?.let { throw it }
            return _notifications.value
        }

        override suspend fun markAsRead(id: String) = Unit
        override suspend fun markAllAsRead(): Int = 0
        override fun clear() {
            _notifications.value = emptyList()
        }
    }
}
