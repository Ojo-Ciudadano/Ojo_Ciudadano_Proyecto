package cl.uct.ojociudadano.ui.reports

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
        assertEquals(IncidentsUiState.Error(repository.failure!!.message!!), viewModel.uiState.value)
        viewModel.filterIncidents(null, DateFilter.RECENT)
        assertTrue(viewModel.incidents.value.isEmpty())
    }

    @Test
    fun `no incidents for current user produces empty state`() = runTest {
        val viewModel = viewModel(FakeIncidents(emptyList()))
        assertEquals(IncidentsUiState.Empty, viewModel.uiState.value)
        assertTrue(viewModel.incidents.value.isEmpty())
    }

    private fun viewModel(repository: IncidentRepository) = ReportsViewModel(
        repository,
        object : CategoryRepository {
            override suspend fun getCategories() = listOf(category)
        }
    )

    private class FakeIncidents(val incidents: List<Incident> = listOf(incident)) : IncidentRepository {
        var userQueryCount = 0
        var failure: Exception? = null
        override suspend fun getCurrentUserIncidents(): List<Incident> {
            userQueryCount++
            failure?.let { throw it }
            return incidents
        }
        override suspend fun getIncidents(): List<Incident> = error("Must not load all users' incidents")
        override suspend fun getIncidentById(id: String): Incident = error("Unused")
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
