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
        assertEquals(AdminIncidentDetailState(incident.id, IncidentsUiState.Success, incident), model.state.value)
        assertEquals(incident.status, model.state.value.currentStatus)
        model.retry()
        runCurrent()
        assertEquals(1, repository.ids.size)
    }

    @Test fun `missing and malformed ids fail locally without retry`() = runTest {
        for (id in listOf(null, "", " ", "invalid", "1-1-1-1-1", "${incident.id}/other", " ${incident.id}")) {
            val repository = Repository()
            val model = AdminIncidentDetailViewModel(repository, id)
            model.retry()
            runCurrent()
            assertEquals(id, model.state.value.incidentId)
            assertTrue(model.state.value.status is IncidentsUiState.Error)
            assertFalse(model.state.value.canRetry)
            assertTrue(repository.ids.isEmpty())
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
        assertEquals(AdminIncidentDetailState(incident.id), model.state.value)
    }

    @Test fun `priority is confirmed only after the backend responds`() = runTest {
        val repository = Repository()
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openPriorityDialog()
        assertTrue(model.state.value.priorityDialogVisible)
        model.selectPriority(4)
        assertEquals(4, model.state.value.selectedPriority)

        val pending = CompletableDeferred<Incident>()
        repository.priorityPending = pending
        model.confirmPriority()
        runCurrent()
        assertEquals(listOf(4), repository.priorityLevels)
        assertTrue(model.state.value.savingPriority)
        assertNull(model.state.value.currentPriority)
        assertTrue(model.state.value.priorityDialogVisible)

        pending.complete(incident.copy(priorityLevel = 4))
        runCurrent()
        assertEquals(4, model.state.value.currentPriority)
        assertFalse(model.state.value.priorityDialogVisible)
        assertTrue(model.state.value.priorityUpdated)
        assertFalse(model.state.value.savingPriority)
        assertNull(model.state.value.priorityError)
    }

    @Test fun `priority failure keeps the previous value and can be retried`() = runTest {
        val repository = Repository().apply { this.row = prioritizedIncident }
        repository.priorityFailure = http(503)
        val model = AdminIncidentDetailViewModel(repository, prioritizedIncident.id)
        runCurrent()
        assertEquals(3, model.state.value.currentPriority)

        model.openPriorityDialog()
        model.selectPriority(5)
        model.confirmPriority()
        runCurrent()
        assertEquals(listOf(5), repository.priorityLevels)
        assertEquals(3, model.state.value.currentPriority)
        assertTrue(model.state.value.priorityDialogVisible)
        assertEquals(
            "El servicio de incidentes no está disponible (HTTP 503). Intenta más tarde.",
            model.state.value.priorityError
        )
        assertTrue(model.state.value.priorityCanRetry)

        repository.priorityFailure = null
        model.confirmPriority()
        runCurrent()
        assertEquals(listOf(5, 5), repository.priorityLevels)
        assertEquals(5, model.state.value.currentPriority)
        assertFalse(model.state.value.priorityDialogVisible)
    }

    @Test fun `invalid unchanged and unselected priorities are never sent`() = runTest {
        val repository = Repository().apply { this.row = prioritizedIncident }
        val model = AdminIncidentDetailViewModel(repository, prioritizedIncident.id)
        runCurrent()
        model.openPriorityDialog()

        model.confirmPriority()
        runCurrent()
        model.selectPriority(0)
        assertNull(model.state.value.selectedPriority)
        model.selectPriority(6)
        assertNull(model.state.value.selectedPriority)
        model.selectPriority(3)
        assertEquals(3, model.state.value.selectedPriority)
        assertFalse(model.state.value.canConfirmPriority)
        model.confirmPriority()
        runCurrent()
        assertTrue(repository.priorityLevels.isEmpty())
        assertEquals(3, model.state.value.currentPriority)
    }

    @Test fun `confirmed priority from the backend wins over the requested one`() = runTest {
        val repository = Repository().apply { this.priorityResult = 5 }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openPriorityDialog()
        model.selectPriority(2)
        model.confirmPriority()
        runCurrent()
        assertEquals(listOf(2), repository.priorityLevels)
        assertEquals(5, model.state.value.currentPriority)
    }

    @Test fun `session change while saving priority is terminal`() = runTest {
        val repository = Repository().apply { this.priorityFailure = SessionRequiredException() }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openPriorityDialog()
        model.selectPriority(4)
        model.confirmPriority()
        runCurrent()
        assertNull(model.state.value.currentPriority)
        assertEquals(
            "La sesión no está disponible o cambió. Inicia sesión nuevamente.",
            model.state.value.priorityError
        )
        assertFalse(model.state.value.priorityCanRetry)
        assertTrue(model.state.value.priorityDialogVisible)
        assertFalse(model.state.value.savingPriority)
    }

    @Test fun `priority dialog is not dismissed nor duplicated while saving`() = runTest {
        val pending = CompletableDeferred<Incident>()
        val repository = Repository().apply { this.priorityPending = pending }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openPriorityDialog()
        model.selectPriority(1)
        model.confirmPriority()
        runCurrent()
        model.dismissPriorityDialog()
        assertTrue(model.state.value.priorityDialogVisible)
        model.confirmPriority()
        runCurrent()
        assertEquals(1, repository.priorityLevels.size)
        pending.complete(incident.copy(priorityLevel = 1))
        runCurrent()
        assertEquals(1, model.state.value.currentPriority)
    }

    @Test fun `priority update keeps the rest of the incident untouched`() = runTest {
        val repository = Repository()
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openPriorityDialog()
        model.selectPriority(4)
        model.confirmPriority()
        runCurrent()
        val updated = model.state.value.incident!!
        assertEquals(4, updated.priorityLevel)
        assertEquals(incident.status, updated.status)
        assertEquals(incident.category, updated.category)
        assertEquals(incident.location, updated.location)
        assertEquals(incident.history, updated.history)
        assertEquals(incident.activeAssignment, updated.activeAssignment)
        assertEquals(incident.evidences, updated.evidences)
        assertEquals(incident.createdAt, updated.createdAt)
    }

    private class Repository : IncidentRepository {
        val ids = mutableListOf<String>()
        var failure: Exception? = null
        var pending: CompletableDeferred<Incident>? = null
        var cancelled = false
        var row: Incident = incident
        val priorityLevels = mutableListOf<Int>()
        var priorityFailure: Exception? = null
        var priorityPending: CompletableDeferred<Incident>? = null
        var priorityResult: Int? = null
        override suspend fun getIncidentById(id: String): Incident {
            ids += id
            failure?.let { throw it }
            return try {
                pending?.await() ?: row
            } catch (exception: kotlin.coroutines.cancellation.CancellationException) {
                cancelled = true
                throw exception
            }
        }
        override suspend fun getIncidents(filters: IncidentFilters): List<Incident> = error("Must not query list")
        override suspend fun getCurrentUserIncidents(): List<Incident> = error("Must not query own incidents")
        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")
        override suspend fun uploadEvidence(incidenteId: String, file: File, mimeType: String): Evidence = error("Unused")
        override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> = error("Must not query photos")
        override suspend fun getEvidenceById(id: String): Evidence = error("Unused")
        override suspend fun downloadEvidenceFile(id: String): ResponseBody = error("Unused")
        override suspend fun getIncidentHistory(incidentId: String): List<IncidentStateHistory> = error("Unused")
        override suspend fun assignResponsible(incidentId: String, assignedUserId: String): IncidentAssignment = error("Unused")
        override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident {
            priorityLevels += priorityLevel
            priorityPending?.await()
            priorityFailure?.let { throw it }
            return row.copy(priorityLevel = priorityResult ?: priorityLevel)
        }
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
        val prioritizedIncident = incident.copy(priorityLevel = 3)
    }
}
