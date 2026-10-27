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
import java.util.UUID

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

    /**
     * Serializa el inicio y la finalización de las operaciones fotográficas.
     *
     * Solo la operación dueña de [activePhotoToken] puede modificar la evidencia.
     * Una operación nueva invalida a la anterior incluso si esta última todavía
     * está copiando contenido desde el selector.
     */
    private val photoOperationLock = Any()
    private var activePhotoToken: PhotoOperationToken? = null

    init {
        loadCategories()
        cleanupExpiredPhotoFiles()
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
     * Marca el inicio de una captura y entrega el token que identificará sus
     * resultados. Una captura nueva invalida cualquier operación anterior.
     */
    fun onPhotoCaptureStarted(): PhotoOperationToken =
        beginPhotoOperation(PhotoOperation.CAMERA)

    /**
     * Agrega la evidencia capturada a la colección del formulario: no se
     * persiste ni se envía al backend en esta tarea.
     *
     * Si la captura ya fue reemplazada por otra operación, se descarta el
     * archivo para no dejar evidencias huérfanas ni incorporar un resultado
     * obsoleto a la colección.
     */
    fun onPhotoCaptured(
        token: PhotoOperationToken,
        evidence: IncidentPhotoEvidence
    ) {
        if (claimPhotoOperation(token)) {
            addEvidencePhoto(evidence)
        } else {
            discardEvidencePhoto(evidence)
        }
    }

    /**
     * El usuario cerró la cámara sin capturar: no es un error y la fotografía
     * anterior, si existía, se mantiene disponible.
     */
    fun onPhotoCaptureCancelled(token: PhotoOperationToken) {
        if (claimPhotoOperation(token)) {
            formState = formState.cancelPhotoOperation()
        }
    }

    fun onPhotoCaptureError(token: PhotoOperationToken, message: String) {
        if (claimPhotoOperation(token)) {
            formState = formState.failPhotoOperation(message)
        }
    }

    /**
     * Marca el inicio de la selección desde galería y entrega el token que
     * identificará su resultado.
     */
    fun onGallerySelectionStarted(): PhotoOperationToken =
        beginPhotoOperation(PhotoOperation.GALLERY)

    /**
     * Copia la fotografía elegida a la caché temporal fuera del hilo principal.
     *
     * La URI del selector tiene un permiso de lectura temporal; por eso no se
     * guarda como evidencia directamente, sino que se importa al almacén local
     * para que pueda reutilizarse mientras el formulario permanezca abierto.
     *
     * Al terminar se comprueba nuevamente el token: si mientras tanto se inició
     * otra operación, la evidencia importada se elimina y no se publica.
     */
    fun onGalleryPhotoSelected(
        token: PhotoOperationToken,
        contentUri: String
    ) {
        if (!isCurrentPhotoOperation(token)) return

        viewModelScope.launch {
            var importedEvidence: IncidentPhotoEvidence? = null

            try {
                val evidence = withContext(ioDispatcher) {
                    photoFileStore.import(contentUri)
                }
                importedEvidence = evidence

                if (claimPhotoOperation(token)) {
                    addEvidencePhoto(evidence)
                } else {
                    discardEvidencePhoto(evidence)
                }
            } catch (exception: CancellationException) {
                // Si la copia alcanzó a terminar antes de la cancelación, su
                // archivo tampoco debe quedar huérfano.
                importedEvidence?.let(::discardEvidencePhoto)
                throw exception
            } catch (exception: PhotoImportException) {
                failPhotoOperationIfCurrent(token, exception.toUserMessage())
            } catch (exception: SecurityException) {
                failPhotoOperationIfCurrent(token, PhotoSelectionErrors.ACCESS_DENIED)
            } catch (exception: Exception) {
                failPhotoOperationIfCurrent(
                    token,
                    PhotoSelectionErrors.RECOVER_PHOTO_FAILED
                )
            }
        }
    }

    /**
     * El usuario cerró el selector sin elegir una fotografía: no es un error y
     * la evidencia anterior, si existía, se mantiene disponible.
     */
    fun onGallerySelectionCancelled(token: PhotoOperationToken) {
        if (claimPhotoOperation(token)) {
            formState = formState.cancelPhotoOperation()
        }
    }

    fun onGallerySelectionError(token: PhotoOperationToken, message: String) {
        if (claimPhotoOperation(token)) {
            formState = formState.failPhotoOperation(message)
        }
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

    private fun cleanupExpiredPhotoFiles() {
        viewModelScope.launch(ioDispatcher) {
            try {
                photoFileStore.cleanupExpired()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // La limpieza es de mejor esfuerzo: no debe afectar el formulario.
                runCatching {
                    Log.w(
                        "CreateReportViewModel",
                        "No fue posible limpiar las evidencias temporales: " +
                            "type=${exception::class.java.name}, message=${exception.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(photoOperationLock) {
            activePhotoToken = null
        }
        formState.evidencePhotos.forEach { evidence ->
            deleteEvidenceFile(evidence.fileName)
        }
    }

    private fun beginPhotoOperation(operation: PhotoOperation): PhotoOperationToken =
        synchronized(photoOperationLock) {
            val token = UUID.randomUUID().toString()
            activePhotoToken = token
            formState = formState.startPhotoOperation(operation)
            token
        }

    private fun isCurrentPhotoOperation(token: PhotoOperationToken): Boolean =
        synchronized(photoOperationLock) {
            activePhotoToken == token
        }

    /**
     * Finaliza la operación identificada por [token] si sigue siendo la activa.
     *
     * La comprobación y el cambio de token ocurren en la misma sección crítica
     * para que dos resultados cercanos no puedan publicar a la vez.
     */
    private fun claimPhotoOperation(token: PhotoOperationToken): Boolean =
        synchronized(photoOperationLock) {
            if (activePhotoToken != token) {
                false
            } else {
                activePhotoToken = null
                true
            }
        }

    private fun failPhotoOperationIfCurrent(
        token: PhotoOperationToken,
        message: String
    ) {
        if (claimPhotoOperation(token)) {
            formState = formState.failPhotoOperation(message)
        }
    }

    /**
     * Incorpora una evidencia a la colección del formulario.
     *
     * El estado puede rechazarla, por ejemplo cuando se alcanza
     * [MAX_EVIDENCE_PHOTOS]. En ese caso el archivo recién creado se elimina
     * para no dejar una evidencia huérfana.
     */
    private fun addEvidencePhoto(evidence: IncidentPhotoEvidence) {
        formState = formState.addEvidencePhoto(evidence)

        if (formState.evidencePhotos.none { it.fileName == evidence.fileName }) {
            discardEvidencePhoto(evidence)
        }
    }

    /**
     * Elimina el archivo de un resultado que ya no corresponde a la operación
     * activa, salvo que coincida con alguna evidencia vigente.
     */
    private fun discardEvidencePhoto(evidence: IncidentPhotoEvidence) {
        if (formState.evidencePhotos.none { it.fileName == evidence.fileName }) {
            deleteEvidenceFile(evidence.fileName)
        }
    }

    private fun deleteEvidenceFile(fileName: String) {
        runCatching { photoFileStore.delete(fileName) }
    }

    private fun PhotoImportException.toUserMessage(): String = when (failure) {
        PhotoImportFailure.NOT_AN_IMAGE -> PhotoSelectionErrors.NOT_AN_IMAGE
        PhotoImportFailure.INVALID_IMAGE -> PhotoSelectionErrors.INVALID_IMAGE
        PhotoImportFailure.EMPTY_FILE -> PhotoSelectionErrors.EMPTY_FILE
        PhotoImportFailure.FILE_TOO_LARGE -> PhotoSelectionErrors.FILE_TOO_LARGE
        PhotoImportFailure.UNREADABLE -> PhotoSelectionErrors.ACCESS_DENIED
    }

    private fun CreateReportFormState.hasValidationErrors(): Boolean =
        titleError != null || descriptionError != null || categoryError != null

}
