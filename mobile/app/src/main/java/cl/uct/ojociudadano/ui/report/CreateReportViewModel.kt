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
import cl.uct.ojociudadano.data.model.CreateIncidentLocationInput
import cl.uct.ojociudadano.data.model.EvidenceUploadConstraints
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.domain.repository.LocationClient
import cl.uct.ojociudadano.ui.reports.incidentMessage
import cl.uct.ojociudadano.ui.report.photo.PhotoPreviewErrors
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
    private val photoFileStore: EvidencePhotoFileStore,
    private val locationClient: LocationClient,
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

    /**
     * Indica que el usuario ya solicitó crear el reporte y que el envío debe
     * continuar en cuanto el GPS entregue una ubicación válida.
     */
    private var submitAfterLocation = false

    init {
        loadCategories()
        refreshLocation()
        cleanupExpiredPhotoFiles()
    }

    fun onLocationDetailsChange(referenceAddress: String, commune: String, region: String, sector: String) {
        formState = formState.copy(referenceAddress = referenceAddress, commune = commune,
            region = region, sector = sector, locationDetailsError = null)
        onFormEdited()
    }

    fun onTitleChange(title: String) {
        formState = formState.updateTitle(title)
        onFormEdited()
    }

    fun onDescriptionChange(description: String) {
        formState = formState.updateDescription(description)
        onFormEdited()
    }

    fun onCategorySelected(category: Category) {
        formState = formState.updateCategory(category)
        onFormEdited()
    }

    /**
     * Solo un fallo de creación habilita un nuevo envío.
     *
     * Después de crear el incidente, editar el formulario no debe volver al
     * estado inicial porque habilitaría la creación de un incidente duplicado.
     */
    private fun onFormEdited() {
        if (submissionState is CreateReportSubmissionState.Error) {
            submissionState = CreateReportSubmissionState.Idle
        }
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

    /**
     * Elimina una evidencia de la colección y su archivo temporal.
     *
     * Solo se borra la fotografía indicada: las demás permanecen disponibles y
     * una operación de cámara o galería en curso no se cancela. El borrado es de
     * mejor esfuerzo para que un fallo del sistema de archivos no cierre la app.
     *
     * Si la fotografía ya no pertenece al formulario, la llamada no tiene
     * efecto para evitar eliminar archivos ajenos a la colección.
     */
    fun onRemovePhoto(photo: IncidentPhotoEvidence) {
        val current = formState.evidencePhotos.firstOrNull {
            it.fileName == photo.fileName
        } ?: return

        formState = formState.removeEvidencePhoto(current.fileName)
        deleteEvidenceFile(current.fileName)
    }

    /**
     * Gestiona un fallo al previsualizar una evidencia local.
     *
     * La fotografía nunca se elimina automáticamente: si el archivo dejó de
     * estar disponible, se informa el problema para que el usuario decida
     * eliminarla o reemplazarla. Así una evidencia pendiente de subida no puede
     * desaparecer de la colección y convertir un fallo en un éxito incorrecto.
     */
    fun onPhotoPreviewError(photo: IncidentPhotoEvidence) {
        val current = formState.evidencePhotos.firstOrNull {
            it.fileName == photo.fileName
        } ?: return

        formState = formState.reportPhotoPreviewError(
            if (isEvidenceFileAvailable(current.fileName)) {
                PhotoPreviewErrors.RENDER_FAILED
            } else {
                PhotoPreviewErrors.UNAVAILABLE
            }
        )
    }

    /**
     * Crea el incidente y, solo si la operación finaliza correctamente, sube
     * las fotografías seleccionadas como evidencias.
     *
     * Si ya existe un incidente creado con evidencias pendientes, la llamada
     * reintenta únicamente la subida y nunca crea un segundo incidente.
     */
    fun createReport() {
        when (val current = submissionState) {
            is CreateReportSubmissionState.CreatingIncident,
            is CreateReportSubmissionState.UploadingEvidence,
            is CreateReportSubmissionState.Success -> return

            is CreateReportSubmissionState.EvidenceUploadFailed -> {
                // Un resultado incierto no se reenvía automáticamente: el
                // usuario debe confirmar el riesgo de duplicar la evidencia.
                if (!current.hasUncertainEvidence) {
                    retryEvidenceUploads(current)
                }
                return
            }

            is CreateReportSubmissionState.Idle,
            is CreateReportSubmissionState.Error -> Unit
        }

        if (formState.activePhotoOperation != null) return

        formState = formValidator.validate(formState)

        if (formState.hasValidationErrors()) {
            submissionState = CreateReportSubmissionState.Idle
            return
        }

        val evidenceValidationErrors = validateEvidencePhotos(formState.evidencePhotos)

        if (evidenceValidationErrors.isNotEmpty()) {
            formState = formState.reportEvidenceValidationError(
                evidenceValidationErrors.joinToString("\n")
            )
            submissionState = CreateReportSubmissionState.Idle
            return
        }

        if (!formState.hasValidLocation) {
            createReportWhenLocationIsAvailable()
            return
        }

        submitReport()
    }

    /**
     * Solicita la ubicación GPS actual y conserva el resultado en el formulario.
     *
     * Si el usuario ya intentó crear el reporte, el envío continúa en cuanto
     * exista una ubicación válida. Si el GPS falla o no hay permisos, no se
     * construye ninguna solicitud con coordenadas inventadas.
     */
    fun refreshLocation() {
        if (formState.isLoadingLocation) return

        formState = formState.startLocationRequest()

        viewModelScope.launch {
            val result = try {
                locationClient.getCurrentLocation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                LocationResult.Error(
                    exception.message ?: CreateReportLocationErrors.UNKNOWN
                )
            }

            formState = formState.updateLocation(result)

            if (submitAfterLocation) {
                submitAfterLocation = false

                if (result is LocationResult.Success) {
                    createReport()
                }
            }
        }
    }

    /**
     * Refleja una denegación del permiso de ubicación sin dejar pendiente un
     * envío que ya no puede completarse.
     */
    fun onLocationPermissionDenied() {
        submitAfterLocation = false
        formState = formState.updateLocation(LocationResult.MissingPermissions)
    }

    private fun createReportWhenLocationIsAvailable() {
        submitAfterLocation = true
        submissionState = CreateReportSubmissionState.Idle
        refreshLocation()
    }

    /**
     * Construye la solicitud con las coordenadas que ya están asociadas al
     * formulario. La capa de networking solo recibe el resultado mediante
     * [CreateIncidentInput]; no consulta el GPS.
     */
    private fun submitReport() {
        val selectedCategory = formState.selectedCategory ?: return
        val location = formState.location as? LocationResult.Success ?: return
        val input = CreateIncidentInput(
            title = formState.title,
            description = formState.description,
            categoryId = selectedCategory.id,
            location = location.toIncidentLocationInput()
        )
        val photos = formState.evidencePhotos.toList()

        submissionState = CreateReportSubmissionState.CreatingIncident

        viewModelScope.launch {
            val incident = try {
                incidentRepository.createIncident(input)
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
                    exception.reportOperationMessage("crear el reporte")
                )
                return@launch
            }

            if (photos.isEmpty()) {
                submissionState = CreateReportSubmissionState.Success(
                    incidentId = incident.id,
                    uploadedEvidenceCount = 0
                )
                return@launch
            }

            uploadEvidencePhotos(
                incidentId = incident.id,
                photos = photos,
                alreadyUploaded = 0
            )
        }
    }

    /**
     * Reintenta las evidencias pendientes después de que el usuario confirmó
     * que acepta el riesgo de duplicarlas.
     *
     * Solo tiene efecto cuando el estado actual contiene al menos una evidencia
     * cuyo resultado es incierto. El reintento conserva el incidente original y
     * nunca vuelve a llamar a [IncidentRepository.createIncident].
     */
    fun confirmRetryAfterPossibleDuplicate() {
        val current = submissionState as? CreateReportSubmissionState.EvidenceUploadFailed
            ?: return

        if (!current.hasUncertainEvidence) return

        retryEvidenceUploads(current)
    }

    /**
     * Sube las evidencias de forma secuencial y aislada.
     *
     * Un fallo individual se registra y se continúa con la siguiente fotografía.
     * Las evidencias subidas se eliminan del formulario y del almacén temporal;
     * las fallidas permanecen para poder reintentarlas contra el mismo
     * incidente.
     */
    private suspend fun uploadEvidencePhotos(
        incidentId: String,
        photos: List<IncidentPhotoEvidence>,
        alreadyUploaded: Int
    ) {
        submissionState = CreateReportSubmissionState.UploadingEvidence(
            incidentId = incidentId,
            processed = 0,
            total = photos.size
        )

        var uploadedNow = 0
        val pendingEvidence = mutableListOf<PendingEvidenceUpload>()

        photos.forEachIndexed { index, photo ->
            when (val result = uploadEvidence(incidentId, photo)) {
                EvidenceUploadResult.Uploaded -> {
                    uploadedNow++
                    formState = formState.removeEvidencePhoto(photo.fileName)
                    deleteEvidenceFile(photo.fileName)
                }

                is EvidenceUploadResult.Failed -> {
                    pendingEvidence += PendingEvidenceUpload(
                        photo = photo,
                        failureKind = result.failureKind,
                        reason = result.reason
                    )
                }
            }

            submissionState = CreateReportSubmissionState.UploadingEvidence(
                incidentId = incidentId,
                processed = index + 1,
                total = photos.size
            )
        }

        if (pendingEvidence.isEmpty()) {
            clearEvidencePhotoState()
            submissionState = CreateReportSubmissionState.Success(
                incidentId = incidentId,
                uploadedEvidenceCount = alreadyUploaded + uploadedNow
            )
            return
        }

        val totalEvidence = alreadyUploaded + uploadedNow + pendingEvidence.size
        val hasUncertainEvidence = pendingEvidence.any {
            it.failureKind == EvidenceUploadFailureKind.POSSIBLY_UPLOADED
        }
        submissionState = CreateReportSubmissionState.EvidenceUploadFailed(
            incidentId = incidentId,
            uploadedEvidenceCount = alreadyUploaded + uploadedNow,
            pendingEvidence = pendingEvidence,
            message = EvidenceUploadErrors.partial(
                failed = pendingEvidence.size,
                total = totalEvidence,
                hasUncertainEvidence = hasUncertainEvidence
            )
        )
    }

    /**
     * Resultado de un intento de subida individual.
     *
     * [Failed.failureKind] distingue un rechazo confirmado de un resultado
     * incierto que podría haberse guardado en el servidor.
     */
    private sealed interface EvidenceUploadResult {
        data object Uploaded : EvidenceUploadResult

        data class Failed(
            val failureKind: EvidenceUploadFailureKind,
            val reason: String? = null
        ) : EvidenceUploadResult
    }

    /**
     * Sube una única evidencia y clasifica el resultado de un eventual fallo.
     *
     * La cancelación de la corrutina se propaga: no representa un fallo de la
     * evidencia, sino que el flujo completo dejó de ejecutarse.
     */
    private suspend fun uploadEvidence(
        incidentId: String,
        photo: IncidentPhotoEvidence
    ): EvidenceUploadResult {
        val file = runCatching { photoFileStore.file(photo.fileName) }.getOrNull()

        if (file == null) {
            runCatching {
                Log.w(
                    "CreateReportViewModel",
                    "Evidencia no disponible para subir: fileName=${photo.fileName}"
                )
            }
            return EvidenceUploadResult.Failed(
                failureKind = EvidenceUploadFailureKind.CONFIRMED,
                reason = EvidenceUploadErrors.FILE_UNAVAILABLE
            )
        }

        return try {
            incidentRepository.uploadEvidence(incidentId, file, photo.mimeType)
            EvidenceUploadResult.Uploaded
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            runCatching {
                Log.w(
                    "CreateReportViewModel",
                    "Error al subir evidencia: incidentId=$incidentId, " +
                        "fileName=${photo.fileName}, type=${exception::class.java.name}, " +
                        "message=${exception.message}",
                    exception
                )
            }
            EvidenceUploadResult.Failed(
                failureKind = classifyEvidenceUploadFailure(exception),
                reason = exception.message
            )
        }
    }

    /**
     * Distingue un rechazo confirmado de un resultado incierto.
     *
     * Los errores 4xx y las validaciones locales permiten afirmar que el
     * backend no guardó la evidencia. Los timeouts, desconexiones, respuestas
     * 5xx y errores inesperados no permiten saberlo: la petición pudo haber
     * llegado al servidor y su respuesta se perdió en el camino.
     */
    private fun classifyEvidenceUploadFailure(
        exception: Exception
    ): EvidenceUploadFailureKind = when {
        exception is ApiException.Http && exception.statusCode in 400..499 ->
            EvidenceUploadFailureKind.CONFIRMED

        exception is IllegalArgumentException ->
            EvidenceUploadFailureKind.CONFIRMED

        else -> EvidenceUploadFailureKind.POSSIBLY_UPLOADED
    }

    /**
     * Reintenta solo las evidencias pendientes del incidente ya creado.
     *
     * La lista de pendientes vive en [CreateReportSubmissionState.EvidenceUploadFailed],
     * no en las miniaturas visibles. Así una fotografía cuyo archivo desapareció
     * sigue siendo un fallo explícito y no se declara un éxito por una colección
     * visible vacía.
     */
    private fun retryEvidenceUploads(state: CreateReportSubmissionState.EvidenceUploadFailed) {
        val pending = state.pendingEvidence.map { it.photo }

        if (pending.isEmpty()) return

        submissionState = CreateReportSubmissionState.UploadingEvidence(
            incidentId = state.incidentId,
            processed = 0,
            total = pending.size
        )

        viewModelScope.launch {
            uploadEvidencePhotos(
                incidentId = state.incidentId,
                photos = pending,
                alreadyUploaded = state.uploadedEvidenceCount
            )
        }
    }

    /**
     * Finaliza el flujo conservando únicamente las evidencias ya subidas.
     *
     * El incidente y sus fotografías exitosas ya existen en el backend, por lo
     * que esta operación no vuelve a crear el incidente ni elimina evidencias
     * remotas. Solo descarta las fotografías locales que quedaron pendientes.
     */
    fun finishWithoutPendingEvidence() {
        val current = submissionState as? CreateReportSubmissionState.EvidenceUploadFailed
            ?: return

        clearEvidencePhotoState(
            extraPhotos = current.pendingEvidence.map { it.photo }
        )
        submissionState = CreateReportSubmissionState.Success(
            incidentId = current.incidentId,
            uploadedEvidenceCount = current.uploadedEvidenceCount,
            discardedEvidenceCount = current.pendingEvidence.size
        )
    }

    /**
     * Limpia las referencias y los archivos temporales de las fotografías.
     *
     * [extraPhotos] permite borrar pendientes que ya no estén en la colección
     * visible, por ejemplo si su miniatura dejó de estar disponible.
     */
    private fun clearEvidencePhotoState(
        extraPhotos: List<IncidentPhotoEvidence> = emptyList()
    ) {
        (formState.evidencePhotos + extraPhotos)
            .distinctBy { it.fileName }
            .forEach { evidence ->
                deleteEvidenceFile(evidence.fileName)
            }
        formState = formState.copy(
            evidencePhotos = emptyList(),
            activePhotoOperation = null,
            photoError = null
        )
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
                    errorMessage = exception.reportOperationMessage("cargar las categorías")
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
        submitAfterLocation = false

        val pendingEvidence =
            (submissionState as? CreateReportSubmissionState.EvidenceUploadFailed)
                ?.pendingEvidence
                .orEmpty()
                .map { it.photo }

        (formState.evidencePhotos + pendingEvidence)
            .distinctBy { it.fileName }
            .forEach { evidence ->
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
     * Si el archivo temporal ya no está disponible, la evidencia se descarta y
     * se informa un error controlado. El estado también puede rechazarla, por
     * ejemplo cuando se alcanza [MAX_EVIDENCE_PHOTOS]. En ambos casos el archivo
     * recién creado se elimina para no dejar una evidencia huérfana.
     */
    private fun addEvidencePhoto(evidence: IncidentPhotoEvidence) {
        if (!isEvidenceFileAvailable(evidence.fileName)) {
            discardEvidencePhoto(evidence)
            formState = formState.failPhotoOperation(PhotoPreviewErrors.UNAVAILABLE)
            return
        }

        formState = formState.addEvidencePhoto(evidence)

        if (formState.evidencePhotos.none { it.fileName == evidence.fileName }) {
            discardEvidencePhoto(evidence)
        }
    }

    private fun isEvidenceFileAvailable(fileName: String): Boolean =
        runCatching { photoFileStore.exists(fileName) }.getOrDefault(false)

    /**
     * Comprueba que todas las fotografías puedan subirse antes de crear el
     * incidente.
     *
     * Si alguna no cumple los límites del backend, el usuario recibe el motivo
     * específico y el formulario sigue editable para eliminarla o reemplazarla.
     * Así no se crea un incidente con evidencias que nunca podrán asociarse.
     */
    private fun validateEvidencePhotos(
        photos: List<IncidentPhotoEvidence>
    ): List<String> = photos.mapIndexedNotNull { index, photo ->
        val issues = EvidenceUploadConstraints.issuesFor(
            photo = photo,
            fileAvailable = isEvidenceFileAvailable(photo.fileName)
        )

        issues.takeIf { it.isNotEmpty() }?.let {
            EvidenceUploadErrors.compatibility(
                position = index + 1,
                photo = photo,
                issues = it
            )
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

    /**
     * La geocodificación inversa está fuera del alcance de esta tarea. Se
     * conservan las coordenadas reales y se completan los campos descriptivos
     * obligatorios del contrato con valores explícitos, sin reutilizar la
     * ubicación temporal de Sprint 1.
     */
    private fun LocationResult.Success.toIncidentLocationInput() =
        CreateIncidentLocationInput(
            latitude = latitude,
            longitude = longitude,
            referenceAddress = formState.referenceAddress.trim(),
            commune = formState.commune.trim(),
            region = formState.region.trim(),
            sector = formState.sector.trim()
        )

    private fun PhotoImportException.toUserMessage(): String = when (failure) {
        PhotoImportFailure.NOT_AN_IMAGE -> PhotoSelectionErrors.NOT_AN_IMAGE
        PhotoImportFailure.INVALID_IMAGE -> PhotoSelectionErrors.INVALID_IMAGE
        PhotoImportFailure.EMPTY_FILE -> PhotoSelectionErrors.EMPTY_FILE
        PhotoImportFailure.FILE_TOO_LARGE -> PhotoSelectionErrors.FILE_TOO_LARGE
        PhotoImportFailure.UNREADABLE -> PhotoSelectionErrors.ACCESS_DENIED
    }

    private fun CreateReportFormState.hasValidationErrors(): Boolean =
        titleError != null || descriptionError != null || categoryError != null || locationDetailsError != null

}

/** Controlled messages shared by category loading and incident creation. */
private fun Throwable.reportOperationMessage(operation: String): String = when (this) {
    is cl.uct.ojociudadano.data.session.SessionRequiredException -> incidentMessage()
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para $operation."
        in 400..499 -> "No se pudo $operation. Revisa los datos e intenta nuevamente."
        else -> "El servicio no está disponible. Intenta más tarde."
    }
    is ApiException.Network, is java.io.IOException -> "No fue posible conectar con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "No fue posible $operation. Intenta nuevamente."
}
