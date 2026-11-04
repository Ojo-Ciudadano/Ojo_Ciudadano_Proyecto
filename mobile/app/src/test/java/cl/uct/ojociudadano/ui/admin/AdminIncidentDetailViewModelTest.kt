package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModelProvider
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

    @Test fun `matrix covers every source and destination including terminal and unknown statuses`() = runTest {
        val matrix = mapOf("Nuevo" to listOf("En revisión"), "En revisión" to listOf("En atención"),
            "En atención" to listOf("Resuelto"), "Resuelto" to listOf("En atención", "Cerrado"),
            "Cerrado" to emptyList(), "En revision" to emptyList(), "unknown" to emptyList())
        for ((source, allowed) in matrix) {
            val row = incident.copy(status = incident.status.copy(name = source))
            val repository = Repository().apply { result = row }
            val model = AdminIncidentDetailViewModel(repository, row.id)
            runCurrent()
            assertEquals(allowed, model.state.value.availableTransitions)
            for (destination in matrix.keys) {
                model.selectStatus(destination)
                assertEquals(if (destination in allowed) destination else null, model.state.value.selectedStatus)
                model.cancelChange()
            }
            assertTrue(repository.changes.isEmpty())
        }
    }

    @Test fun `blank observation blocks request and cancellation preserves confirmed incident`() = runTest {
        val repository = Repository()
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.selectStatus("En atención")
        model.selectStatus("Resuelto")
        assertEquals("En atención", model.state.value.selectedStatus)
        for (blank in listOf("", " ", "\n\t")) {
            model.editObservation(blank)
            model.confirmChange()
            runCurrent()
            assertNotNull(model.state.value.validationError)
            assertTrue(repository.changes.isEmpty())
        }
        model.cancelChange()
        model.confirmChange()
        runCurrent()
        assertNull(model.state.value.selectedStatus)
        assertEquals(incident, model.state.value.incident)
        assertTrue(repository.changes.isEmpty())
    }

    @Test fun `valid confirmation sends correct fields once and waits for backend before changing status`() = runTest {
        val confirmed = incident.copy(status = incident.status.copy(name = "En atención"))
        val repository = Repository().apply { updatePending = CompletableDeferred() }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.selectStatus("En atención")
        model.editObservation("  Inspección técnica  ")
        repeat(3) { model.confirmChange() }
        assertTrue(model.state.value.submitting)
        model.cancelChange()
        model.editObservation("ignored")
        runCurrent()
        assertEquals(listOf(Triple(incident.id, "En atención", "Inspección técnica")), repository.changes)
        assertEquals(incident, model.state.value.incident)
        assertEquals("  Inspección técnica  ", model.state.value.observation)
        repository.result = confirmed
        repository.updatePending?.complete(confirmed)
        runCurrent()
        assertEquals(confirmed, model.state.value.incident)
        assertEquals(listOf("Resuelto"), model.state.value.availableTransitions)
        assertFalse(model.state.value.busy)
        assertNull(model.state.value.selectedStatus)
        assertEquals("", model.state.value.observation)
        assertNotNull(model.state.value.successMessage)
        assertEquals(2, repository.ids.size)
    }

    @Test fun `rejected requests and connectivity failures retain last confirmed state and safe errors`() = runTest {
        for (failure in listOf(http(400), http(401), http(403), http(404), http(409), http(500), http(503),
            ApiException.Network(IOException("private host")), SocketTimeoutException(),
            ApiException.Unexpected(IllegalStateException("private host")), SessionRequiredException())) {
            val repository = Repository().apply { updateFailure = failure }
            val model = AdminIncidentDetailViewModel(repository, incident.id)
            runCurrent()
            model.selectStatus("En atención")
            model.editObservation("Verificado")
            model.confirmChange()
            runCurrent()
            assertEquals(incident, model.state.value.incident)
            assertFalse(model.state.value.busy)
            assertNull(model.state.value.successMessage)
            assertNotNull(model.state.value.changeError)
            assertFalse(model.state.value.changeError.orEmpty().contains("private host"))
            assertEquals("Verificado", model.state.value.observation)
            assertEquals(1, repository.ids.size)
            if (failure is SessionRequiredException || failure is ApiException.Http && failure.statusCode in setOf(401, 403, 404)) {
                assertTrue(model.state.value.availableTransitions.isEmpty())
                model.confirmChange()
                runCurrent()
                assertEquals(1, repository.changes.size)
            } else {
                repository.updateFailure = null
                model.confirmChange()
                runCurrent()
                assertEquals(2, repository.changes.size)
                assertNotNull(model.state.value.successMessage)
            }
        }
    }

    @Test fun `successful patch and failed refresh keep confirmed result and retry GET only`() = runTest {
        val confirmed = incident.copy(status = incident.status.copy(name = "Resuelto"))
        val repository = Repository()
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        runCurrent()
        model.selectStatus("En atención")
        model.editObservation("Observación")
        repository.updateResult = confirmed
        repository.failure = IOException("private host")
        model.confirmChange()
        runCurrent()
        assertEquals(confirmed, model.state.value.incident)
        assertNotNull(model.state.value.successMessage)
        assertNotNull(model.state.value.syncError)
        assertNull(model.state.value.changeError)
        assertTrue(model.state.value.availableTransitions.isEmpty())
        repository.failure = null
        repository.pending = CompletableDeferred()
        repeat(3) { model.retrySynchronization() }
        assertTrue(model.state.value.synchronizing)
        runCurrent()
        assertEquals(3, repository.ids.size)
        assertEquals(1, repository.changes.size)
        repository.pending?.complete(confirmed)
        runCurrent()
        assertNull(model.state.value.syncError)
        assertEquals(listOf("En atención", "Cerrado"), model.state.value.availableTransitions)
        assertFalse(model.state.value.busy)
    }

    @Test fun `leaving detail cancels pending change without publishing success or reloading`() = runTest {
        val repository = Repository().apply { updatePending = CompletableDeferred() }
        val model = AdminIncidentDetailViewModel(repository, incident.id)
        val store = ViewModelStore().apply { put("detail", model) }
        runCurrent()
        model.selectStatus("En atención")
        model.editObservation("Verificado")
        model.confirmChange()
        runCurrent()
        store.clear()
        runCurrent()
        repository.updatePending?.complete(incident.copy(status = incident.status.copy(name = "En atención")))
        runCurrent()
        assertEquals(incident, model.state.value.incident)
        assertNull(model.state.value.successMessage)
        assertEquals(1, repository.ids.size)
        assertEquals(1, repository.changes.size)
    }

    private class Repository : IncidentRepository {
        override suspend fun getIncidentHistory(incidentId: String): List<cl.uct.ojociudadano.data.model.IncidentStateHistory> = error("Unused")
        override suspend fun assignResponsible(incidentId: String, assignedUserId: String): cl.uct.ojociudadano.data.model.IncidentAssignment = error("Unused")
        override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident = error("Unused")
        override suspend fun getCandidates(page: Int?, limit: Int?): cl.uct.ojociudadano.data.model.Paginated<cl.uct.ojociudadano.data.model.CandidateUser> = error("Unused")
        val changes = mutableListOf<Triple<String, String, String>>()
        var updatePending: CompletableDeferred<Incident>? = null
        var updateFailure: Exception? = null
        var updateResult: Incident? = null
        var result: Incident = incident
        override suspend fun updateStatus(incidentId: String, status: String, observation: String): Incident {
            changes += Triple(incidentId, status, observation)
            updateFailure?.let { throw it }
            return updatePending?.await() ?: updateResult ?: result.copy(status = result.status.copy(name = status))
        }

        val ids = mutableListOf<String>()
        var failure: Exception? = null
        var pending: CompletableDeferred<Incident>? = null
        var cancelled = false
        override suspend fun getIncidentById(id: String): Incident {
            ids += id
            failure?.let { throw it }
            return try {
                pending?.await() ?: result
            } catch (exception: kotlin.coroutines.cancellation.CancellationException) {
                cancelled = true
                throw exception
            }
        }
        override suspend fun getIncidents(): List<Incident> = error("Must not query list")
        override suspend fun getCurrentUserIncidents(): List<Incident> = error("Must not query own incidents")
        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")
        override suspend fun uploadEvidence(incidenteId: String, file: File, mimeType: String): Evidence = error("Unused")
        override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> = error("Must not query photos")
        override suspend fun getEvidenceById(id: String): Evidence = error("Unused")
        override suspend fun downloadEvidenceFile(id: String): ResponseBody = error("Unused")
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
    }
}
