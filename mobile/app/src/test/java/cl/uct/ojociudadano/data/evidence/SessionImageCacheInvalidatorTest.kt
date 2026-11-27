package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.data.session.SessionState
import cl.uct.ojociudadano.data.session.testSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionImageCacheInvalidatorTest {

    private val clearedCaches = mutableListOf<String>()

    @Test
    fun `clears both caches when the account changes`() = runTest(UnconfinedTestDispatcher()) {
        val sessions = MutableStateFlow<SessionState>(authenticated("usuario-1"))
        val invalidator = invalidator()

        invalidator.observe(backgroundScope, sessions)

        sessions.value = authenticated("usuario-2")

        assertEquals(listOf("memory", "disk"), clearedCaches)
    }

    @Test
    fun `clears both caches when the session is closed`() = runTest(UnconfinedTestDispatcher()) {
        val sessions = MutableStateFlow<SessionState>(authenticated("usuario-1"))
        val invalidator = invalidator()

        invalidator.observe(backgroundScope, sessions)

        sessions.value = SessionState.Unauthenticated

        assertEquals(listOf("memory", "disk"), clearedCaches)
    }

    @Test
    fun `does not clear the cache while the session is still being restored`() = runTest(UnconfinedTestDispatcher()) {
        val sessions = MutableStateFlow<SessionState>(SessionState.Restoring)
        val invalidator = invalidator()

        invalidator.observe(backgroundScope, sessions)

        sessions.value = authenticated("usuario-1")

        assertEquals(emptyList<String>(), clearedCaches)
    }

    @Test
    fun `keeps clearing when one cache fails`() = runTest(UnconfinedTestDispatcher()) {
        val sessions = MutableStateFlow<SessionState>(authenticated("usuario-1"))
        val failures = mutableListOf<Throwable>()
        val invalidator = SessionImageCacheInvalidator(
            clearMemoryCache = { throw IllegalStateException("memoria no disponible") },
            clearDiskCache = { clearedCaches += "disk" },
            reportFailure = { failures += it }
        )

        invalidator.observe(backgroundScope, sessions)

        sessions.value = authenticated("usuario-2")

        assertEquals(listOf("disk"), clearedCaches)
        assertEquals(1, failures.size)
    }

    private fun invalidator() = SessionImageCacheInvalidator(
        clearMemoryCache = { clearedCaches += "memory" },
        clearDiskCache = { clearedCaches += "disk" }
    )

    private fun authenticated(userId: String) = SessionState.Authenticated(testSession(userId))
}
