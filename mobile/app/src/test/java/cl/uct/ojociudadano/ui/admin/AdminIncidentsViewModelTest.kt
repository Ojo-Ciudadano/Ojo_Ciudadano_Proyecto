package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModelStore
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.domain.model.Evidence
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import cl.uct.ojociudadano.ui.reports.incidentMessage
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminIncidentsViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initial load uses general query once and preserves real ids and server order`() = runTest {
        val repository = Repository().apply { rows = listOf(incident, incident.copy(id = "other-id")) }
        val model = AdminIncidentsViewModel(repository)
        assertEquals(IncidentsUiState.Loading, model.state.value.status)
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(repository.rows, model.state.value.incidents)
        assertEquals(listOf("real-id", "other-id"), model.state.value.incidents.map { it.id })
        assertEquals(1, repository.calls)
    }

    @Test fun `successful empty response is distinct from error`() = runTest {
        val repository = Repository().apply { rows = emptyList() }
        val model = AdminIncidentsViewModel(repository)
        runCurrent()
        assertEquals(AdminIncidentsState(IncidentsUiState.Empty), model.state.value)
    }

    @Test fun `pending load ignores duplicate requests and retry recovers from network error`() = runTest {
        val pending = CompletableDeferred<List<Incident>>()
        val repository = Repository().apply { response = pending }
        val model = AdminIncidentsViewModel(repository)
        runCurrent()
        repeat(3) { model.loadIncidents() }
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals(AdminIncidentsState(), model.state.value)
        pending.completeExceptionally(ApiException.Network(IOException("private host")))
        runCurrent()
        assertTrue(model.state.value.status is IncidentsUiState.Error)
        repository.response = null
        model.loadIncidents()
        runCurrent()
        assertEquals(2, repository.calls)
        assertEquals(IncidentsUiState.Success, model.state.value.status)
    }

    @Test fun `failures clear old results and expose safe differentiated messages`() = runTest {
        val failures = listOf(
            ApiException.Network(IOException("private host")),
            ApiException.Http(401, listOf("private host"), null, null),
            ApiException.Http(403, listOf("private host"), null, null),
            ApiException.Http(503, listOf("private host"), null, null),
            ApiException.Unexpected(IllegalArgumentException("private host")),
            SessionRequiredException()
        )
        for (failure in failures) {
            val repository = Repository()
            val model = AdminIncidentsViewModel(repository)
            runCurrent()
            assertEquals(listOf(incident), model.state.value.incidents)
            repository.failure = failure
            model.loadIncidents()
            runCurrent()
            assertEquals(AdminIncidentsState(IncidentsUiState.Error(failure.incidentMessage())), model.state.value)
            assertFalse((model.state.value.status as IncidentsUiState.Error).message.contains("private host"))
        }
        assertEquals(failures.size, failures.map { it.incidentMessage() }.distinct().size)
    }

    @Test fun `disposing viewmodel cancels pending request without publishing error`() = runTest {
        val pending = CompletableDeferred<List<Incident>>()
        val repository = Repository().apply { response = pending }
        val model = AdminIncidentsViewModel(repository)
        val store = ViewModelStore().apply { put("admin", model) }
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(repository.cancelled)
        pending.complete(listOf(incident))
        runCurrent()
        assertEquals(AdminIncidentsState(), model.state.value)
    }

    private class Repository : IncidentRepository {
        var calls = 0
        var rows = listOf(incident)
        var failure: Exception? = null
        var response: CompletableDeferred<List<Incident>>? = null
        var cancelled = false
        override suspend fun getIncidents(): List<Incident> {
            calls++
            failure?.let { throw it }
            return try {
                response?.await() ?: rows
            } catch (exception: kotlin.coroutines.cancellation.CancellationException) {
                cancelled = true
                throw exception
            }
        }
        override suspend fun getCurrentUserIncidents(): List<Incident> = error("Must not query own incidents")
        override suspend fun getIncidentById(id: String): Incident = error("Must not query detail")
        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")
        override suspend fun uploadEvidence(incidenteId: String, file: File, mimeType: String): Evidence = error("Unused")
        override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> = error("Must not query photos")
        override suspend fun getEvidenceById(id: String): Evidence = error("Unused")
        override suspend fun downloadEvidenceFile(id: String): ResponseBody = error("Unused")
    }

    private companion object {
        val incident = Incident(
            "real-id", "Luminaria", "Sin iluminación",
            Category("category", "Alumbrado", "", 1, false, null),
            IncidentStatus("status", "Nuevo", 1),
            IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
            "another-citizen", null, "2026-11-02T12:00:00Z", "2026-11-02T12:00:00Z"
        )
    }
}
