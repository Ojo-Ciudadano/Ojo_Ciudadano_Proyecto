package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateReportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads categories returned by the repository`() = runTest {
        val category = sampleCategory
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(categories = listOf(category))
        )

        assertEquals(listOf(category), viewModel.categoryUiState.categories)
        assertFalse(viewModel.categoryUiState.isLoading)
        assertNull(viewModel.categoryUiState.errorMessage)
    }

    @Test
    fun `keeps an empty category list without an error`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(categories = emptyList())
        )

        assertTrue(viewModel.categoryUiState.categories.isEmpty())
        assertFalse(viewModel.categoryUiState.isLoading)
        assertNull(viewModel.categoryUiState.errorMessage)
    }

    @Test
    fun `exposes an error when category loading fails`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(
                failure = IllegalStateException("Error de red")
            )
        )

        assertTrue(viewModel.categoryUiState.categories.isEmpty())
        assertFalse(viewModel.categoryUiState.isLoading)
        assertEquals("Error de red", viewModel.categoryUiState.errorMessage)
    }

    @Test
    fun `stores form events in view model state`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        assertEquals("Poste dañado", viewModel.formState.title)
        assertEquals("El poste bloquea la vereda", viewModel.formState.description)
        assertEquals(sampleCategory, viewModel.formState.selectedCategory)
    }

    @Test
    fun `does not submit when form validation fails`() = runTest {
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.createReport()

        assertEquals("El título es obligatorio", viewModel.formState.titleError)
        assertEquals("La descripción es obligatoria", viewModel.formState.descriptionError)
        assertEquals("La categoría es obligatoria", viewModel.formState.categoryError)
        assertEquals(CreateReportSubmissionState.Idle, viewModel.submissionState)
        assertTrue(incidentRepository.createInputs.isEmpty())
    }

    @Test
    fun `submits valid form through repository with current session user`() = runTest {
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            sessionStorage = FakeSessionStorage(sampleSession)
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        viewModel.createReport()

        assertEquals(CreateReportSubmissionState.Success, viewModel.submissionState)
        assertEquals(1, incidentRepository.createInputs.size)

        val input = incidentRepository.createInputs.single()
        assertEquals("Poste dañado", input.title)
        assertEquals("El poste bloquea la vereda", input.description)
        assertEquals(sampleCategory.id, input.categoryId)
        assertEquals(sampleSession.userId, input.reporterUserId)
        assertEquals(-38.7359, input.location.latitude, 0.0)
        assertEquals(-72.5904, input.location.longitude, 0.0)
        assertEquals("Ubicación de prueba Sprint 1", input.location.referenceAddress)
        assertEquals("Temuco", input.location.commune)
        assertEquals("La Araucanía", input.location.region)
        assertEquals("Centro", input.location.sector)
    }

    @Test
    fun `exposes error when incident creation fails`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = FakeIncidentRepository(
                failure = IllegalStateException("Error de red")
            ),
            sessionStorage = FakeSessionStorage(sampleSession)
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.Error("Error de red"),
            viewModel.submissionState
        )
    }

    @Test
    fun `prevents duplicate submissions while creation is loading`() = runTest {
        val creationStarted = CompletableDeferred<Unit>()
        val releaseCreation = CompletableDeferred<Unit>()
        val incidentRepository = FakeIncidentRepository(
            onCreate = {
                creationStarted.complete(Unit)
                releaseCreation.await()
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            sessionStorage = FakeSessionStorage(sampleSession)
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        viewModel.createReport()
        creationStarted.await()
        viewModel.createReport()

        assertEquals(CreateReportSubmissionState.Loading, viewModel.submissionState)
        assertEquals(1, incidentRepository.createInputs.size)

        releaseCreation.complete(Unit)

        assertEquals(CreateReportSubmissionState.Success, viewModel.submissionState)
    }
}

private fun createViewModel(
    categoryRepository: CategoryRepository,
    incidentRepository: IncidentRepository = FakeIncidentRepository(),
    sessionStorage: SessionStorage = FakeSessionStorage()
): CreateReportViewModel = CreateReportViewModel(
    categoryRepository = categoryRepository,
    incidentRepository = incidentRepository,
    sessionStorage = sessionStorage
)

private class FakeCategoryRepository(
    private val categories: List<Category> = emptyList(),
    private val failure: Throwable? = null
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> {
        failure?.let { throw it }
        return categories
    }
}

private class FakeIncidentRepository(
    private val failure: Throwable? = null,
    private val onCreate: suspend () -> Unit = {}
) : IncidentRepository {

    val createInputs = mutableListOf<CreateIncidentInput>()

    override suspend fun getIncidents(): List<Incident> = emptyList()

    override suspend fun getIncidentsByReporter(reporterUserId: String): List<Incident> = emptyList()

    override suspend fun getIncidentById(id: String): Incident = error("Not used by this test")

    override suspend fun createIncident(input: CreateIncidentInput): Incident {
        createInputs += input
        failure?.let { throw it }
        onCreate()
        return sampleIncident(input)
    }
}

private class FakeSessionStorage(
    private val session: AuthenticatedSession? = null
) : SessionStorage {

    override fun currentToken(): String? = null

    override fun currentSession(): AuthenticatedSession? = session

    override fun save(session: AuthenticatedSession) = Unit

    override fun clear() = Unit
}

private fun sampleIncident(input: CreateIncidentInput) = Incident(
    id = "incident-id",
    title = input.title,
    description = input.description,
    category = sampleCategory,
    status = IncidentStatus(id = "status-id", name = "Nuevo", order = 1),
    location = IncidentLocation(
        id = "location-id",
        latitude = input.location.latitude,
        longitude = input.location.longitude,
        referenceAddress = input.location.referenceAddress,
        commune = input.location.commune,
        region = input.location.region,
        sector = input.location.sector
    ),
    reporterUserId = input.reporterUserId,
    priorityLevel = null,
    createdAt = "2026-09-30T00:00:00.000Z",
    updatedAt = "2026-09-30T00:00:00.000Z"
)

private val sampleSession = AuthenticatedSession(
    accessToken = "test-token",
    tokenType = "Bearer",
    userId = "test-user-id",
    email = "usuario@example.invalid",
    name = "Usuario de prueba",
    roles = listOf("CIUDADANO")
)

private val sampleCategory = Category(
    id = "real-category-id",
    name = "Categoría real",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)
