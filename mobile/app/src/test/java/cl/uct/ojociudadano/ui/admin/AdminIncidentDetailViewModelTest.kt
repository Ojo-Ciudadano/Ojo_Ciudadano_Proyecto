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
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(incident, model.state.value.incident)
        assertEquals(AssignmentCandidates.Ready(emptyList(), 0), model.state.value.candidates)
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

    @Test fun `preloaded catalog resolves the confirmed responsible name`() = runTest {
        val repository = Repository().apply { row = assignedIncident; candidatePage = page(listOf(admin)) }
        val model = AdminIncidentDetailViewModel(repository, assignedIncident.id)
        runCurrent()
        assertEquals(admin.name, model.state.value.responsibleName)
        assertEquals(assignment, model.state.value.incident?.activeAssignment)
        assertEquals(AssignmentCandidates.Ready(listOf(admin), 1), model.state.value.candidates)
        assertEquals(1, repository.candidateCalls)
    }

    @Test fun `picker selection is temporary and dismissing restores the persisted assignment`() = runTest {
        val repository = Repository().apply { row = assignedIncident; candidatePage = page(listOf(admin, otherAdmin)) }
        val model = AdminIncidentDetailViewModel(repository, assignedIncident.id)
        runCurrent()
        model.openAssignmentPicker()
        runCurrent()
        assertTrue(model.state.value.pickerVisible)
        assertNull(model.state.value.assignmentError)

        model.selectCandidate(otherAdmin.id)
        assertEquals(otherAdmin, model.state.value.selectedCandidate)
        // The pending selection never replaces the confirmed responsible.
        assertEquals(assignment, model.state.value.incident?.activeAssignment)
        assertEquals(assignedIncident, model.state.value.incident)

        model.dismissAssignmentPicker()
        assertFalse(model.state.value.pickerVisible)
        assertNull(model.state.value.selectedCandidateId)
        assertEquals(assignment, model.state.value.incident?.activeAssignment)
        assertTrue(repository.assignments.isEmpty())
    }

    @Test fun `confirming assigns the selected administrator and reflects only the backend response`() = runTest {
        val repository = Repository().apply { candidatePage = page(listOf(admin)) }
        val confirmed = assignment.copy(id = "a2", assignedUserId = admin.id)
        val pending = CompletableDeferred<IncidentAssignment>()
        repository.assignPending = pending
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openAssignmentPicker()
        runCurrent()
        model.selectCandidate(admin.id)
        model.confirmAssignment()
        runCurrent()

        assertEquals(listOf(incident.id to admin.id), repository.assignments)
        assertTrue(model.state.value.assigning)
        assertTrue(model.state.value.pickerVisible)
        // Not confirmed until the backend answers.
        assertNull(model.state.value.incident?.activeAssignment)
        assertFalse(model.state.value.assignmentCompleted)

        pending.complete(confirmed)
        runCurrent()
        assertFalse(model.state.value.assigning)
        assertFalse(model.state.value.pickerVisible)
        assertTrue(model.state.value.assignmentCompleted)
        assertEquals(confirmed, model.state.value.incident?.activeAssignment)
        assertNull(model.state.value.assignmentError)
    }

    @Test fun `failed assignment keeps the previous responsible and reports a retryable error`() = runTest {
        val repository = Repository().apply {
            candidatePage = page(listOf(admin))
            assignFailure = http(503)
            row = assignedIncident
        }
        val model = AdminIncidentDetailViewModel(repository, assignedIncident.id)
        runCurrent()
        model.openAssignmentPicker()
        runCurrent()
        model.selectCandidate(admin.id)
        model.confirmAssignment()
        runCurrent()

        assertFalse(model.state.value.assigning)
        assertTrue(model.state.value.pickerVisible)
        assertTrue(model.state.value.assignmentCompleted.not())
        assertEquals(assignment, model.state.value.incident?.activeAssignment)
        val failure = model.state.value.assignmentError
        assertNotNull(failure)
        assertTrue(failure!!.canRetry)
        assertFalse(failure.message.contains("private host"))

        repository.assignFailure = null
        model.confirmAssignment()
        runCurrent()
        assertEquals(2, repository.assignments.size)
        assertEquals(assignment.copy(assignedUserId = admin.id), model.state.value.incident?.activeAssignment)
    }

    @Test fun `candidate catalog failure is controlled and retryable`() = runTest {
        val repository = Repository().apply {
            candidateFailure = ApiException.Network(IOException("private host"))
        }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        val failed = model.state.value.candidates
        assertTrue(failed is AssignmentCandidates.Failed)
        assertTrue((failed as AssignmentCandidates.Failed).canRetry)
        assertFalse(failed.message.contains("private host"))
        assertNull(model.state.value.responsibleName)

        model.openAssignmentPicker()
        assertTrue(model.state.value.pickerVisible)

        repository.candidateFailure = null
        repository.candidatePage = page(listOf(admin))
        model.retryCandidates()
        runCurrent()
        assertEquals(AssignmentCandidates.Ready(listOf(admin), 1), model.state.value.candidates)
        assertNull(model.state.value.assignmentError)
        assertEquals(IncidentsUiState.Success, model.state.value.status)
    }

    @Test fun `empty administrator catalog is valid and blocks confirmation`() = runTest {
        val repository = Repository()
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.openAssignmentPicker()
        runCurrent()
        assertEquals(AssignmentCandidates.Ready(emptyList(), 0), model.state.value.candidates)
        assertTrue(model.state.value.pickerVisible)
        assertNull(model.state.value.selectedCandidateId)

        model.confirmAssignment()
        runCurrent()
        assertTrue(repository.assignments.isEmpty())
        assertFalse(model.state.value.assigning)
    }

    @Test fun `session change during confirmation is terminal and never reported as confirmed`() = runTest {
        val repository = Repository().apply {
            candidatePage = page(listOf(admin))
            assignFailure = SessionRequiredException()
            row = assignedIncident
        }
        val model = AdminIncidentDetailViewModel(repository, assignedIncident.id)
        runCurrent()
        model.openAssignmentPicker()
        runCurrent()
        model.selectCandidate(admin.id)
        model.confirmAssignment()
        runCurrent()

        assertEquals(assignment, model.state.value.incident?.activeAssignment)
        assertFalse(model.state.value.assignmentCompleted)
        val failure = model.state.value.assignmentError
        assertNotNull(failure)
        assertFalse(failure!!.canRetry)
    }

    private class Repository : IncidentRepository {
        val ids = mutableListOf<String>()
        var failure: Exception? = null
        var pending: CompletableDeferred<Incident>? = null
        var cancelled = false
        var row: Incident = incident
        var candidatePage: Paginated<CandidateUser> = Paginated(emptyList(), 1, 100, 0, 0)
        var candidateFailure: Exception? = null
        var candidateCalls = 0
        var assignFailure: Exception? = null
        var assignPending: CompletableDeferred<IncidentAssignment>? = null
        val assignments = mutableListOf<Pair<String, String>>()
        private var assigned: IncidentAssignment? = null

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

        override suspend fun assignResponsible(incidentId: String, assignedUserId: String): IncidentAssignment {
            assignments += incidentId to assignedUserId
            assignFailure?.let { throw it }
            return assignPending?.await() ?: assignment.copy(assignedUserId = assignedUserId).also { assigned = it }
        }

        override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident = error("Unused")

        override suspend fun getCandidates(page: Int?, limit: Int?): Paginated<CandidateUser> {
            candidateCalls += 1
            candidateFailure?.let { throw it }
            return candidatePage
        }
    }

    private fun http(code: Int) = ApiException.Http(code, listOf("private host"), null, null)

    private fun page(users: List<CandidateUser>) = Paginated(users, 1, 100, users.size, 1)

    private companion object {
        val admin = CandidateUser(
            "c0000000-0000-4000-8000-000000000001",
            "Admin Uno",
            "admin1@uct.cl",
            listOf("ADMIN")
        )
        val otherAdmin = CandidateUser(
            "c0000000-0000-4000-8000-000000000002",
            "Admin Dos",
            "admin2@uct.cl",
            listOf("ADMIN")
        )
        val incident = Incident(
            "d0000000-0000-4000-8000-000000000004", "Luminaria", "Sin iluminación",
            Category("category", "Alumbrado", "", 1, false, null),
            IncidentStatus("status", "En revisión", 2),
            IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
            "another-citizen", null, "2026-11-02T12:00:00Z", "2026-11-03T12:00:00Z"
        )
        val assignment = IncidentAssignment(
            "a0000000-0000-4000-8000-000000000001",
            incident.id,
            admin.id,
            "a0000000-0000-4000-8000-0000000000aa",
            "2026-11-03T10:00:00Z",
            true
        )
        val assignedIncident = incident.copy(activeAssignment = assignment)
    }
}
