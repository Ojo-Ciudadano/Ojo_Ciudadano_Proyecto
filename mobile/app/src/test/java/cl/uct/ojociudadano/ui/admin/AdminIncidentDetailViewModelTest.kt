package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.data.model.IncidentFilters
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.domain.model.Evidence
import cl.uct.ojociudadano.ui.reports.IncidentsUiState
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
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
class AdminIncidentDetailViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initial query uses selected id and retains complete incident and backend status`() = runTest {
        val repository = Repository()
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        assertEquals(AdminIncidentDetailState(incident.id), model.state.value)
        runCurrent()
        assertEquals(listOf(incident.id), repository.ids)
        assertEquals(listOf(incident.id), repository.historyIds)
        assertEquals(
            AdminIncidentDetailState(
                incident.id,
                IncidentsUiState.Success,
                incident,
                historyStatus = IncidentsUiState.Empty
            ),
            model.state.value
        )
        assertEquals(incident.status, model.state.value.currentStatus)
        model.retry()
        runCurrent()
        assertEquals(1, repository.ids.size)
    }

    @Test fun `missing and malformed ids fail locally without retry or history request`() = runTest {
        for (id in listOf(null, "", " ", "invalid", "1-1-1-1-1", "${incident.id}/other", " ${incident.id}")) {
            val repository = Repository()
            val model = AdminIncidentDetailViewModel(repository, id)
            model.retry()
            runCurrent()
            assertEquals(id, model.state.value.incidentId)
            assertTrue(model.state.value.status is IncidentsUiState.Error)
            assertFalse(model.state.value.canRetry)
            assertTrue(repository.ids.isEmpty())
            assertTrue(repository.historyIds.isEmpty())
        }
    }

    @Test fun `network timeout and server errors can recover with same id without duplicate loads`() = runTest {
        for (failure in listOf(ApiException.Network(IOException("private host")),
            ApiException.Network(SocketTimeoutException()), SocketTimeoutException(),
            http(500), http(503), http(408), http(429))) {
            val repository = Repository().apply { this.failure = failure }
            val model = AdminIncidentDetailViewModel(repository, incident.id)
            runCurrent()
            assertTrue(model.state.value.status is IncidentsUiState.Error)
            assertTrue(model.state.value.canRetry)
            assertEquals(incident.id, model.state.value.incidentId)
            assertNull(model.state.value.incident)
            assertFalse((model.state.value.status as IncidentsUiState.Error).message.contains("private host"))
            val pending = CompletableDeferred<Incident>()
            repository.failure = null
            repository.pending = pending
            repeat(3) { model.retry() }
            runCurrent()
            assertEquals(2, repository.ids.size)
            assertEquals(IncidentsUiState.Loading, model.state.value.status)
            model.retry()
            runCurrent()
            assertEquals(2, repository.ids.size)
            pending.complete(incident)
            runCurrent()
            assertEquals(IncidentsUiState.Success, model.state.value.status)
        }
    }

    @Test fun `authorization not found bad request and malformed response are terminal and safe`() = runTest {
        val failures = listOf(http(401), http(403), http(404), http(400),
            ApiException.Unexpected(IllegalStateException("private host")), SessionRequiredException())
        val messages = mutableListOf<String>()
        for (failure in failures) {
            val repository = Repository().apply { this.failure = failure }
            val model = AdminIncidentDetailViewModel(repository, incident.id)
            runCurrent()
            assertFalse(model.state.value.canRetry)
            assertNull(model.state.value.currentStatus)
            val error = model.state.value.status as IncidentsUiState.Error
            assertFalse(error.message.contains("private host"))
            messages += error.message
            model.retry()
            runCurrent()
            assertEquals(1, repository.ids.size)
        }
        assertEquals(failures.size, messages.distinct().size)
    }

    @Test fun `navigation owner reuses viewmodel while new owner restores query from route id`() = runTest {
        val repository = Repository()
        val store = ViewModelStore()
        fun model(owner: ViewModelStore) = ViewModelProvider.create(
            owner, AdminIncidentDetailViewModelFactory(repository, incident.id)
        )["admin-detail:${incident.id}", AdminIncidentDetailViewModel::class.java]
        val first = model(store)
        runCurrent()
        assertSame(first, model(store))
        runCurrent()
        assertEquals(1, repository.ids.size)
        store.clear()
        val restoredStore = ViewModelStore()
        val restored = model(restoredStore)
        runCurrent()
        assertEquals(incident, restored.state.value.incident)
        assertEquals(listOf(incident.id, incident.id), repository.ids)
        restoredStore.clear()
    }

    @Test fun `clearing navigation owner cancels request without publishing error`() = runTest {
        val pending = CompletableDeferred<Incident>()
        val repository = Repository().apply { this.pending = pending }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        val store = ViewModelStore().apply { put("detail", model) }
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(repository.cancelled)
        pending.complete(incident)
        runCurrent()
        assertEquals(
            AdminIncidentDetailState(incident.id, historyStatus = IncidentsUiState.Empty),
            model.state.value
        )
    }

    @Test fun `history is requested with the same id and published in chronological order`() = runTest {
        val repository = Repository().apply {
            historyRows = listOf(
                history("h2", "2026-11-03T12:00:00Z"),
                history("h1", "2026-11-02T12:00:00Z")
            )
        }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        assertEquals(listOf(incident.id), repository.historyIds)
        val state = model.state.value
        assertEquals(IncidentsUiState.Success, state.historyStatus)
        assertEquals(listOf("h1", "h2"), state.history.map { it.id })
        assertEquals(IncidentsUiState.Success, state.status)
    }

    @Test fun `empty history is an empty state and keeps the detail visible`() = runTest {
        val model = AdminIncidentDetailViewModel(Repository(), incident.id)
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(IncidentsUiState.Empty, model.state.value.historyStatus)
        assertTrue(model.state.value.history.isEmpty())
        assertEquals(incident, model.state.value.incident)
    }

    @Test fun `history failure is contained and retryable without reloading the detail`() = runTest {
        val repository = Repository().apply {
            historyFailure = ApiException.Network(IOException("private host"))
        }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertNotNull(model.state.value.incident)
        val error = model.state.value.historyStatus as IncidentsUiState.Error
        assertFalse(error.message.contains("private host"))
        assertTrue(model.state.value.historyCanRetry)

        model.retryHistory()
        runCurrent()
        assertEquals(1, repository.ids.size)
        assertEquals(2, repository.historyIds.size)
        assertTrue(model.state.value.historyStatus is IncidentsUiState.Error)

        repository.historyFailure = null
        repository.historyRows = listOf(history("h1", "2026-11-02T12:00:00Z"))
        model.retryHistory()
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.historyStatus)
        assertEquals(3, repository.historyIds.size)
    }

    @Test fun `terminal history failure is not retryable`() = runTest {
        for (failure in listOf(http(403), http(404), SessionRequiredException())) {
            val repository = Repository().apply { historyFailure = failure }
            val model = AdminIncidentDetailViewModel(repository, incident.id)
            runCurrent()
            assertFalse(model.state.value.historyCanRetry)
            assertEquals(IncidentsUiState.Success, model.state.value.status)
            model.retryHistory()
            runCurrent()
            assertEquals(1, repository.historyIds.size)
        }
    }

    @Test fun `pending history keeps the detail visible while the history loads`() = runTest {
        val pending = CompletableDeferred<List<IncidentStateHistory>>()
        val repository = Repository().apply { historyPending = pending }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(IncidentsUiState.Loading, model.state.value.historyStatus)
        pending.complete(listOf(history("h1", "2026-11-02T12:00:00Z")))
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.historyStatus)
        assertEquals(1, model.state.value.history.size)
    }

    @Test fun `detail failure does not discard an already published history`() = runTest {
        val repository = Repository().apply {
            failure = http(500)
            historyRows = listOf(history("h1", "2026-11-02T12:00:00Z"))
        }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        assertTrue(model.state.value.status is IncidentsUiState.Error)
        assertEquals(IncidentsUiState.Success, model.state.value.historyStatus)
        assertEquals(1, model.state.value.history.size)
    }

    private class Repository : IncidentRepository {
        val ids = mutableListOf<String>()
        val historyIds = mutableListOf<String>()
        var failure: Exception? = null
        var pending: CompletableDeferred<Incident>? = null
        var cancelled = false
        var historyFailure: Exception? = null
        var historyPending: CompletableDeferred<List<IncidentStateHistory>>? = null
        var historyRows: List<IncidentStateHistory> = emptyList()

        override suspend fun getIncidentById(id: String): Incident {
            ids += id
            failure?.let { throw it }
            return try {
                pending?.await() ?: incident
            } catch (exception: kotlin.coroutines.cancellation.CancellationException) {
                cancelled = true
                throw exception
            }
        }

        override suspend fun getIncidentHistory(incidentId: String): List<IncidentStateHistory> {
            historyIds += incidentId
            historyFailure?.let { throw it }
            return historyPending?.await() ?: historyRows
        }

        override suspend fun getIncidents(filters: IncidentFilters): List<Incident> = error("Must not query list")
        override suspend fun getCurrentUserIncidents(): List<Incident> = error("Must not query own incidents")
        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")
        override suspend fun uploadEvidence(incidenteId: String, file: File, mimeType: String): Evidence = error("Unused")
        override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> = error("Must not query photos")
        override suspend fun getEvidenceById(id: String): Evidence = error("Unused")
        override suspend fun downloadEvidenceFile(id: String): ResponseBody = error("Unused")
        override suspend fun assignResponsible(incidentId: String, assignedUserId: String): IncidentAssignment = error("Unused")
        override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident = error("Unused")
        override suspend fun getCandidates(page: Int?, limit: Int?): Paginated<CandidateUser> = error("Unused")
    }

    private fun http(code: Int) = ApiException.Http(code, listOf("private host"), null, null)

    private companion object {
        val incident = Incident(
            "d0000000-0000-4000-8000-000000000004", "Luminaria", "Sin iluminación",
            Category("category", "Alumbrado", "", 1, false, null),
            IncidentStatus("status", "En revisión", 2),
            IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
            "another-citizen", null, "2026-11-02T12:00:00Z", "2026-11-03T12:00:00Z"
        )

        fun history(id: String, date: String, observation: String? = "Se inicia la evaluación") =
            IncidentStateHistory(
                id = id,
                incidenteId = incident.id,
                status = IncidentStatus("status-$id", "En revisión", 2),
                userId = "b0000000-0000-4000-8000-000000000012",
                observation = observation,
                date = date
            )
    }
}
