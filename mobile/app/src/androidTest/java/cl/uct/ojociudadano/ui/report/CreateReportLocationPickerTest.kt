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
    fun opensTheMapPickerFromTheLocationSelector() {
        var openRequests = 0
        setScreen(onChooseLocationOnMap = { openRequests += 1 })

        composeTestRule
            .onNodeWithText("Elegir en el mapa")
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, openRequests)
        }
    }

    @Test
    fun requestsTheCurrentGpsLocationFromTheLocationSelector() {
        var refreshRequests = 0
        setScreen(onRefreshLocation = { refreshRequests += 1 })

        composeTestRule
            .onNodeWithText("Usar mi GPS actual")
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, refreshRequests)
        }
    }

    @Test
    fun identifiesWhenTheLocationComesFromTheMap() {
        setScreen(
            location = LocationResult.Success(latitude = -38.7396, longitude = -72.5984),
            locationSource = LocationSelectionSource.MANUAL
        )

        composeTestRule
            .onNodeWithText("Seleccionada en el mapa")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun identifiesWhenTheLocationComesFromTheGps() {
        setScreen(
            location = LocationResult.Success(latitude = -38.7396, longitude = -72.5984),
            locationSource = LocationSelectionSource.DEVICE
        )

        composeTestRule
            .onNodeWithText("Obtenida por GPS")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun disablesTheLocationActionsWhileTheFormIsNotEditable() {
        setScreen(
            submissionState = CreateReportSubmissionState.CreatingIncident
        )

        composeTestRule
            .onNodeWithText("Usar mi GPS actual")
            .performScrollTo()
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText("Elegir en el mapa")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    private fun setScreen(
        location: LocationResult = LocationResult.NotAvailable,
        locationSource: LocationSelectionSource = LocationSelectionSource.NONE,
        submissionState: CreateReportSubmissionState = CreateReportSubmissionState.Idle,
        onRefreshLocation: () -> Unit = {},
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
                    onRefreshLocation = onRefreshLocation,
                    onChooseLocationOnMap = onChooseLocationOnMap
                )
            }
        }
    }
}
