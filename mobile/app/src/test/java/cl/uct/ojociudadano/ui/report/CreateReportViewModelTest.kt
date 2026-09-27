package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.repository.CategoryRepository
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
        val viewModel = CreateReportViewModel(
            categoryRepository = FakeCategoryRepository(categories = listOf(category))
        )

        assertEquals(listOf(category), viewModel.categoryUiState.categories)
        assertFalse(viewModel.categoryUiState.isLoading)
        assertNull(viewModel.categoryUiState.errorMessage)
    }

    @Test
    fun `keeps an empty category list without an error`() = runTest {
        val viewModel = CreateReportViewModel(
            categoryRepository = FakeCategoryRepository(categories = emptyList())
        )

        assertTrue(viewModel.categoryUiState.categories.isEmpty())
        assertFalse(viewModel.categoryUiState.isLoading)
        assertNull(viewModel.categoryUiState.errorMessage)
    }

    @Test
    fun `exposes an error when category loading fails`() = runTest {
        val viewModel = CreateReportViewModel(
            categoryRepository = FakeCategoryRepository(
                failure = IllegalStateException("Error de red")
            )
        )

        assertTrue(viewModel.categoryUiState.categories.isEmpty())
        assertFalse(viewModel.categoryUiState.isLoading)
        assertEquals("Error de red", viewModel.categoryUiState.errorMessage)
    }
}

private class FakeCategoryRepository(
    private val categories: List<Category> = emptyList(),
    private val failure: Throwable? = null
) : CategoryRepository {

    override suspend fun getCategories(): List<Category> {
        failure?.let { throw it }
        return categories
    }
}

private val sampleCategory = Category(
    id = "real-category-id",
    name = "Categoría real",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)
