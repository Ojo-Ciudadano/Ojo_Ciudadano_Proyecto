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
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.data.session.SessionRequiredException
import cl.uct.ojociudadano.ui.report.photo.PhotoCaptureErrors
import cl.uct.ojociudadano.ui.report.photo.PhotoPreviewErrors
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
import java.io.File
import java.io.IOException
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
    fun `submits valid form without accepting a reporter identity from UI`() = runTest {
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 0
            ),
            viewModel.submissionState
        )
        assertEquals(1, incidentRepository.createInputs.size)
        assertTrue(incidentRepository.uploadAttempts.isEmpty())

        val input = incidentRepository.createInputs.single()
        assertEquals("Poste dañado", input.title)
        assertEquals("El poste bloquea la vereda", input.description)
        assertEquals(sampleCategory.id, input.categoryId)
        assertEquals(-38.7359, input.location.latitude, 0.0)
        assertEquals(-72.5904, input.location.longitude, 0.0)
        assertEquals("Ubicación de prueba Sprint 1", input.location.referenceAddress)
        assertEquals("Temuco", input.location.commune)
        assertEquals("La Araucanía", input.location.region)
        assertEquals("Centro", input.location.sector)
    }

    @Test
    fun `exposes error when incident creation fails`() = runTest {
        val incidentRepository = FakeIncidentRepository(
            failure = IllegalStateException("Error de red")
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.Error("Error de red"),
            viewModel.submissionState
        )
        assertTrue(incidentRepository.uploadAttempts.isEmpty())
        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
    }

    @Test
    fun `shows session failure returned by repository without crashing`() = runTest {
        val failure = SessionRequiredException()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = FakeIncidentRepository(failure = failure)
        )
        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        viewModel.createReport()

        assertEquals(CreateReportSubmissionState.Error(failure.message!!), viewModel.submissionState)
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
            incidentRepository = incidentRepository
        )

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(sampleCategory)

        viewModel.createReport()
        creationStarted.await()
        viewModel.createReport()

        assertEquals(CreateReportSubmissionState.CreatingIncident, viewModel.submissionState)
        assertEquals(1, incidentRepository.createInputs.size)

        releaseCreation.complete(Unit)

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 0
            ),
            viewModel.submissionState
        )
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

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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
        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
    }

    @Test
    fun `appends a new capture without replacing the previous photo`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onPhotoCaptureStarted()

        viewModel.onPhotoCaptured(token, secondSamplePhoto)

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
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

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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
        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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
            assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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
        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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
        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
    }

    @Test
    fun `appends the gallery selection and keeps the previous photo`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore(importedPhoto = secondSamplePhoto)
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `keeps a single photo when the gallery selection resolves to the same file`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore(importedPhoto = samplePhoto)
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)
        val token = viewModel.onGallerySelectionStarted()

        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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

        assertEquals(listOf(secondSamplePhoto), viewModel.formState.evidencePhotos)

        photoFileStore.completeImport(FIRST_CONTENT_URI, samplePhoto)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(secondSamplePhoto), viewModel.formState.evidencePhotos)
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

        assertEquals(listOf(secondSamplePhoto), viewModel.formState.evidencePhotos)
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

        assertEquals(listOf(secondSamplePhoto), viewModel.formState.evidencePhotos)
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

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
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

        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertEquals(PhotoOperation.CAMERA, viewModel.formState.activePhotoOperation)

        viewModel.onPhotoCaptured(secondToken, secondSamplePhoto)

        assertEquals(listOf(secondSamplePhoto), viewModel.formState.evidencePhotos)
        assertNull(viewModel.formState.activePhotoOperation)
    }

    @Test
    fun `accumulates photos captured with the camera and selected from the gallery`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore(importedPhoto = secondSamplePhoto)
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )

        viewModel.seedPhoto(samplePhoto)

        val token = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
        assertNull(viewModel.formState.activePhotoOperation)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `removes only the selected photo and keeps the rest of the collection`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        viewModel.onTitleChange("Poste dañado")
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto, thirdSamplePhoto)

        viewModel.onRemovePhoto(secondSamplePhoto)

        assertEquals(
            listOf(samplePhoto, thirdSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertEquals(listOf(secondSamplePhoto.fileName), photoFileStore.deletedFileNames)
        assertEquals("Poste dañado", viewModel.formState.title)
        assertNull(viewModel.formState.photoError)
    }

    @Test
    fun `removing an unknown photo does not change the collection or delete files`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.onRemovePhoto(thirdSamplePhoto)

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `removing a photo does not cancel an active photo operation`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)
        val token = viewModel.onPhotoCaptureStarted()

        viewModel.onRemovePhoto(samplePhoto)

        assertEquals(listOf(secondSamplePhoto), viewModel.formState.evidencePhotos)
        assertEquals(PhotoOperation.CAMERA, viewModel.formState.activePhotoOperation)
        assertEquals(listOf(samplePhoto.fileName), photoFileStore.deletedFileNames)

        viewModel.onPhotoCaptured(token, thirdSamplePhoto)

        assertEquals(
            listOf(secondSamplePhoto, thirdSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertNull(viewModel.formState.activePhotoOperation)
    }

    @Test
    fun `keeps a photo whose file is no longer available and reports it`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)
        photoFileStore.markMissing(samplePhoto.fileName)

        viewModel.onPhotoPreviewError(samplePhoto)

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertEquals(PhotoPreviewErrors.UNAVAILABLE, viewModel.formState.photoError)
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `keeps a photo whose file exists but cannot be rendered`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.onPhotoPreviewError(samplePhoto)

        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertEquals(PhotoPreviewErrors.RENDER_FAILED, viewModel.formState.photoError)
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `ignores preview errors for photos outside the collection`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        viewModel.seedPhoto(samplePhoto)

        viewModel.onPhotoPreviewError(secondSamplePhoto)

        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
        assertNull(viewModel.formState.photoError)
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `rejects a captured photo whose file is not available`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )
        photoFileStore.markMissing(samplePhoto.fileName)

        val token = viewModel.onPhotoCaptureStarted()
        viewModel.onPhotoCaptured(token, samplePhoto)

        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertEquals(PhotoPreviewErrors.UNAVAILABLE, viewModel.formState.photoError)
        assertEquals(listOf(samplePhoto.fileName), photoFileStore.deletedFileNames)
        assertNull(viewModel.formState.activePhotoOperation)
    }

    @Test
    fun `rejects a gallery photo whose imported file is not available`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore(importedPhoto = samplePhoto)
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore,
            ioDispatcher = testDispatcher
        )
        photoFileStore.markMissing(samplePhoto.fileName)

        val token = viewModel.onGallerySelectionStarted()
        viewModel.onGalleryPhotoSelected(token, "content://media/picker/photo")

        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertEquals(PhotoPreviewErrors.UNAVAILABLE, viewModel.formState.photoError)
        assertEquals(listOf(samplePhoto.fileName), photoFileStore.deletedFileNames)
        assertNull(viewModel.formState.activePhotoOperation)
    }

    @Test
    fun `rejects a photo beyond the maximum and deletes its file`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            photoFileStore = photoFileStore
        )

        repeat(MAX_EVIDENCE_PHOTOS) { index ->
            viewModel.seedPhoto(
                samplePhoto.copy(
                    fileName = "evidence_$index.jpg",
                    localUri = "content://evidences/evidence_$index.jpg"
                )
            )
        }

        val extraPhoto = samplePhoto.copy(
            fileName = "evidence_extra.jpg",
            localUri = "content://evidences/evidence_extra.jpg"
        )
        val token = viewModel.onPhotoCaptureStarted()

        viewModel.onPhotoCaptured(token, extraPhoto)

        assertEquals(MAX_EVIDENCE_PHOTOS, viewModel.formState.evidencePhotos.size)
        assertTrue(
            viewModel.formState.evidencePhotos.none {
                it.fileName == extraPhoto.fileName
            }
        )
        assertEquals(MAX_EVIDENCE_PHOTOS_ERROR, viewModel.formState.photoError)
        assertEquals(listOf(extraPhoto.fileName), photoFileStore.deletedFileNames)
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
    fun `deletes all accumulated evidence when the view model is cleared`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModelStore = ViewModelStore()
        val factory = CreateReportViewModelFactory(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = FakeIncidentRepository(),
            photoFileStore = photoFileStore
        )
        val viewModel = ViewModelProvider(viewModelStore, factory)
            .get(CreateReportViewModel::class.java)

        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)
        viewModelStore.clear()

        assertEquals(
            listOf(samplePhoto.fileName, secondSamplePhoto.fileName),
            photoFileStore.deletedFileNames
        )
    }

    @Test
    fun `creates the report and uploads the selected evidence photos`() = runTest {
        val incidentRepository = FakeIncidentRepository()
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            photoFileStore = photoFileStore
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 2
            ),
            viewModel.submissionState
        )
        assertEquals(
            CreateIncidentInput(
                title = "Poste dañado",
                description = "El poste bloquea la vereda",
                categoryId = sampleCategory.id,
                location = Sprint1TemporaryLocation.incidentLocation
            ),
            incidentRepository.createInputs.single()
        )
        assertEquals(
            listOf(
                UploadedEvidence("incident-id", samplePhoto.fileName, samplePhoto.mimeType),
                UploadedEvidence(
                    "incident-id",
                    secondSamplePhoto.fileName,
                    secondSamplePhoto.mimeType
                )
            ),
            incidentRepository.uploadAttempts
        )
        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertEquals(
            listOf(samplePhoto.fileName, secondSamplePhoto.fileName),
            photoFileStore.deletedFileNames
        )
    }

    @Test
    fun `uploads several photos with their own mime types and the created incident id`() = runTest {
        val pngPhoto = secondSamplePhoto.copy(
            fileName = "evidence_png.png",
            mimeType = "image/png"
        )
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, pngPhoto)

        viewModel.createReport()

        assertEquals(
            listOf(
                UploadedEvidence("incident-id", samplePhoto.fileName, "image/jpeg"),
                UploadedEvidence("incident-id", pngPhoto.fileName, "image/png")
            ),
            incidentRepository.uploadAttempts
        )
        assertEquals(1, incidentRepository.createInputs.size)
    }

    @Test
    fun `continues uploading the remaining photos when one evidence fails`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val incidentRepository = FakeIncidentRepository(
            onUpload = { file, _ ->
                if (file.name == secondSamplePhoto.fileName) {
                    throw confirmedUploadFailure()
                }
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            photoFileStore = photoFileStore
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto, thirdSamplePhoto)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.EvidenceUploadFailed(
                incidentId = "incident-id",
                uploadedEvidenceCount = 2,
                pendingEvidence = listOf(
                    PendingEvidenceUpload(
                        photo = secondSamplePhoto,
                        failureKind = EvidenceUploadFailureKind.CONFIRMED,
                        reason = "Archivo inválido"
                    )
                ),
                message = EvidenceUploadErrors.partial(failed = 1, total = 3)
            ),
            viewModel.submissionState
        )
        assertEquals(3, incidentRepository.uploadAttempts.size)
        assertEquals(1, incidentRepository.createInputs.size)
        assertEquals(
            listOf(secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertEquals(
            listOf(samplePhoto.fileName, thirdSamplePhoto.fileName),
            photoFileStore.deletedFileNames
        )
    }

    @Test
    fun `keeps every photo when all evidence uploads fail`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = FakeIncidentRepository(
                onUpload = { _, _ -> throw confirmedUploadFailure() }
            ),
            photoFileStore = photoFileStore
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.EvidenceUploadFailed(
                incidentId = "incident-id",
                uploadedEvidenceCount = 0,
                pendingEvidence = listOf(
                    PendingEvidenceUpload(
                        photo = samplePhoto,
                        failureKind = EvidenceUploadFailureKind.CONFIRMED,
                        reason = "Archivo inválido"
                    ),
                    PendingEvidenceUpload(
                        photo = secondSamplePhoto,
                        failureKind = EvidenceUploadFailureKind.CONFIRMED,
                        reason = "Archivo inválido"
                    )
                ),
                message = EvidenceUploadErrors.partial(failed = 2, total = 2)
            ),
            viewModel.submissionState
        )
        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )
        assertTrue(photoFileStore.deletedFileNames.isEmpty())
    }

    @Test
    fun `retries only pending evidence against the existing incident`() = runTest {
        var failFirstPhoto = true
        val photoFileStore = FakeEvidencePhotoFileStore()
        val incidentRepository = FakeIncidentRepository(
            onUpload = { file, _ ->
                if (failFirstPhoto && file.name == samplePhoto.fileName) {
                    failFirstPhoto = false
                    throw confirmedUploadFailure()
                }
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            photoFileStore = photoFileStore
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.EvidenceUploadFailed(
                incidentId = "incident-id",
                uploadedEvidenceCount = 1,
                pendingEvidence = listOf(
                    PendingEvidenceUpload(
                        photo = samplePhoto,
                        failureKind = EvidenceUploadFailureKind.CONFIRMED,
                        reason = "Archivo inválido"
                    )
                ),
                message = EvidenceUploadErrors.partial(failed = 1, total = 2)
            ),
            viewModel.submissionState
        )

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 2
            ),
            viewModel.submissionState
        )
        assertEquals(1, incidentRepository.createInputs.size)
        assertEquals(3, incidentRepository.uploadAttempts.size)
        assertEquals(
            UploadedEvidence("incident-id", samplePhoto.fileName, samplePhoto.mimeType),
            incidentRepository.uploadAttempts.last()
        )
        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertEquals(
            listOf(secondSamplePhoto.fileName, samplePhoto.fileName),
            photoFileStore.deletedFileNames
        )
    }

    @Test
    fun `marks a transport failure as possibly uploaded and requires confirmation before retrying`() =
        runTest {
            var attempts = 0
            val incidentRepository = FakeIncidentRepository(
                onUpload = { _, _ ->
                    attempts++
                    if (attempts == 1) {
                        throw ApiException.Network(IOException("timeout"))
                    }
                }
            )
            val viewModel = createViewModel(
                categoryRepository = FakeCategoryRepository(),
                incidentRepository = incidentRepository
            )

            viewModel.fillValidForm()
            viewModel.seedPhotos(samplePhoto)
            viewModel.createReport()

            val failed =
                viewModel.submissionState as CreateReportSubmissionState.EvidenceUploadFailed
            val pending = failed.pendingEvidence.single()
            assertEquals(samplePhoto, pending.photo)
            assertEquals(
                EvidenceUploadFailureKind.POSSIBLY_UPLOADED,
                pending.failureKind
            )
            assertTrue(failed.hasUncertainEvidence)
            assertEquals(
                EvidenceUploadErrors.partial(
                    failed = 1,
                    total = 1,
                    hasUncertainEvidence = true
                ),
                failed.message
            )
            assertEquals(1, incidentRepository.createInputs.size)
            assertEquals(1, incidentRepository.uploadAttempts.size)

            viewModel.createReport()

            assertEquals(1, incidentRepository.uploadAttempts.size)
            assertEquals(1, incidentRepository.createInputs.size)
            assertTrue(
                viewModel.submissionState is CreateReportSubmissionState.EvidenceUploadFailed
            )

            viewModel.confirmRetryAfterPossibleDuplicate()

            assertEquals(
                CreateReportSubmissionState.Success(
                    incidentId = "incident-id",
                    uploadedEvidenceCount = 1
                ),
                viewModel.submissionState
            )
            assertEquals(2, incidentRepository.uploadAttempts.size)
            assertEquals(1, incidentRepository.createInputs.size)
        }

    @Test
    fun `treats server errors as possibly uploaded and blocks automatic retry`() = runTest {
        var attempts = 0
        val incidentRepository = FakeIncidentRepository(
            onUpload = { _, _ ->
                attempts++
                if (attempts == 1) {
                    throw ApiException.Http(500, emptyList(), null, null)
                }
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto)
        viewModel.createReport()

        val failed =
            viewModel.submissionState as CreateReportSubmissionState.EvidenceUploadFailed
        assertEquals(
            EvidenceUploadFailureKind.POSSIBLY_UPLOADED,
            failed.pendingEvidence.single().failureKind
        )
        assertTrue(failed.hasUncertainEvidence)

        viewModel.createReport()

        assertEquals(1, incidentRepository.uploadAttempts.size)
        assertEquals(1, incidentRepository.createInputs.size)
        assertTrue(viewModel.submissionState is CreateReportSubmissionState.EvidenceUploadFailed)
    }

    @Test
    fun `retries confirmed failures without duplicate confirmation`() = runTest {
        var attempts = 0
        val incidentRepository = FakeIncidentRepository(
            onUpload = { _, _ ->
                attempts++
                if (attempts == 1) {
                    throw confirmedUploadFailure()
                }
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto)
        viewModel.createReport()

        val failed =
            viewModel.submissionState as CreateReportSubmissionState.EvidenceUploadFailed
        assertEquals(
            EvidenceUploadFailureKind.CONFIRMED,
            failed.pendingEvidence.single().failureKind
        )
        assertFalse(failed.hasUncertainEvidence)
        assertEquals(
            EvidenceUploadErrors.partial(failed = 1, total = 1),
            failed.message
        )

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 1
            ),
            viewModel.submissionState
        )
        assertEquals(2, incidentRepository.uploadAttempts.size)
        assertEquals(1, incidentRepository.createInputs.size)
    }

    @Test
    fun `reports upload progress and clears photos only after the last evidence`() = runTest {
        val firstUploadStarted = CompletableDeferred<Unit>()
        val releaseFirstUpload = CompletableDeferred<Unit>()
        val secondUploadStarted = CompletableDeferred<Unit>()
        val releaseSecondUpload = CompletableDeferred<Unit>()
        val incidentRepository = FakeIncidentRepository(
            onUpload = { file, _ ->
                if (file.name == samplePhoto.fileName) {
                    firstUploadStarted.complete(Unit)
                    releaseFirstUpload.await()
                } else {
                    secondUploadStarted.complete(Unit)
                    releaseSecondUpload.await()
                }
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)

        viewModel.createReport()
        firstUploadStarted.await()

        assertEquals(
            CreateReportSubmissionState.UploadingEvidence(
                incidentId = "incident-id",
                processed = 0,
                total = 2
            ),
            viewModel.submissionState
        )
        assertEquals(
            listOf(samplePhoto, secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )

        releaseFirstUpload.complete(Unit)
        secondUploadStarted.await()

        assertEquals(
            CreateReportSubmissionState.UploadingEvidence(
                incidentId = "incident-id",
                processed = 1,
                total = 2
            ),
            viewModel.submissionState
        )
        assertEquals(
            listOf(secondSamplePhoto),
            viewModel.formState.evidencePhotos
        )

        releaseSecondUpload.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 2
            ),
            viewModel.submissionState
        )
        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
    }

    @Test
    fun `prevents duplicate incident creation while evidence is uploading`() = runTest {
        val uploadStarted = CompletableDeferred<Unit>()
        val releaseUpload = CompletableDeferred<Unit>()
        val incidentRepository = FakeIncidentRepository(
            onUpload = { _, _ ->
                uploadStarted.complete(Unit)
                releaseUpload.await()
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto)

        viewModel.createReport()
        uploadStarted.await()
        viewModel.createReport()

        assertEquals(1, incidentRepository.createInputs.size)
        assertEquals(1, incidentRepository.uploadAttempts.size)
        assertEquals(
            CreateReportSubmissionState.UploadingEvidence(
                incidentId = "incident-id",
                processed = 0,
                total = 1
            ),
            viewModel.submissionState
        )

        releaseUpload.complete(Unit)

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 1
            ),
            viewModel.submissionState
        )
    }

    @Test
    fun `treats a missing local file as a controlled evidence failure`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            photoFileStore = photoFileStore
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)
        photoFileStore.markMissing(samplePhoto.fileName)

        viewModel.createReport()

        assertEquals(
            CreateReportSubmissionState.EvidenceUploadFailed(
                incidentId = "incident-id",
                uploadedEvidenceCount = 1,
                pendingEvidence = listOf(
                    PendingEvidenceUpload(
                        photo = samplePhoto,
                        failureKind = EvidenceUploadFailureKind.CONFIRMED,
                        reason = EvidenceUploadErrors.FILE_UNAVAILABLE
                    )
                ),
                message = EvidenceUploadErrors.partial(failed = 1, total = 2)
            ),
            viewModel.submissionState
        )
        assertEquals(1, incidentRepository.uploadAttempts.size)
        assertEquals(
            UploadedEvidence("incident-id", secondSamplePhoto.fileName, secondSamplePhoto.mimeType),
            incidentRepository.uploadAttempts.single()
        )
        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
    }

    @Test
    fun `keeps a pending evidence after its preview fails and does not report success on retry`() =
        runTest {
            val photoFileStore = FakeEvidencePhotoFileStore()
            val incidentRepository = FakeIncidentRepository(
                onUpload = { file, _ ->
                    if (file.name == samplePhoto.fileName) {
                        throw confirmedUploadFailure()
                    }
                }
            )
            val viewModel = createViewModel(
                categoryRepository = FakeCategoryRepository(),
                incidentRepository = incidentRepository,
                photoFileStore = photoFileStore
            )

            viewModel.fillValidForm()
            viewModel.seedPhotos(samplePhoto, secondSamplePhoto)
            viewModel.createReport()

            assertTrue(
                viewModel.submissionState is CreateReportSubmissionState.EvidenceUploadFailed
            )

            photoFileStore.markMissing(samplePhoto.fileName)
            viewModel.onPhotoPreviewError(samplePhoto)

            assertEquals(
                listOf(samplePhoto),
                viewModel.formState.evidencePhotos
            )
            assertEquals(PhotoPreviewErrors.UNAVAILABLE, viewModel.formState.photoError)
            assertEquals(
                listOf(samplePhoto),
                (viewModel.submissionState as CreateReportSubmissionState.EvidenceUploadFailed)
                    .pendingEvidence
                    .map { it.photo }
            )

            viewModel.createReport()

            assertEquals(
                listOf(samplePhoto),
                (viewModel.submissionState as CreateReportSubmissionState.EvidenceUploadFailed)
                    .pendingEvidence
                    .map { it.photo }
            )
            assertEquals(1, incidentRepository.createInputs.size)
            assertEquals(2, incidentRepository.uploadAttempts.size)
        }

    @Test
    fun `editing the form after a partial upload does not create another incident`() = runTest {
        val incidentRepository = FakeIncidentRepository(
            onUpload = { _, _ -> throw confirmedUploadFailure() }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto)
        viewModel.createReport()

        assertTrue(viewModel.submissionState is CreateReportSubmissionState.EvidenceUploadFailed)

        viewModel.onTitleChange("Otro título")

        assertTrue(viewModel.submissionState is CreateReportSubmissionState.EvidenceUploadFailed)
        assertEquals(1, incidentRepository.createInputs.size)

        viewModel.createReport()

        assertEquals(1, incidentRepository.createInputs.size)
        assertEquals(2, incidentRepository.uploadAttempts.size)
    }

    @Test
    fun `finishes with the uploaded photos and discards the pending evidence`() = runTest {
        val photoFileStore = FakeEvidencePhotoFileStore()
        val incidentRepository = FakeIncidentRepository(
            onUpload = { file, _ ->
                if (file.name == samplePhoto.fileName) {
                    throw confirmedUploadFailure()
                }
            }
        )
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository,
            photoFileStore = photoFileStore
        )

        viewModel.fillValidForm()
        viewModel.seedPhotos(samplePhoto, secondSamplePhoto)
        viewModel.createReport()

        assertTrue(viewModel.submissionState is CreateReportSubmissionState.EvidenceUploadFailed)

        viewModel.finishWithoutPendingEvidence()

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = "incident-id",
                uploadedEvidenceCount = 1,
                discardedEvidenceCount = 1
            ),
            viewModel.submissionState
        )
        assertEquals(1, incidentRepository.createInputs.size)
        assertEquals(2, incidentRepository.uploadAttempts.size)
        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertEquals(
            listOf(secondSamplePhoto.fileName, samplePhoto.fileName),
            photoFileStore.deletedFileNames
        )
    }

    @Test
    fun `does not finish early when the incident has not been created`() = runTest {
        val incidentRepository = FakeIncidentRepository()
        val viewModel = createViewModel(
            categoryRepository = FakeCategoryRepository(),
            incidentRepository = incidentRepository
        )

        viewModel.seedPhotos(samplePhoto)

        viewModel.finishWithoutPendingEvidence()

        assertEquals(CreateReportSubmissionState.Idle, viewModel.submissionState)
        assertEquals(listOf(samplePhoto), viewModel.formState.evidencePhotos)
        assertTrue(incidentRepository.createInputs.isEmpty())
    }
}

