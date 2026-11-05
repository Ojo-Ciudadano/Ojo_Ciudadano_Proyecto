package cl.uct.ojociudadano.ui.admin

import androidx.lifecycle.ViewModelStore
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CandidateUser
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentAssignment
import cl.uct.ojociudadano.data.model.IncidentFilters
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStateHistory
import cl.uct.ojociudadano.data.model.Paginated
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.CategoryRepository
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
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        assertEquals(IncidentsUiState.Loading, model.state.value.status)
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(repository.rows, model.state.value.incidents)
        assertEquals(listOf("real-id", "other-id"), model.state.value.incidents.map { it.id })
        assertEquals(1, repository.calls)
    }

    @Test fun `successful empty response is distinct from error`() = runTest {
        val repository = Repository().apply { rows = emptyList() }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()
        assertEquals(AdminIncidentsState(IncidentsUiState.Empty), model.state.value)
    }

    @Test fun `pending load ignores duplicate requests and retry recovers from network error`() = runTest {
        val pending = CompletableDeferred<List<Incident>>()
        val repository = Repository().apply { response = pending }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
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
            val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
            runCurrent()
            assertEquals(listOf(incident), model.state.value.incidents)
            repository.failure = failure
            model.loadIncidents()
            runCurrent()
            assertEquals(IncidentsUiState.Error(failure.incidentMessage()), model.state.value.status)
            assertTrue(model.state.value.incidents.isEmpty())
            assertFalse((model.state.value.status as IncidentsUiState.Error).message.contains("private host"))
        }
        assertEquals(failures.size, failures.map { it.incidentMessage() }.distinct().size)
    }

    @Test fun `disposing viewmodel cancels pending request without publishing error`() = runTest {
        val pending = CompletableDeferred<List<Incident>>()
        val repository = Repository().apply { response = pending }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        val store = ViewModelStore().apply { put("admin", model) }
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(repository.cancelled)
        pending.complete(listOf(incident))
        runCurrent()
        assertEquals(AdminIncidentsState(), model.state.value)
    }

    @Test fun `applying a filter forwards selected criteria to the repository`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()
        repository.requestedFilters.clear()
        model.applyFilters(IncidentFilters(estadoId = "status"))
        runCurrent()
        assertEquals(listOf(IncidentFilters(estadoId = "status")), repository.requestedFilters)
        assertEquals(IncidentFilters(estadoId = "status"), model.state.value.filters)
        assertEquals(IncidentsUiState.Success, model.state.value.status)
    }

    @Test fun `combining filters sends all selected criteria at once`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()
        repository.requestedFilters.clear()
        model.applyFilters(IncidentFilters(estadoId = "status", categoriaId = "category", comuna = "Temuco"))
        runCurrent()
        assertEquals(listOf(IncidentFilters("status", "category", "Temuco")), repository.requestedFilters)
    }

    @Test fun `selecting a single dimension preserves the other active filters`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()
        model.applyFilters(IncidentFilters(estadoId = "status"))
        runCurrent()
        repository.requestedFilters.clear()
        model.selectComuna("Temuco")
        runCurrent()
        assertEquals(listOf(IncidentFilters(estadoId = "status", comuna = "Temuco")), repository.requestedFilters)
    }

    @Test fun `clearFilters reloads without restrictions`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()
        model.applyFilters(IncidentFilters(estadoId = "status"))
        runCurrent()
        repository.requestedFilters.clear()
        model.clearFilters()
        runCurrent()
        assertEquals(listOf(IncidentFilters()), repository.requestedFilters)
        assertEquals(IncidentFilters(), model.state.value.filters)
    }

    @Test fun `filtered query with no matches maps to Empty and keeps options stable`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()
        repository.rows = emptyList()
        model.applyFilters(IncidentFilters(comuna = "Inexistente"))
        runCurrent()
        assertEquals(IncidentsUiState.Empty, model.state.value.status)
        assertTrue(model.state.value.incidents.isEmpty())
        assertEquals(listOf(IncidentStatus("status", "Nuevo", 1)), model.state.value.availableStates)
        assertEquals(listOf("Temuco"), model.state.value.availableCommunes)
    }

    @Test fun `rapid filter changes cancel the previous in-flight query`() = runTest {
        val first = CompletableDeferred<List<Incident>>()
        val second = CompletableDeferred<List<Incident>>()
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()

        repository.response = first
        model.applyFilters(IncidentFilters(estadoId = "status"))
        runCurrent()
        repository.response = second
        model.applyFilters(IncidentFilters(comuna = "Temuco"))
        runCurrent()

        first.complete(listOf(incident))
        runCurrent()
        assertEquals(IncidentsUiState.Loading, model.state.value.status)

        second.complete(listOf(incident.copy(id = "filtered")))
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(listOf("filtered"), model.state.value.incidents.map { it.id })
        assertEquals(IncidentFilters(comuna = "Temuco"), model.state.value.filters)
    }

    @Test fun `unfiltered load derives state and commune options`() = runTest {
        val resuelto = IncidentStatus("s2", "Resuelto", 5)
        val nuevo = IncidentStatus("s1", "Nuevo", 1)
        val a = incident.copy(status = resuelto, location = incident.location.copy(commune = "Villarrica"))
        val b = incident.copy(status = nuevo, location = incident.location.copy(commune = "Temuco"))
        val repository = Repository().apply { rows = listOf(a, b) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository(listOf(incident.category)))
        runCurrent()
        assertEquals(listOf("s1", "s2"), model.state.value.availableStates.map { it.id })
        assertEquals(listOf("Temuco", "Villarrica"), model.state.value.availableCommunes)
        assertEquals(listOf(incident.category), model.state.value.categories)
    }

    @Test fun `category catalog failure does not block incident listing`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository(failure = IOException("private host")))
        runCurrent()
        assertEquals(IncidentsUiState.Success, model.state.value.status)
        assertEquals(listOf(incident), model.state.value.incidents)
        assertTrue(model.state.value.categories.isEmpty())
        assertTrue(model.state.value.categoriesFailed)
    }

    @Test fun `filtering before the initial load completes still initializes selectors`() = runTest {
        val initial = CompletableDeferred<List<Incident>>()
        val repository = Repository().apply { response = initial }
        val model = AdminIncidentsViewModel(repository, FakeCategoryRepository())
        runCurrent()

        // The unfiltered request that populates the selectors is still pending…
        repository.response = null
        model.selectCategoria("category")
        runCurrent()

        // …yet the filter is applied and the selectors are rebuilt by the separate catalog job.
        assertEquals(IncidentFilters(categoriaId = "category"), model.state.value.filters)
        assertEquals(listOf(IncidentStatus("status", "Nuevo", 1)), model.state.value.availableStates)
        assertEquals(listOf("Temuco"), model.state.value.availableCommunes)
        assertTrue(repository.cancelled)

        // The recovered options make it possible to combine a second filter.
        repository.requestedFilters.clear()
        model.selectEstado("status")
        runCurrent()
        assertEquals(
            listOf(IncidentFilters(estadoId = "status", categoriaId = "category")),
            repository.requestedFilters
        )
    }

    @Test fun `category catalog failure can be retried without losing selected filters`() = runTest {
        val repository = Repository().apply { rows = listOf(incident) }
        val categories = FakeCategoryRepository(failure = IOException("private host"))
        val model = AdminIncidentsViewModel(repository, categories)
        runCurrent()
        assertTrue(model.state.value.categoriesFailed)
        assertTrue(model.state.value.categories.isEmpty())

        model.applyFilters(IncidentFilters(estadoId = "status"))
        runCurrent()

        categories.failure = null
        categories.rows = listOf(incident.category)
        model.loadCategories()
        runCurrent()

        assertEquals(2, categories.calls)
        assertFalse(model.state.value.categoriesFailed)
        assertEquals(listOf(incident.category), model.state.value.categories)
        assertEquals(IncidentFilters(estadoId = "status"), model.state.value.filters)
    }

    private class Repository : IncidentRepository {
        var calls = 0
        var rows = listOf(incident)
        var failure: Exception? = null
        var response: CompletableDeferred<List<Incident>>? = null
        var cancelled = false
        val requestedFilters = mutableListOf<IncidentFilters>()
        override suspend fun getIncidents(filters: IncidentFilters): List<Incident> {
            calls++
            requestedFilters += filters
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

        override suspend fun getIncidentHistory(incidentId: String): List<IncidentStateHistory> = error("Unused")

        override suspend fun assignResponsible(
            incidentId: String,
            assignedUserId: String
        ): IncidentAssignment = error("Unused")

        override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident = error("Unused")

        override suspend fun getCandidates(page: Int?, limit: Int?): Paginated<CandidateUser> = error("Unused")
    }

    private class FakeCategoryRepository(
        var rows: List<Category> = emptyList(),
        var failure: Exception? = null
    ) : CategoryRepository {
        var calls = 0
        override suspend fun getCategories(): List<Category> {
            calls++
            failure?.let { throw it }
            return rows
        }
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
