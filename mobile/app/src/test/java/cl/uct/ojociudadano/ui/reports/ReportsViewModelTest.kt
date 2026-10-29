package cl.uct.ojociudadano.ui.reports

import cl.uct.ojociudadano.ui.home.HomeViewModel
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `my reports uses current user query and preserves filters`() = runTest {
        val repository = FakeIncidents()
        val viewModel = viewModel(repository)

        assertEquals(1, repository.userQueryCount)
        assertEquals(listOf(incident), viewModel.incidents.value)
        assertEquals(IncidentsUiState.Success, viewModel.uiState.value)
        viewModel.filterIncidents(category, DateFilter.RECENT)
        assertEquals(listOf(incident), viewModel.incidents.value)
    }

    @Test
    fun `session failure clears previously loaded incidents and filter source`() = runTest {
        val repository = FakeIncidents()
        val viewModel = viewModel(repository)
        repository.failure = SessionRequiredException()

        viewModel.loadIncidents()

        assertTrue(viewModel.incidents.value.isEmpty())
        assertEquals(IncidentsUiState.Error(repository.failure!!.incidentMessage()), viewModel.uiState.value)
        viewModel.filterIncidents(null, DateFilter.RECENT)
        assertTrue(viewModel.incidents.value.isEmpty())
    }

    @Test
    fun `no incidents for current user produces empty state`() = runTest {
        val viewModel = viewModel(FakeIncidents(emptyList()))
        assertEquals(IncidentsUiState.Empty, viewModel.uiState.value)
        assertTrue(viewModel.incidents.value.isEmpty())
    }

    @Test fun `all and mine switch endpoints and never retain other scope rows`() = runTest {
        val repository = FakeIncidents()
        val model = viewModel(repository)
        model.selectScope(IncidentScope.ALL)
        assertEquals(1, repository.allQueryCount)
        assertEquals(2, model.incidents.value.size)
        model.selectScope(IncidentScope.MINE)
        assertEquals(listOf(incident), model.incidents.value)
        repository.failure = ApiException.Http(403, emptyList(), null, null)
        model.selectScope(IncidentScope.ALL)
        assertTrue(model.incidents.value.isEmpty())
        assertTrue(model.uiState.value is IncidentsUiState.Error)
    }

    @Test fun `changing scope cancels pending load and removes previous rows while loading`() = runTest {
        val repository = FakeIncidents()
        val model = viewModel(repository)
        val pending = kotlinx.coroutines.CompletableDeferred<List<Incident>>()
        repository.pendingAll = pending
        model.selectScope(IncidentScope.ALL)
        assertEquals(IncidentsUiState.Loading, model.uiState.value)
        assertTrue(model.incidents.value.isEmpty())
        model.selectScope(IncidentScope.MINE)
        pending.complete(listOf(incident.copy(reporterUserId = "other-user")))
        assertEquals(listOf(incident), model.incidents.value)
        assertEquals(IncidentsUiState.Success, model.uiState.value)
    }

    @Test fun `network and server failures never become empty results on home or lists`() = runTest {
        val failures = listOf(
            ApiException.Network(java.io.IOException()),
            ApiException.Http(401, emptyList(), null, null),
            ApiException.Http(403, emptyList(), null, null),
            ApiException.Http(500, listOf("private database host"), null, null),
            ApiException.Http(503, emptyList(), null, null),
            ApiException.Unexpected(IllegalStateException("invalid JSON")),
            SessionRequiredException()
        )
        failures.forEach { failure ->
            val repository = FakeIncidents().apply { this.failure = failure }
            val model = viewModel(repository)
            val home = HomeViewModel(repository)
            assertEquals(IncidentsUiState.Error(failure.incidentMessage()), model.uiState.value)
            assertEquals(IncidentsUiState.Error(failure.incidentMessage()), home.uiState.value)
            assertTrue(home.incidents.value.isEmpty())
            assertFalse(failure.incidentMessage().contains("private database host"))
        }
    }

    @Test fun `home only queries own incidents and distinguishes successful empty response`() = runTest {
        val repository = FakeIncidents(emptyList())
        val home = HomeViewModel(repository)
        assertEquals(1, repository.userQueryCount)
        assertEquals(0, repository.allQueryCount)
        assertEquals(IncidentsUiState.Empty, home.uiState.value)
    }

    @Test fun `detail loads selected incident and clears it on failure`() = runTest {
        val repository = FakeIncidents()
        val detail = IncidentDetailViewModel(repository, "selected-id")
        assertEquals("selected-id", detail.state.value.incident?.id)
        repository.failure = ApiException.Http(404, emptyList(), null, null)
        detail.load()
        assertNull(detail.state.value.incident)
        assertTrue(detail.state.value.status is IncidentsUiState.Error)
    }

    @Test fun `invalid date cannot crash filtering and filters survive reload`() = runTest {
        val model = viewModel(FakeIncidents(listOf(incident.copy(createdAt = "invalid"))))
        model.filterIncidents(category, DateFilter.TODAY)
        assertTrue(model.incidents.value.isEmpty())
        model.loadIncidents()
        assertTrue(model.incidents.value.isEmpty())
        model.filterIncidents(null, DateFilter.RECENT)
        assertEquals(1, model.incidents.value.size)
    }

    private fun viewModel(repository: IncidentRepository) = ReportsViewModel(
        repository,
        object : CategoryRepository {
            override suspend fun getCategories() = listOf(category)
        }
    )

    private class FakeIncidents(val incidents: List<Incident> = listOf(incident)) : IncidentRepository {
        override suspend fun uploadEvidence(incidenteId: String, file: java.io.File, mimeType: String): cl.uct.ojociudadano.domain.model.Evidence = error("Unused")
        override suspend fun getEvidencesByIncident(incidenteId: String): List<cl.uct.ojociudadano.domain.model.Evidence> = error("Unused")
        override suspend fun getEvidenceById(id: String): cl.uct.ojociudadano.domain.model.Evidence = error("Unused")
        override suspend fun downloadEvidenceFile(id: String): okhttp3.ResponseBody = error("Unused")
        var userQueryCount = 0
        var failure: Exception? = null
        override suspend fun getCurrentUserIncidents(): List<Incident> {
            userQueryCount++
            failure?.let { throw it }
            return incidents
        }
        var allQueryCount = 0
        var pendingAll: kotlinx.coroutines.CompletableDeferred<List<Incident>>? = null
        override suspend fun getIncidents(): List<Incident> {
            allQueryCount++
            pendingAll?.let { return it.await() }
            failure?.let { throw it }
            return incidents + incident.copy(id = "other", reporterUserId = "other-user")
        }
        override suspend fun getIncidentById(id: String): Incident {
            failure?.let { throw it }
            return incident.copy(id = id)
        }
        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")
    }

    private companion object {
        val category = Category("category", "Alumbrado", "", 1, false, null)
        val incident = Incident(
            id = "incident", title = "Poste", description = "Poste caído", category = category,
            status = IncidentStatus("status", "Nuevo", 1),
            location = IncidentLocation("location", -38.7, -72.5, "Plaza", "Temuco", "Araucanía", "Centro"),
            reporterUserId = "a0000000-0000-4000-8000-000000000001", priorityLevel = null,
            createdAt = "2026-10-26T00:00:00Z", updatedAt = "2026-10-26T00:00:00Z"
        )
    }
}
