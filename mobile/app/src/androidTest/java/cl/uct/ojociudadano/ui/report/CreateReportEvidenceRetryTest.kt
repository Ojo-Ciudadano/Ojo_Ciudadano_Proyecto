package cl.uct.ojociudadano.ui.report

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreateReportEvidenceRetryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun successOpensTheCreatedReportWithoutSubmittingAgain() {
        var opened = ""
        var submits = 0
        setScreen(CreateReportSubmissionState.Success("created-id", 0),
            onCreateReportClick = { submits++ }, onConfirmRetry = {}, onViewReport = { opened = it })
        composeTestRule.onNodeWithText("Reporte creado").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithText("Ver reporte creado").performScrollTo().performClick()
        composeTestRule.runOnIdle {
            assertEquals("created-id", opened)
            assertEquals(0, submits)
        }
    }

    @Test
    fun categoryFailureOffersAnExplicitRetry() {
        var retries = 0
        setScreen(CreateReportSubmissionState.Idle, {}, {}, categoryError = "No fue posible cargar las categorías.",
            onRetryCategories = { retries++ })
        composeTestRule.onNodeWithText("Reintentar categorías").performScrollTo().performClick()
        composeTestRule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun uncertainRetryRequiresConfirmationBeforeCallingTheViewModel() {
        var directRetries = 0
        var confirmedRetries = 0

        setScreen(
            submissionState = failedState(
                failureKind = EvidenceUploadFailureKind.POSSIBLY_UPLOADED
            ),
            onCreateReportClick = { directRetries += 1 },
            onConfirmRetry = { confirmedRetries += 1 }
        )

        composeTestRule
            .onNodeWithText("Reintentar evidencias (1)")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(EvidenceUploadErrors.UNCERTAIN_RETRY_WARNING)
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            assertEquals(0, directRetries)
            assertEquals(0, confirmedRetries)
        }

        composeTestRule
            .onNodeWithText("Reintentar de todas formas")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, directRetries)
            assertEquals(1, confirmedRetries)
        }
    }

    @Test
    fun confirmedRetryDoesNotShowTheDuplicateWarning() {
        var directRetries = 0
        var confirmedRetries = 0

        setScreen(
            submissionState = failedState(
                failureKind = EvidenceUploadFailureKind.CONFIRMED
            ),
            onCreateReportClick = { directRetries += 1 },
            onConfirmRetry = { confirmedRetries += 1 }
        )

        composeTestRule
            .onNodeWithText("Reintentar evidencias (1)")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(EvidenceUploadErrors.UNCERTAIN_RETRY_WARNING)
            .assertDoesNotExist()

        composeTestRule.runOnIdle {
            assertEquals(1, directRetries)
            assertEquals(0, confirmedRetries)
        }
    }

    private fun setScreen(
        submissionState: CreateReportSubmissionState,
        onCreateReportClick: () -> Unit,
        onConfirmRetry: () -> Unit,
        onViewReport: (String) -> Unit = {},
        categoryError: String? = null,
        onRetryCategories: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                CreateReportScreen(
                    state = CreateReportFormState(
                        title = "Poste dañado",
                        description = "El poste bloquea la vereda",
                        selectedCategory = category
                    ),
                    categories = listOf(category),
                    isLoadingCategories = false,
                    categoryLoadError = categoryError,
                    submissionState = submissionState,
                    onTitleChange = {},
                    onDescriptionChange = {},
                    onCategorySelected = {},
                    onTakePhotoClick = {},
                    onPickPhotoClick = {},
                    onRemovePhoto = {},
                    onPhotoPreviewError = {},
                    onCreateReportClick = onCreateReportClick,
                    onConfirmRetry = onConfirmRetry,
                    onFinishWithoutPendingEvidence = {},
                    onViewReport = onViewReport,
                    onRetryCategories = onRetryCategories
                )
            }
        }
    }

    private fun failedState(
        failureKind: EvidenceUploadFailureKind
    ): CreateReportSubmissionState.EvidenceUploadFailed =
        CreateReportSubmissionState.EvidenceUploadFailed(
            incidentId = "incident-id",
            uploadedEvidenceCount = 0,
            pendingEvidence = listOf(
                PendingEvidenceUpload(
                    photo = photo,
                    failureKind = failureKind
                )
            ),
            message = EvidenceUploadErrors.partial(
                failed = 1,
                total = 1,
                hasUncertainEvidence =
                    failureKind == EvidenceUploadFailureKind.POSSIBLY_UPLOADED
            )
        )

    private companion object {
        val photo = IncidentPhotoEvidence(
            localUri = "content://evidences/evidence_1.jpg",
            fileName = "evidence_1.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1024L,
            capturedAt = 1_700_000_000_000L
        )

        val category = Category(
            id = "category-id",
            name = "Alumbrado público",
            description = "",
            basePriority = 1,
            isCriticalEmergency = false,
            officialContactChannel = null
        )
    }
}
