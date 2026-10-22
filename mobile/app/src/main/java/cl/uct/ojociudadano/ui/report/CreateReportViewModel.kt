package cl.uct.ojociudadano.ui.report

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.data.model.Sprint1TemporaryLocation
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateReportViewModel(
    private val categoryRepository: CategoryRepository,
    private val incidentRepository: IncidentRepository,
    private val sessionStorage: SessionStorage,
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
     * Marca el inicio de una captura para bloquear el botón mientras la cámara
     * del sistema toma el control de la pantalla.
     */
    fun onPhotoCaptureStarted() {
        formState = formState.startPhotoCapture()
    }

    /**
     * Conserva la evidencia solo en el estado del formulario: no se persiste ni
     * se envía al backend en esta tarea.
     */
    fun onPhotoCaptured(evidence: IncidentPhotoEvidence) {
        formState = formState.updateEvidencePhoto(evidence)
    }

    /**
     * El usuario cerró la cámara sin capturar: no es un error y la fotografía
     * anterior, si existía, se mantiene disponible.
     */
    fun onPhotoCaptureCancelled() {
        formState = formState.cancelPhotoCapture()
    }

    fun onPhotoCaptureError(message: String) {
        formState = formState.failPhotoCapture(message)
    }

    fun createReport() {
        if (submissionState is CreateReportSubmissionState.Loading) return

        formState = formValidator.validate(formState)

        if (formState.hasValidationErrors()) {
            submissionState = CreateReportSubmissionState.Idle
            return
        }

        val reporterUserId = sessionStorage.currentSession()?.userId

        if (reporterUserId == null) {
            submissionState = CreateReportSubmissionState.Error(
                "No hay una sesión activa para crear el reporte"
            )
            return
        }

        val selectedCategory = requireNotNull(formState.selectedCategory)
        val input = CreateIncidentInput(
            title = formState.title,
            description = formState.description,
            categoryId = selectedCategory.id,
            reporterUserId = reporterUserId,
            location = Sprint1TemporaryLocation.incidentLocation
        )

        submissionState = CreateReportSubmissionState.Loading

        viewModelScope.launch {
            try {
                // Temporary diagnostic log: verifies the API UUID selected in the dropdown.
                runCatching {
                    Log.d(
                        "CreateReportViewModel",
                        "Creating incident: selectedCategory=$selectedCategory, " +
                            "categoryName=${selectedCategory.name}, " +
                            "categoriaId=${input.categoryId}"
                    )
                }
                incidentRepository.createIncident(input)
                submissionState = CreateReportSubmissionState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                runCatching {
                    Log.e(
                        "CreateReportViewModel",
                        "Error al crear reporte: type=${exception::class.java.name}, " +
                            "message=${exception.message}, cause=${exception.cause}",
                        exception
                    )
                }
                submissionState = CreateReportSubmissionState.Error(
                    exception.message ?: "No fue posible crear el reporte"
                )
            }
        }
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

    private fun CreateReportFormState.hasValidationErrors(): Boolean =
        titleError != null || descriptionError != null || categoryError != null

}
