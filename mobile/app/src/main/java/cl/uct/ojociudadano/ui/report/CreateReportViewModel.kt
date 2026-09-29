package cl.uct.ojociudadano.ui.report

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateReportViewModel(
    private val categoryRepository: CategoryRepository,
    private val formValidator: CreateReportFormValidator = CreateReportFormValidator()
) : ViewModel() {

    var formState by mutableStateOf(CreateReportFormState())
        private set

    var categoryUiState by mutableStateOf(CreateReportCategoryUiState())
        private set

    var submissionState by mutableStateOf<CreateReportSubmissionState>(
        CreateReportSubmissionState.Idle
    )
        private set

    init {
        loadCategories()
    }

    fun onTitleChange(title: String) {
        formState = formState.updateTitle(title)
        submissionState = CreateReportSubmissionState.Idle
    }

    fun onDescriptionChange(description: String) {
        formState = formState.updateDescription(description)
        submissionState = CreateReportSubmissionState.Idle
    }

    fun onCategorySelected(category: Category) {
        formState = formState.updateCategory(category)
        submissionState = CreateReportSubmissionState.Idle
    }

    /**
     * Ejecuta únicamente la validación local. No inicia un envío remoto.
     */
    fun onCreateReportClick() {
        formState = formValidator.validate(formState)
        submissionState = CreateReportSubmissionState.Idle
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
                runCatching {
                    Log.e(
                        "CreateReportViewModel",
                        "Error al cargar categorías: type=${exception::class.java.name}, " +
                            "message=${exception.message}, cause=${exception.cause}",
                        exception
                    )
                }
                categoryUiState = categoryUiState.copy(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "No fue posible cargar las categorías"
                )
            }
        }
    }
}
