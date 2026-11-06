package cl.uct.ojociudadano.ui.map

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
import cl.uct.ojociudadano.domain.model.Evidence
import cl.uct.ojociudadano.ui.reports.incidentMessage
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load represents only incidents with valid coordinates`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("valid"),
                incident("invalid").copy(
                    location = location(latitude = 120.0, longitude = 300.0)
                )
            )
        )

        val model = MapViewModel(repository)

        assertEquals(MapUiState.Success(listOf(marker("valid"))), model.uiState.value)
        assertEquals(1, repository.allQueryCount)
        assertEquals(0, repository.userQueryCount)
    }

    @Test
    fun `empty response produces empty state`() = runTest {
        val model = MapViewModel(FakeIncidents(rows = emptyList()))

        assertEquals(MapUiState.Empty, model.uiState.value)
    }

    @Test
    fun `response without valid coordinates produces empty state`() = runTest {
        val model = MapViewModel(
            FakeIncidents(
                rows = listOf(
                    incident("invalid").copy(
                        location = location(latitude = Double.NaN, longitude = 0.0)
                    )
                )
            )
        )

        assertEquals(MapUiState.Empty, model.uiState.value)
    }

    @Test
    fun `repository failure produces controlled error state`() = runTest {
        val failure = ApiException.Network(IOException("offline"))
        val model = MapViewModel(FakeIncidents(failure = failure))

        assertEquals(MapUiState.Error(failure.incidentMessage()), model.uiState.value)
    }

    @Test
    fun `reload replaces markers with the updated data set`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(incident("first"))
        )
        val model = MapViewModel(repository)

        repository.rows = listOf(
            incident("first").copy(
                location = location(latitude = 10.0, longitude = 20.0)
            ),
            incident("second")
        )
        model.loadIncidents()

        val state = model.uiState.value
        assertTrue(state is MapUiState.Success)
        assertEquals(
            listOf(
                IncidentMapMarker("first", 10.0, 20.0),
                marker("second")
            ),
            (state as MapUiState.Success).markers
        )
        assertEquals(2, repository.allQueryCount)
    }

    private fun marker(id: String) = IncidentMapMarker(id, -38.7359, -72.5904)

    private fun incident(id: String) = Incident(
        id = id,
        title = "Poste caído",
        description = "Poste caído en la vía pública",
        category = Category("category", "Alumbrado", "", 1, false, null),
        status = IncidentStatus("status", "Nuevo", 1),
        location = location(),
        reporterUserId = "a0000000-0000-4000-8000-000000000001",
        priorityLevel = null,
        createdAt = "2026-11-05T00:00:00Z",
        updatedAt = "2026-11-05T00:00:00Z"
    )

    private fun location(
        latitude: Double = -38.7359,
        longitude: Double = -72.5904
    ) = IncidentLocation(
        id = "location",
        latitude = latitude,
        longitude = longitude,
        referenceAddress = "Plaza",
        commune = "Temuco",
        region = "La Araucanía",
        sector = "Centro"
    )

    private class FakeIncidents(
        var rows: List<Incident> = emptyList(),
        var failure: Exception? = null
    ) : IncidentRepository {

        var allQueryCount = 0
        var userQueryCount = 0

        override suspend fun getIncidents(filters: IncidentFilters): List<Incident> {
            allQueryCount++
            failure?.let { throw it }
            return rows
        }

        override suspend fun getCurrentUserIncidents(): List<Incident> {
            userQueryCount++
            failure?.let { throw it }
            return rows
        }

        override suspend fun updateStatus(
            incidentId: String,
            status: String,
            observation: String
        ): Incident = error("Unused")

        override suspend fun getIncidentById(id: String): Incident = error("Unused")

        override suspend fun createIncident(input: CreateIncidentInput): Incident = error("Unused")

        override suspend fun uploadEvidence(
            incidenteId: String,
            file: File,
            mimeType: String
        ): Evidence = error("Unused")

        override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> =
            error("Unused")

        override suspend fun getEvidenceById(id: String): Evidence = error("Unused")

        override suspend fun downloadEvidenceFile(id: String): ResponseBody = error("Unused")

        override suspend fun getIncidentHistory(incidentId: String): List<IncidentStateHistory> =
            error("Unused")

        override suspend fun assignResponsible(
            incidentId: String,
            assignedUserId: String
        ): IncidentAssignment = error("Unused")

        override suspend fun updatePriority(incidentId: String, priorityLevel: Int): Incident =
            error("Unused")

        override suspend fun getCandidates(page: Int?, limit: Int?): Paginated<CandidateUser> =
            error("Unused")
    }
}
