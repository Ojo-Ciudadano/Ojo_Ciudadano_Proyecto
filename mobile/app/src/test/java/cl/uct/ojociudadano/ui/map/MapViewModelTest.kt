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

        val state = model.uiState.value
        assertTrue(state is MapUiState.Success)
        val success = state as MapUiState.Success
        assertEquals(listOf("valid"), success.markers.map { it.incidentId })
        assertEquals("Poste caído", success.markers.single().summary.title)
        assertEquals("Alumbrado", success.markers.single().summary.categoryName)
        assertEquals("Nuevo", success.markers.single().summary.statusName)
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
            repository.rows.toMapMarkers(),
            (state as MapUiState.Success).markers
        )
        assertEquals(2, repository.allQueryCount)
    }

    @Test
    fun `selecting a marker exposes its id and summary without new queries`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(incident("first"), incident("second"))
        )
        val model = MapViewModel(repository)

        model.selectIncident("second")

        val success = model.uiState.value as MapUiState.Success
        assertEquals("second", success.selectedIncidentId)
        assertEquals("second", success.selectedMarker?.incidentId)
        assertEquals("Poste caído", success.selectedMarker?.summary?.title)
        assertEquals("Alumbrado", success.selectedMarker?.summary?.categoryName)
        assertEquals("Nuevo", success.selectedMarker?.summary?.statusName)
        assertEquals(1, repository.allQueryCount)
        assertEquals(0, repository.userQueryCount)
    }

    @Test
    fun `selecting a different marker updates the summary`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("first"),
                incident("second").copy(title = "Semáforo sin luz")
            )
        )
        val model = MapViewModel(repository)

        model.selectIncident("first")
        model.selectIncident("second")

        val success = model.uiState.value as MapUiState.Success
        assertEquals("second", success.selectedIncidentId)
        assertEquals("Semáforo sin luz", success.selectedMarker?.summary?.title)
        assertEquals(1, repository.allQueryCount)
    }

    @Test
    fun `selecting an unknown marker keeps the current state`() = runTest {
        val repository = FakeIncidents(rows = listOf(incident("first")))
        val model = MapViewModel(repository)

        model.selectIncident("missing")

        val success = model.uiState.value as MapUiState.Success
        assertEquals(null, success.selectedIncidentId)
        assertEquals(null, success.selectedMarker)
    }

    @Test
    fun `clearing the selection hides the summary`() = runTest {
        val model = MapViewModel(FakeIncidents(rows = listOf(incident("first"))))
        model.selectIncident("first")

        model.clearSelection()

        val success = model.uiState.value as MapUiState.Success
        assertEquals(null, success.selectedIncidentId)
        assertEquals(null, success.selectedMarker)
    }

    @Test
    fun `reload keeps the selection while the incident remains on the map`() = runTest {
        val repository = FakeIncidents(rows = listOf(incident("first"), incident("second")))
        val model = MapViewModel(repository)
        model.selectIncident("second")

        repository.rows = listOf(incident("first"), incident("second"))
        model.loadIncidents()

        val success = model.uiState.value as MapUiState.Success
        assertEquals("second", success.selectedIncidentId)
        assertEquals(2, repository.allQueryCount)
    }

    @Test
    fun `reload clears the selection when the incident is no longer represented`() = runTest {
        val repository = FakeIncidents(rows = listOf(incident("first"), incident("second")))
        val model = MapViewModel(repository)
        model.selectIncident("second")

        repository.rows = listOf(incident("first"))
        model.loadIncidents()

        val success = model.uiState.value as MapUiState.Success
        assertEquals(null, success.selectedIncidentId)
        assertEquals(null, success.selectedMarker)
    }

    @Test
    fun `category filter updates visible markers without new queries`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("lighting"),
                incident("waste").copy(
                    category = Category("waste", "Aseo", "", 1, false, null)
                )
            )
        )
        val model = MapViewModel(repository)

        model.selectCategory("waste")

        val success = model.uiState.value as MapUiState.Success
        assertEquals(MapIncidentFilters(categoryId = "waste"), success.filters)
        assertEquals(listOf("waste"), success.visibleMarkers.map { it.incidentId })
        assertEquals(listOf("lighting", "waste"), success.markers.map { it.incidentId })
        assertEquals(1, repository.allQueryCount)
    }

    @Test
    fun `status filter updates visible markers without new queries`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("open"),
                incident("resolved").copy(
                    status = IncidentStatus("resolved", "Resuelto", 2)
                )
            )
        )
        val model = MapViewModel(repository)

        model.selectStatus("resolved")

        val success = model.uiState.value as MapUiState.Success
        assertEquals(MapIncidentFilters(statusId = "resolved"), success.filters)
        assertEquals(listOf("resolved"), success.visibleMarkers.map { it.incidentId })
        assertEquals(1, repository.allQueryCount)
    }

    @Test
    fun `combined filters are applied and can be reset`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("lighting"),
                incident("waste-new").copy(
                    category = Category("waste", "Aseo", "", 1, false, null)
                ),
                incident("waste-resolved").copy(
                    category = Category("waste", "Aseo", "", 1, false, null),
                    status = IncidentStatus("resolved", "Resuelto", 2)
                )
            )
        )
        val model = MapViewModel(repository)

        model.selectCategory("waste")
        model.selectStatus("resolved")

        var success = model.uiState.value as MapUiState.Success
        assertEquals(listOf("waste-resolved"), success.visibleMarkers.map { it.incidentId })
        assertEquals(1, repository.allQueryCount)

        model.clearFilters()

        success = model.uiState.value as MapUiState.Success
        assertEquals(MapIncidentFilters(), success.filters)
        assertEquals(3, success.visibleMarkers.size)
        assertEquals(1, repository.allQueryCount)
    }

    @Test
    fun `no matching incidents keeps a controlled filtered empty state`() = runTest {
        val model = MapViewModel(
            FakeIncidents(
                rows = listOf(
                    incident("lighting").copy(
                        status = IncidentStatus("new", "Nuevo", 1)
                    ),
                    incident("waste").copy(
                        category = Category("waste", "Aseo", "", 1, false, null),
                        status = IncidentStatus("resolved", "Resuelto", 2)
                    )
                )
            )
        )

        model.selectCategory("waste")
        model.selectStatus("new")

        val success = model.uiState.value as MapUiState.Success
        assertTrue(success.isFilteredEmpty)
        assertTrue(success.visibleMarkers.isEmpty())
        assertEquals(2, success.markers.size)

        model.clearFilters()

        val restored = model.uiState.value as MapUiState.Success
        assertEquals(listOf("lighting", "waste"), restored.visibleMarkers.map { it.incidentId })
        assertTrue(!restored.isFilteredEmpty)
    }

    @Test
    fun `filters survive a reload while the options remain available`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("lighting"),
                incident("waste").copy(
                    category = Category("waste", "Aseo", "", 1, false, null)
                )
            )
        )
        val model = MapViewModel(repository)
        model.selectCategory("waste")

        model.loadIncidents()

        val success = model.uiState.value as MapUiState.Success
        assertEquals(MapIncidentFilters(categoryId = "waste"), success.filters)
        assertEquals(listOf("waste"), success.visibleMarkers.map { it.incidentId })
        assertEquals(2, repository.allQueryCount)
    }

    @Test
    fun `reload drops a filter whose category is no longer available`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("lighting"),
                incident("waste").copy(
                    category = Category("waste", "Aseo", "", 1, false, null)
                )
            )
        )
        val model = MapViewModel(repository)
        model.selectCategory("waste")

        repository.rows = listOf(incident("lighting"))
        model.loadIncidents()

        val success = model.uiState.value as MapUiState.Success
        assertEquals(MapIncidentFilters(), success.filters)
        assertEquals(listOf("lighting"), success.visibleMarkers.map { it.incidentId })
    }

    @Test
    fun `filtering out the selected marker clears its selection`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("lighting").copy(
                    category = Category("lighting", "Alumbrado", "", 1, false, null)
                ),
                incident("waste").copy(
                    category = Category("waste", "Aseo", "", 1, false, null)
                )
            )
        )
        val model = MapViewModel(repository)
        model.selectIncident("waste")

        model.selectCategory("lighting")

        val success = model.uiState.value as MapUiState.Success
        assertEquals(null, success.selectedIncidentId)
        assertEquals(null, success.selectedMarker)
    }

    @Test
    fun `hidden markers cannot be selected while a filter is active`() = runTest {
        val repository = FakeIncidents(
            rows = listOf(
                incident("lighting").copy(
                    category = Category("lighting", "Alumbrado", "", 1, false, null)
                ),
                incident("waste").copy(
                    category = Category("waste", "Aseo", "", 1, false, null)
                )
            )
        )
        val model = MapViewModel(repository)
        model.selectCategory("lighting")

        model.selectIncident("waste")

        val success = model.uiState.value as MapUiState.Success
        assertEquals(null, success.selectedIncidentId)
    }

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
