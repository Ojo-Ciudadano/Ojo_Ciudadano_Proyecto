package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
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
    fun `validates form through view model without starting a submission`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        viewModel.onCreateReportClick()

        assertEquals("El título es obligatorio", viewModel.formState.titleError)
        assertEquals("La descripción es obligatoria", viewModel.formState.descriptionError)
        assertEquals("La categoría es obligatoria", viewModel.formState.categoryError)
        assertEquals(CreateReportSubmissionState.Idle, viewModel.submissionState)
    }
}

private fun createViewModel(
    categoryRepository: CategoryRepository
): CreateReportViewModel = CreateReportViewModel(
    categoryRepository = categoryRepository,
    incidentRepository = FakeIncidentRepository(),
    sessionStorage = FakeSessionStorage()
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

private class FakeIncidentRepository : IncidentRepository {

    override suspend fun getIncidents(): List<Incident> = emptyList()

    override suspend fun getIncidentsByReporter(reporterUserId: String): List<Incident> = emptyList()

    override suspend fun getIncidentById(id: String): Incident = error("Not used by this test")

    override suspend fun createIncident(input: CreateIncidentInput): Incident =
        error("Not used by this test")
}

private class FakeSessionStorage : SessionStorage {

    override fun currentToken(): String? = null

    override fun currentSession(): AuthenticatedSession? = null

    override fun save(session: AuthenticatedSession) = Unit

    override fun clear() = Unit
}

private val sampleCategory = Category(
    id = "real-category-id",
    name = "Categoría real",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)
