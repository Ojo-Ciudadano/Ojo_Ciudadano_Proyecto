package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PendingPhotoCapture
import cl.uct.ojociudadano.data.media.PhotoImportException
import cl.uct.ojociudadano.data.media.PhotoImportFailure
import cl.uct.ojociudadano.data.media.TEMP_EVIDENCE_MAX_AGE_MILLIS
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.data.model.Sprint1TemporaryLocation
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.AuthenticatedSession
import cl.uct.ojociudadano.data.session.SessionStorage
import cl.uct.ojociudadano.ui.report.photo.PhotoCaptureErrors
import cl.uct.ojociudadano.ui.report.photo.PhotoSelectionErrors
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

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

    @Test
    fun `stores the captured photo while keeping the rest of the form`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )
        viewModel.onTitleChange("Poste dañado")

        val token = viewModel.onPhotoCaptureStarted()

        assertEquals(PhotoOperation.CAMERA, viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)

        viewModel.onPhotoCaptured(token, samplePhoto)

        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
        assertEquals("Poste dañado", viewModel.formState.title)
    }

    @Test
    fun `cancelling the capture keeps the previous photo and reports no error`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onPhotoCaptureStarted()

        viewModel.onPhotoCaptureCancelled(token)

        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
        assertEquals(CreateReportSubmissionState.Idle, viewModel.submissionState)
    }

    @Test
    fun `exposes the camera error and keeps the previous photo`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onPhotoCaptureStarted()

        viewModel.onPhotoCaptureError(token, PhotoCaptureErrors.PERMISSION_REQUIRED)

        assertEquals(
            PhotoCaptureErrors.PERMISSION_REQUIRED,
            viewModel.formState.photoError
        )
        assertNull(viewModel.formState.activePhotoOperation)
        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
    }

    @Test
    fun `replaces the previous photo when a new capture succeeds`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onPhotoCaptureStarted()

        viewModel.onPhotoCaptured(token, secondSamplePhoto)

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `stores the photo selected from the gallery and clears the operation`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            ioDispatcher = testDispatcher
        )
        viewModel.onTitleChange("Poste dañado")

        val token = viewModel.onGallerySelectionStarted()

        assertEquals(PhotoOperation.GALLERY, viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
        assertEquals("Poste dañado", viewModel.formState.title)
    }

    @Test
    fun `cancelling the gallery selection keeps the previous photo without error`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGallerySelectionCancelled(token)

        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `reports picker launch errors and keeps the previous photo`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGallerySelectionError(token, PhotoSelectionErrors.PICKER_UNAVAILABLE)

        assertEquals(PhotoSelectionErrors.PICKER_UNAVAILABLE, viewModel.formState.photoError)
        assertNull(viewModel.formState.activePhotoOperation)
        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
    }

    @Test
    fun `translates gallery import failures and keeps the previous photo`() = runTest {
        val cases = listOf(
            PhotoImportFailure.NOT_AN_IMAGE to PhotoSelectionErrors.NOT_AN_IMAGE,
            PhotoImportFailure.INVALID_IMAGE to PhotoSelectionErrors.INVALID_IMAGE,
            PhotoImportFailure.EMPTY_FILE to PhotoSelectionErrors.EMPTY_FILE,
            PhotoImportFailure.FILE_TOO_LARGE to PhotoSelectionErrors.FILE_TOO_LARGE,
            PhotoImportFailure.UNREADABLE to PhotoSelectionErrors.ACCESS_DENIED
        )

        cases.forEach { (failure, expectedMessage) ->
            val viewModel = createViewModel(
                categoryRepository = FakeCategoryRepository(),
                photoFileStore = FakeEvidencePhotoFileStore(
                    importFailure = PhotoImportException(failure, "Fallo de importación")
                ),
                ioDispatcher = testDispatcher
            )
            viewModel.seedPhoto(samplePhoto)
            val token = viewModel.onGallerySelectionStarted()

            viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

            assertEquals(expectedMessage, viewModel.formState.photoError)
            assertNull(viewModel.formState.activePhotoOperation)
            assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        }
    }

    @Test
    fun `reports access denied when the selected photo cannot be read`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = FakeEvidencePhotoFileStore(
                importFailure = SecurityException("Permiso revocado")
            ),
            ioDispatcher = testDispatcher
        )
        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(PhotoSelectionErrors.ACCESS_DENIED, viewModel.formState.photoError)
        assertNull(viewModel.formState.activePhotoOperation)
        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
    }

    @Test
    fun `reports a generic error for unexpected gallery import failures`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = FakeEvidencePhotoFileStore(
                importFailure = IllegalStateException("Fallo inesperado")
            ),
            ioDispatcher = testDispatcher
        )
        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(PhotoSelectionErrors.RECOVER_PHOTO_FAILED, viewModel.formState.photoError)
        assertNull(viewModel.formState.activePhotoOperation)
        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
    }

    @Test
    fun `replaces the previous photo with the gallery selection and deletes its file`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore(importedPhoto = secondSamplePhoto)
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)
        assertEquals(listOf(samplePhoto.fileName), photoFileStore.deletedFileNames)
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `keeps the current file when the gallery selection resolves to the same photo`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore(importedPhoto = samplePhoto)
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `discards an older gallery import when a newer one finishes first`() = runTest {
        val photoFileStore = ControllableEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        val firstToken = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(firstToken, FIRST_CONTENT_URI)
        testDispatcher.scheduler.runCurrent()

        val secondToken = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(secondToken, SECOND_CONTENT_URI)
        testDispatcher.scheduler.runCurrent()

        photoFileStore.completeImport(SECOND_CONTENT_URI, secondSamplePhoto)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)

        photoFileStore.completeImport(FIRST_CONTENT_URI, samplePhoto)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)
        assertTrue(photoFileStore.deletedFileNames.contains(samplePhoto.fileName))
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `ignores a stale gallery error after a newer selection succeeded`() = runTest {
        val photoFileStore = ControllableEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        val firstToken = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(firstToken, FIRST_CONTENT_URI)
        testDispatcher.scheduler.runCurrent()

        val secondToken = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(secondToken, SECOND_CONTENT_URI)
        testDispatcher.scheduler.runCurrent()

        photoFileStore.failImport(
            FIRST_CONTENT_URI,
            PhotoImportException(PhotoImportFailure.UNREADABLE, "Error obsoleto")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.formState.photoError)
        assertEquals(PhotoOperation.GALLERY, viewModel.formState.activePhotoOperation)

        photoFileStore.completeImport(SECOND_CONTENT_URI, secondSamplePhoto)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `ignores a stale cancellation from a previous gallery operation`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        val firstToken = viewModel.onGallerySelectionStarted()
        val secondToken = viewModel.onGallerySelectionStarted()

        viewModel.onGallerySelectionCancelled(firstToken)

        assertEquals(PhotoOperation.GALLERY, viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)

        viewModel.onGallerySelectionCancelled(secondToken)

        assertNull(viewModel.formState.activePhotoOperation)
    }

    @Test
    fun `keeps the gallery photo when a stale camera result arrives later`() = runTest {
        val photoFileStore = ControllableEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        val cameraToken = viewModel.onPhotoCaptureStarted()
        val galleryToken = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(galleryToken, FIRST_CONTENT_URI)
        testDispatcher.scheduler.runCurrent()

        photoFileStore.completeImport(FIRST_CONTENT_URI, secondSamplePhoto)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onPhotoCaptured(cameraToken, samplePhoto)

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)
        assertTrue(photoFileStore.deletedFileNames.contains(samplePhoto.fileName))
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `keeps the camera photo when a stale gallery import finishes later`() = runTest {
        val photoFileStore = ControllableEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        val galleryToken = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(galleryToken, FIRST_CONTENT_URI)
        testDispatcher.scheduler.runCurrent()

        val cameraToken = viewModel.onPhotoCaptureStarted()
        viewModel.onPhotoCaptured(cameraToken, samplePhoto)

        photoFileStore.completeImport(FIRST_CONTENT_URI, secondSamplePhoto)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(samplePhoto, viewModel.formState.evidencePhoto)
        assertTrue(photoFileStore.deletedFileNames.contains(secondSamplePhoto.fileName))
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `ignores an older camera capture when a newer operation is active`() = runTest {
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository()
        )

        val firstToken = viewModel.onPhotoCaptureStarted()
        val secondToken = viewModel.onPhotoCaptureStarted()

        viewModel.onPhotoCaptured(firstToken, samplePhoto)

        assertNull(viewModel.formState.evidencePhoto)
        assertEquals(PhotoOperation.CAMERA, viewModel.formState.activePhotoOperation)

        viewModel.onPhotoCaptured(secondToken, secondSamplePhoto)

        assertEquals(secondSamplePhoto, viewModel.formState.evidencePhoto)
        assertNull(viewModel.formState.activePhotoOperation)
    }

    @Test
    fun `cleans expired evidence files when the view model is created`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()

        createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        assertEquals(
            listOf(TEMP_EVIDENCE_MAX_AGE_MILLIS),
            photoFileStore.cleanupMaxAges
        )
    }

    @Test
    fun `deletes the current evidence when the view model is cleared`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModelStore = ViewModelStore()
        val factory = CreateReportViewModelFactory(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = FakeIncidentRepository(),
            sessionStorage = FakeSessionStorage(),
            photoFileStore = photoFileStore
        )
        val viewModel = ViewModelProvider(viewModelStore, factory)
            .get(CreateReportViewModel::class.java)

        viewModel.seedPhoto(samplePhoto)
        viewModelStore.clear()

        assertEquals(listOf(samplePhoto.fileName), photoFileStore.deletedFileNames)
    }

    @Test
    fun `creates the report without sending the evidence photo`() = runTest {
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            sessionStorage = FakeSessionStorage(sampleSession)
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)
        viewModel.seedPhoto(samplePhoto)

        viewModel.createReport()

        assertEquals(CreateReportSubmissionState.Success, viewModel.submissionState)
        assertEquals(
            CreateIncidentInput(
                title = "Poste dañado",
                description = "El poste bloquea la vereda",
                categoryId = sampleCategory.id,
                reporterUserId = sampleSession.userId,
                location = Sprint1TemporaryLocation.incidentLocation
            ),
            incidentRepository.createInputs.single()
        )
    }
}