private fun createViewModel(
    categoryRepository: CategoryRepository,
    incidentRepository: IncidentRepository = FakeIncidentRepository(),
    photoFileStore: EvidencePhotoFileStore = FakeEvidencePhotoFileStore(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): CreateReportViewModel = CreateReportViewModel(
    categoryRepository = categoryRepository,
    incidentRepository = incidentRepository,
    photoFileStore = photoFileStore,
    ioDispatcher = ioDispatcher
)

private fun CreateReportViewModel.seedPhoto(photo: IncidentPhotoEvidence) {
    val token = onPhotoCaptureStarted()
    onPhotoCaptured(token, photo)
}

private fun CreateReportViewModel.seedPhotos(vararg photos: IncidentPhotoEvidence) {
    photos.forEach { seedPhoto(it) }
}

private fun CreateReportViewModel.fillValidForm() {
    onTitleChange("Poste dañado")
    onDescriptionChange("El poste bloquea la vereda")
    onCategorySelected(sampleCategory)
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

    override fun exists(fileName: String): Boolean = true

    override fun file(fileName: String): File? = null

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
    private val missingFileNames = mutableSetOf<String>()

    override fun create(): PendingPhotoCapture = error("No usado en estas pruebas")

    override fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence? = null

    override suspend fun import(contentUri: String): IncidentPhotoEvidence {
        importFailure?.let { throw it }
        return importedPhoto
    }

    override fun exists(fileName: String): Boolean = fileName !in missingFileNames

    override fun file(fileName: String): File? =
        if (fileName in missingFileNames) null else File(fileName)

    fun markMissing(fileName: String) {
        missingFileNames += fileName
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

private fun confirmedUploadFailure(): ApiException.Http =
    ApiException.Http(
        statusCode = 400,
        serverMessages = emptyList(),
        error = "Archivo inválido",
        path = "/api/v1/incidentes/evidencias"
    )

private data class UploadedEvidence(
    val incidentId: String,
    val fileName: String,
    val mimeType: String
)

private class FakeIncidentRepository(
    private val failure: Throwable? = null,
    private val onCreate: suspend () -> Unit = {},
    private val onUpload: suspend (file: File, mimeType: String) -> Unit = { _, _ -> }
) : IncidentRepository {

    val createInputs = mutableListOf<CreateIncidentInput>()
    val uploadAttempts = mutableListOf<UploadedEvidence>()

    override suspend fun getIncidents(): List<Incident> = emptyList()

    override suspend fun getCurrentUserIncidents(): List<Incident> = emptyList()

    override suspend fun getIncidentById(id: String): Incident = error("Not used by this test")

    override suspend fun createIncident(input: CreateIncidentInput): Incident {
        createInputs += input
        failure?.let { throw it }
        onCreate()
        return sampleIncident(input)
    }

    override suspend fun uploadEvidence(
        incidenteId: String,
        file: File,
        mimeType: String
    ): cl.uct.ojociudadano.domain.model.Evidence {
        uploadAttempts += UploadedEvidence(incidenteId, file.name, mimeType)
        onUpload(file, mimeType)
        return cl.uct.ojociudadano.domain.model.Evidence(
            id = "evidence-${uploadAttempts.size}",
            incidentId = incidenteId,
            mimeType = mimeType,
            size = file.length(),
            createdAt = "2026-10-30T00:00:00.000Z",
            fileUrl = "/evidencias/evidence-${uploadAttempts.size}/archivo"
        )
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
    reporterUserId = "a0000000-0000-4000-8000-000000000001",
    priorityLevel = null,
    createdAt = "2026-09-30T00:00:00.000Z",
    updatedAt = "2026-09-30T00:00:00.000Z"
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

private val thirdSamplePhoto = samplePhoto.copy(
    localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_3.jpg",
    fileName = "evidence_3.jpg",
    sizeBytes = 3072L,
    capturedAt = 1_700_000_002_000L
)

private const val FIRST_CONTENT_URI = "content://media/picker/photo/first"
private const val SECOND_CONTENT_URI = "content://media/picker/photo/second"
