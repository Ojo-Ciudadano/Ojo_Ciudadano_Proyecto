package cl.uct.ojociudadano.ui.report

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreateReportLocationPickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun opensTheMapPickerFromTheLocationSection() {
        var openRequests = 0
        setScreen(onChooseLocationOnMap = { openRequests += 1 })

        composeTestRule
            .onNodeWithText("Elegir ubicación en el mapa")
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, openRequests)
        }
    }

    @Test
    fun identifiesWhenTheLocationComesFromTheMap() {
        setScreen(
            location = LocationResult.Success(latitude = -38.7396, longitude = -72.5984),
            locationSource = LocationSelectionSource.MANUAL
        )

        composeTestRule
            .onNodeWithText("Ubicación seleccionada en el mapa")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun disablesTheMapActionWhileTheFormIsNotEditable() {
        setScreen(
            submissionState = CreateReportSubmissionState.CreatingIncident
        )

        composeTestRule
            .onNodeWithText("Elegir ubicación en el mapa")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    private fun setScreen(
        location: LocationResult = LocationResult.NotAvailable,
        locationSource: LocationSelectionSource = LocationSelectionSource.NONE,
        submissionState: CreateReportSubmissionState = CreateReportSubmissionState.Idle,
        onChooseLocationOnMap: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            OjoCiudadanoTheme {
                CreateReportScreen(
                    state = CreateReportFormState(
                        location = location,
                        locationSource = locationSource
                    ),
                    categories = emptyList(),
                    isLoadingCategories = false,
                    categoryLoadError = null,
                    submissionState = submissionState,
                    onTitleChange = {},
                    onDescriptionChange = {},
                    onCategorySelected = {},
                    onTakePhotoClick = {},
                    onPickPhotoClick = {},
                    onRemovePhoto = {},
                    onPhotoPreviewError = {},
                    onCreateReportClick = {},
                    onConfirmRetry = {},
                    onFinishWithoutPendingEvidence = {},
                    onChooseLocationOnMap = onChooseLocationOnMap
                )
            }
        }
    }
}