private fun createViewModel(
    categoryRepository: CategoryRepository,
    incidentRepository: IncidentRepository = FakeIncidentRepository(),
    sessionStorage: SessionStorage = FakeSessionStorage(),
    photoFileStore: EvidencePhotoFileStore = FakeEvidencePhotoFileStore(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): CreateReportViewModel = CreateReportViewModel(
    categoryRepository = categoryRepository,
    incidentRepository = incidentRepository,
    sessionStorage = sessionStorage,
    photoFileStore = photoFileStore,
    ioDispatcher = ioDispatcher
)

private fun CreateReportViewModel.seedPhoto(photo: IncidentPhotoEvidence) {
    val token = onPhotoCaptureStarted()
    onPhotoCaptured(token, photo)
}

private class ControllableEvidencePhotoFileStore : EvidencePhotoFileStore {

    val pendingImports =
        mutableMapOf<String, CompletableDeferred<IncidentPhotoEvidence>>()
    val deletedFileNames = CopyOnWriteArrayList<String>()

    override fun create(): PendingPhotoCapture = error("No usado en estas pruebas")

    override fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence? = null

    override suspend fun import(contentUri: String): IncidentPhotoEvidence {
        val deferred = CompletableDeferred<IncidentPhotoEvidence>()
        pendingImports[contentUri] = deferred
        return deferred.await()
    }

    fun completeImport(contentUri: String, photo: IncidentPhotoEvidence) {
        pendingImports.getValue(contentUri).complete(photo)
    }

    fun failImport(contentUri: String, failure: Throwable) {
        pendingImports.getValue(contentUri).completeExceptionally(failure)
    }

    override fun delete(fileName: String) {
        deletedFileNames += fileName
    }

    override fun cleanupExpired(maxAgeMillis: Long): Int = 0
}

private class FakeEvidencePhotoFileStore(
    private val importedPhoto: IncidentPhotoEvidence = samplePhoto,
    private val importFailure: Throwable? = null
) : EvidencePhotoFileStore {

    val deletedFileNames = mutableListOf<String>()
    val cleanupMaxAges = CopyOnWriteArrayList<Long>()

    override fun create(): PendingPhotoCapture = error("No usado en estas pruebas")

    override fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence? = null

    override suspend fun import(contentUri: String): IncidentPhotoEvidence {
        importFailure?.let { throw it }
        return importedPhoto
    }

    override fun delete(fileName: String) {
        deletedFileNames += fileName
    }

    override fun cleanupExpired(maxAgeMillis: Long): Int {
        cleanupMaxAges += maxAgeMillis
        return 0
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

    override suspend fun uploadEvidence(
        incidenteId: String,
        file: java.io.File,
        mimeType: String
    ): cl.uct.ojociudadano.domain.model.Evidence {
        throw NotImplementedError("No implementado para esta prueba")
    }

    override suspend fun getEvidencesByIncident(incidenteId: String): List<cl.uct.ojociudadano.domain.model.Evidence> {
        return emptyList()
    }

    override suspend fun getEvidenceById(id: String): cl.uct.ojociudadano.domain.model.Evidence {
        throw NotImplementedError("No implementado para esta prueba")
    }

    override suspend fun downloadEvidenceFile(id: String): ResponseBody {
        throw NotImplementedError("No implementado para esta prueba")
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
    id = "3d059063-63eb-4a4c-9f2a-27eea72f84fb",
    name = "Categoría real",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)

private val samplePhoto = IncidentPhotoEvidence(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_1.jpg",
    fileName = "evidence_1.jpg",
    sizeBytes = 1024L,
    capturedAt = 1_700_000_000_000L
)

private val secondSamplePhoto = samplePhoto.copy(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_2.jpg",
    fileName = "evidence_2.jpg",
    sizeBytes = 2048L,
    capturedAt = 1_700_000_001_000L
)

private const val FIRST_CONTENT_URI = "content://media/picker/photo/first"
private const val SECOND_CONTENT_URI = "content://media/picker/photo/second"
