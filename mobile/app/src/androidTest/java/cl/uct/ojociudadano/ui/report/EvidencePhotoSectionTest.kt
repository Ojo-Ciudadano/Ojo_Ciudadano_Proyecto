package cl.uct.ojociudadano.ui.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.ui.report.photo.PhotoCaptureErrors
import cl.uct.ojociudadano.ui.report.photo.PhotoSelectionErrors
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EvidencePhotoSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsBothPhotoActionsWithoutEvidence() {
        setSection()

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.SECTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.CAPTURE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.GALLERY_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomar fotografía").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elegir de la galería").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReportPhotoTestTags.CAPTURED_CARD).assertDoesNotExist()
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
    fun showsTheCapturedPhotoSummaryAndOffersRetake() {
        setSection(evidencePhoto = samplePhoto)

        composeTestRule.onNodeWithTag(ReportPhotoTestTags.CAPTURED_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText("Fotografía adjunta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomar otra fotografía").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elegir otra fotografía").assertIsDisplayed()
    }

    private fun setSection(
        evidencePhoto: IncidentPhotoEvidence? = null,
        activePhotoOperation: PhotoOperation? = null,
        errorMessage: String? = null,
        enabled: Boolean = true,
        onTakePhotoClick: () -> Unit = {},
        onPickPhotoClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                EvidencePhotoSection(
                    evidencePhoto = evidencePhoto,
                    activePhotoOperation = activePhotoOperation,
                    errorMessage = errorMessage,
                    enabled = enabled,
                    onTakePhotoClick = onTakePhotoClick,
                    onPickPhotoClick = onPickPhotoClick
                )
            }
        }
    }

    private companion object {
        val samplePhoto = IncidentPhotoEvidence(
            localUri = "content://cl.uct.ojociudadano.fileprovider/evidences/evidence_1.jpg",
            fileName = "evidence_1.jpg",
            sizeBytes = 1_258_291L,
            capturedAt = 1_700_000_000_000L
        )
    }
}
