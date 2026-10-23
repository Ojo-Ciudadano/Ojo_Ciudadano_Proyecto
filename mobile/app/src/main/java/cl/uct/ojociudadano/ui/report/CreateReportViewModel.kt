package cl.uct.ojociudadano.ui.report

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PhotoImportException
import cl.uct.ojociudadano.data.media.PhotoImportFailure
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.data.model.Sprint1TemporaryLocation
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.ui.report.photo.PhotoSelectionErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateReportViewModel(
    private val categoryRepository: CategoryRepository,
    private val incidentRepository: IncidentRepository,
    private val sessionStorage: SessionStorage,
    private val photoFileStore: EvidencePhotoFileStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
     * Marca el inicio de una captura para bloquear las acciones de fotografía
     * mientras la cámara del sistema toma el control de la pantalla.
     */
    fun onPhotoCaptureStarted() {
        formState = formState.startPhotoOperation(PhotoOperation.CAMERA)
    }

    /**
     * Conserva la evidencia solo en el estado del formulario: no se persiste ni
     * se envía al backend en esta tarea.
     */
    fun onPhotoCaptured(evidence: IncidentPhotoEvidence) {
        replaceEvidencePhoto(evidence)
    }

    /**
     * El usuario cerró la cámara sin capturar: no es un error y la fotografía
     * anterior, si existía, se mantiene disponible.
     */
    fun onPhotoCaptureCancelled() {
        formState = formState.cancelPhotoOperation()
    }

    fun onPhotoCaptureError(message: String) {
        formState = formState.failPhotoOperation(message)
    }

    /**
     * Marca el inicio de la selección desde galería para bloquear las acciones
     * de fotografía mientras el selector del sistema toma el control.
     */
    fun onGallerySelectionStarted() {
        formState = formState.startPhotoOperation(PhotoOperation.GALLERY)
    }

    /**
     * Copia la fotografía elegida a la caché temporal fuera del hilo principal.
     *
     * La URI del selector tiene un permiso de lectura temporal; por eso no se
     * guarda como evidencia directamente, sino que se importa al almacén local
     * para que pueda reutilizarse mientras el formulario permanezca abierto.
     */
    fun onGalleryPhotoSelected(contentUri: String) {
        formState = formState.startPhotoOperation(PhotoOperation.GALLERY)

        viewModelScope.launch {
            try {
                val evidence = withContext(ioDispatcher) {
                    photoFileStore.import(contentUri)
                }
                replaceEvidencePhoto(evidence)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: PhotoImportException) {
                formState = formState.failPhotoOperation(exception.toUserMessage())
            } catch (exception: SecurityException) {
                formState = formState.failPhotoOperation(PhotoSelectionErrors.ACCESS_DENIED)
            } catch (exception: Exception) {
                formState = formState.failPhotoOperation(
                    PhotoSelectionErrors.RECOVER_PHOTO_FAILED
                )
            }
        }
    }

    /**
     * El usuario cerró el selector sin elegir una fotografía: no es un error y
     * la evidencia anterior, si existía, se mantiene disponible.
     */
    fun onGallerySelectionCancelled() {
        formState = formState.cancelPhotoOperation()
    }

    fun onGallerySelectionError(message: String) {
        formState = formState.failPhotoOperation(message)
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

    private fun replaceEvidencePhoto(evidence: IncidentPhotoEvidence) {
        formState.evidencePhoto
            ?.takeIf { previous -> previous.fileName != evidence.fileName }
            ?.let { previous -> photoFileStore.delete(previous.fileName) }

        formState = formState.updateEvidencePhoto(evidence)
    }

    private fun PhotoImportException.toUserMessage(): String = when (failure) {
        PhotoImportFailure.NOT_AN_IMAGE -> PhotoSelectionErrors.NOT_AN_IMAGE
        PhotoImportFailure.EMPTY_FILE -> PhotoSelectionErrors.EMPTY_FILE
        PhotoImportFailure.UNREADABLE -> PhotoSelectionErrors.ACCESS_DENIED
    }

    private fun CreateReportFormState.hasValidationErrors(): Boolean =
        titleError != null || descriptionError != null || categoryError != null

}
