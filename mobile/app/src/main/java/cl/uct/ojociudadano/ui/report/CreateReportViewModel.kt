package cl.uct.ojociudadano.ui.report

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateReportViewModel(
    private val categoryRepository: CategoryRepository,
    private val formValidator: CreateReportFormValidator = CreateReportFormValidator()
) : ViewModel() {

    var categoryUiState by mutableStateOf(CreateReportCategoryUiState())
        private set

    init {
        loadCategories()
    }

    fun validateForm(state: CreateReportFormState): CreateReportFormState {
        return formValidator.validate(state)
    }

    fun loadCategories() {
        viewModelScope.launch {
            categoryUiState = categoryUiState.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                categoryUiState = CreateReportCategoryUiState(
                    categories = categoryRepository.getCategories()
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                categoryUiState = categoryUiState.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "No fue posible cargar las categorías"
                )
            }
        }
    }
}
