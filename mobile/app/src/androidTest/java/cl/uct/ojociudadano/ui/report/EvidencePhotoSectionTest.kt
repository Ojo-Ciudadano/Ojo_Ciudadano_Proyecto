package cl.uct.ojociudadano.ui.report

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import cl.uct.ojociudadano.data.media.CacheEvidencePhotoFileStore
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.ui.report.photo.PhotoCaptureErrors
import cl.uct.ojociudadano.ui.report.photo.PhotoSelectionErrors
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EvidencePhotoSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fileStore = CacheEvidencePhotoFileStore(context)
    private val createdFileNames = mutableListOf<String>()

    @After
    fun tearDown() {
        createdFileNames.forEach(fileStore::delete)
    }

    @Test
    fun showsBothPhotoActionsWithoutEvidence() {
        setSection()

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.SECTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.CAPTURE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.GALLERY_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomar fotografía").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elegir de la galería").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PHOTO_LIST).assertDoesNotExist()
    }

    @Test
    fun notifiesTheFormWhenTheUserAsksToCapture() {
        var captureRequests = 0
        setSection(onTakePhotoClick = { captureRequests += 1 })

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.CAPTURE_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, captureRequests)
        }
    }

    @Test
    fun notifiesTheFormWhenTheUserAsksToPickFromTheGallery() {
        var galleryRequests = 0
        setSection(onPickPhotoClick = { galleryRequests += 1 })

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.GALLERY_BUTTON).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, galleryRequests)
        }
    }

    @Test
    fun showsProgressAndDisablesBothButtonsWhileCapturing() {
        setSection(activePhotoOperation = PhotoOperation.CAMERA)

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.CAPTURING_INDICATOR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Abriendo cámara...").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.CAPTURE_BUTTON)
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.GALLERY_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun showsProgressAndDisablesBothButtonsWhilePickingFromTheGallery() {
        setSection(activePhotoOperation = PhotoOperation.GALLERY)

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.GALLERY_INDICATOR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Procesando fotografía...").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.GALLERY_BUTTON)
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.CAPTURE_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun disablesBothButtonsWhileTheFormIsSubmitting() {
        setSection(enabled = false)

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.CAPTURE_BUTTON)
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.GALLERY_BUTTON)
            .assertIsNotEnabled()
    }

    @Test
    fun disablesTheRemoveActionWhileTheFormIsSubmitting() {
        setSection(evidencePhotos = listOf(cameraPhoto), enabled = false)

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.removeButton(cameraPhoto.fileName))
            .assertIsNotEnabled()
    }

    @Test
    fun showsTheCaptureErrorMessage() {
        setSection(errorMessage = PhotoCaptureErrors.PERMISSION_REQUIRED)

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.ERROR_MESSAGE)
            .assertIsDisplayed()
            .assertTextEquals(PhotoCaptureErrors.PERMISSION_REQUIRED)
    }

    @Test
    fun showsTheGalleryErrorMessage() {
        setSection(errorMessage = PhotoSelectionErrors.RECOVER_PHOTO_FAILED)

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.ERROR_MESSAGE)
            .assertIsDisplayed()
            .assertTextEquals(PhotoSelectionErrors.RECOVER_PHOTO_FAILED)
    }

    @Test
    fun showsThumbnailsForCameraAndGalleryPhotos() {
        setSection(evidencePhotos = listOf(cameraPhoto, galleryPhoto))

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PHOTO_LIST).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(cameraPhoto.fileName))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(galleryPhoto.fileName))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("2 fotografías adjuntas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomar otra fotografía").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elegir otra fotografía").assertIsDisplayed()
    }

    @Test
    fun notifiesRemovalOfTheSelectedPhoto() {
        var removedPhoto: IncidentPhotoEvidence? = null
        setSection(
            evidencePhotos = listOf(cameraPhoto, galleryPhoto),
            onRemovePhoto = { removedPhoto = it }
        )

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.removeButton(galleryPhoto.fileName))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(galleryPhoto, removedPhoto)
        }
    }

    @Test
    fun updatesTheListWhenAPhotoIsRemoved() {
        var photos by mutableStateOf(listOf(cameraPhoto, galleryPhoto))
        setSectionWithState(
            photos = { photos },
            onRemovePhoto = { removed -> photos = photos - removed }
        )

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.removeButton(cameraPhoto.fileName))
            .performClick()

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(cameraPhoto.fileName))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(galleryPhoto.fileName))
            .assertIsDisplayed()
    }

    @Test
    fun removesTheMiddlePhotoWithoutAffectingTheOthers() {
        var photos by mutableStateOf(listOf(cameraPhoto, galleryPhoto, thirdPhoto))
        setSectionWithState(
            photos = { photos },
            onRemovePhoto = { removed -> photos = photos - removed }
        )

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.removeButton(galleryPhoto.fileName))
            .performClick()

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(cameraPhoto.fileName))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(galleryPhoto.fileName))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(thirdPhoto.fileName))
            .assertIsDisplayed()
    }

    @Test
    fun updatesTheListWhenAPhotoIsAdded() {
        var photos by mutableStateOf(emptyList<IncidentPhotoEvidence>())
        setSectionWithState(
            photos = { photos },
            onRemovePhoto = {}
        )

        composeTestRule.runOnIdle {
            photos = listOf(cameraPhoto)
        }

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(cameraPhoto.fileName))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("1 fotografía adjunta").assertIsDisplayed()
    }

    @Test
    fun opensAndClosesTheFullScreenPreview() {
        setSection(evidencePhotos = listOf(cameraPhoto))

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(cameraPhoto.fileName))
            .performClick()

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PREVIEW_DIALOG).assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.CLOSE_PREVIEW_BUTTON)
            .performClick()

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PREVIEW_DIALOG).assertDoesNotExist()
    }

    @Test
    fun closesThePreviewDialogWhenTheOpenPhotoIsRemoved() {
        var photos by mutableStateOf(listOf(cameraPhoto, galleryPhoto))
        setSectionWithState(
            photos = { photos },
            onRemovePhoto = { removed -> photos = photos - removed }
        )

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(cameraPhoto.fileName))
            .performClick()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PREVIEW_DIALOG).assertIsDisplayed()

        composeTestRule.runOnIdle {
            photos = photos - cameraPhoto
        }

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PREVIEW_DIALOG).assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(galleryPhoto.fileName))
            .assertIsDisplayed()
    }

    @Test
    fun showsFallbackWhenThePreviewCannotBeLoaded() {
        setSection(evidencePhotos = listOf(unavailablePhoto))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(
                    ReportPhotoTestTags.previewError(unavailablePhoto.fileName)
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("No disponible").assertIsDisplayed()
    }

    @Test
    fun notifiesTheFormWhenAPhotoCannotBePreviewed() {
        var previewErrors = 0
        setSection(
            evidencePhotos = listOf(unavailablePhoto),
            onPhotoPreviewError = { previewErrors += 1 }
        )

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(
                    ReportPhotoTestTags.previewError(unavailablePhoto.fileName)
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.runOnIdle {
            assertEquals(1, previewErrors)
        }
    }

    @Test
    fun showsThePreviewFallbackInsideTheDialog() {
        setSection(evidencePhotos = listOf(unavailablePhoto))

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(unavailablePhoto.fileName))
            .performClick()

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PREVIEW_DIALOG).assertIsDisplayed()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("No fue posible mostrar la fotografía")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("No fue posible mostrar la fotografía")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.CLOSE_PREVIEW_BUTTON)
            .performClick()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.PREVIEW_DIALOG).assertDoesNotExist()
    }

    @Test
    fun allowsRemovingAPhotoWhosePreviewFailed() {
        var removedPhoto: IncidentPhotoEvidence? = null
        setSection(
            evidencePhotos = listOf(unavailablePhoto),
            onRemovePhoto = { removedPhoto = it }
        )

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(
                    ReportPhotoTestTags.previewError(unavailablePhoto.fileName)
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.removeButton(unavailablePhoto.fileName))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(unavailablePhoto, removedPhoto)
        }
    }

    @Test
    fun displaysPreviewForAnAvailableLocalPhoto() {
        val evidence = createRealEvidence()
        setSection(evidencePhotos = listOf(evidence))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(ReportPhotoTestTags.loaded(evidence.fileName))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag(ReportPhotoTestTags.thumbnail(evidence.fileName))
            .assertIsDisplayed()
    }

    private fun setSection(
        evidencePhotos: List<IncidentPhotoEvidence> = emptyList(),
        activePhotoOperation: PhotoOperation? = null,
        errorMessage: String? = null,
        enabled: Boolean = true,
        onTakePhotoClick: () -> Unit = {},
        onPickPhotoClick: () -> Unit = {},
        onRemovePhoto: (IncidentPhotoEvidence) -> Unit = {},
        onPhotoPreviewError: (IncidentPhotoEvidence) -> Unit = {}
    ) {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                EvidencePhotoSection(
                    evidencePhotos = evidencePhotos,
                    activePhotoOperation = activePhotoOperation,
                    errorMessage = errorMessage,
                    enabled = enabled,
                    onTakePhotoClick = onTakePhotoClick,
                    onPickPhotoClick = onPickPhotoClick,
                    onRemovePhoto = onRemovePhoto,
                    onPhotoPreviewError = onPhotoPreviewError
                )
            }
        }
    }

    private fun setSectionWithState(
        photos: () -> List<IncidentPhotoEvidence>,
        onRemovePhoto: (IncidentPhotoEvidence) -> Unit,
        onPhotoPreviewError: (IncidentPhotoEvidence) -> Unit = {}
    ) {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                EvidencePhotoSection(
                    evidencePhotos = photos(),
                    activePhotoOperation = null,
                    errorMessage = null,
                    enabled = true,
                    onTakePhotoClick = {},
                    onPickPhotoClick = {},
                    onRemovePhoto = onRemovePhoto,
                    onPhotoPreviewError = onPhotoPreviewError
                )
            }
        }
    }

    private fun createRealEvidence(): IncidentPhotoEvidence {
        val pending = fileStore.create()
        createdFileNames += pending.fileName

        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)

        try {
            context.contentResolver
                .openOutputStream(Uri.parse(pending.localUri))!!
                .use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
        } finally {
            bitmap.recycle()
        }

        return requireNotNull(fileStore.complete(pending))
    }

    private companion object {
        val cameraPhoto = IncidentPhotoEvidence(
            localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/camera.jpg",
            fileName = "camera.jpg",
            sizeBytes = 1_258_291L,
            capturedAt = 1_700_000_000_000L
        )

        val galleryPhoto = IncidentPhotoEvidence(
            localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/gallery.jpg",
            fileName = "gallery.jpg",
            sizeBytes = 2_048_000L,
            capturedAt = 1_700_000_001_000L
        )

        val thirdPhoto = IncidentPhotoEvidence(
            localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/third.jpg",
            fileName = "third.jpg",
            sizeBytes = 512_000L,
            capturedAt = 1_700_000_002_000L
        )

        val unavailablePhoto = IncidentPhotoEvidence(
            localUri = "content://cl.uct.ojociudadano.invalid/missing.jpg",
            fileName = "missing.jpg",
            sizeBytes = 1_024L,
            capturedAt = 1_700_000_003_000L
        )
    }
}
