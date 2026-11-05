package cl.uct.ojociudadano.ui.location

import cl.uct.ojociudadano.data.location.LocationClient
import cl.uct.ojociudadano.domain.model.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var viewModel: LocationViewModel

    @Before
    fun setUp() {
        // Configuramos el despachador principal para corrutinas en tests
        Dispatchers.setMain(testDispatcher)
        fakeLocationClient = FakeLocationClient()
        viewModel = LocationViewModel(fakeLocationClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchCurrentLocation updates state to Success when location is available`() = runTest {
        // Given (Preparar escenario de éxito)
        val expectedLat = -38.7396
        val expectedLng = -72.5984
        fakeLocationClient.resultToReturn = LocationResult.Success(expectedLat, expectedLng)

        // When (Ejecutar acción)
        viewModel.fetchCurrentLocation()
        testScheduler.advanceUntilIdle() // Esperar a que la corrutina termine

        // Then (Verificar resultado)
        val currentState = viewModel.locationState.value
        assertEquals(LocationResult.Success(expectedLat, expectedLng), currentState)
    }

    @Test
    fun `fetchCurrentLocation updates state to MissingPermissions when permissions are absent`() = runTest {
        // Given
        fakeLocationClient.resultToReturn = LocationResult.MissingPermissions

        // When
        viewModel.fetchCurrentLocation()
        testScheduler.advanceUntilIdle()

        // Then
        val currentState = viewModel.locationState.value
        assertEquals(LocationResult.MissingPermissions, currentState)
    }

    @Test
    fun `fetchCurrentLocation updates state to NotAvailable when GPS is disabled`() = runTest {
        // Given
        fakeLocationClient.resultToReturn = LocationResult.NotAvailable

        // When
        viewModel.fetchCurrentLocation()
        testScheduler.advanceUntilIdle()

        // Then
        val currentState = viewModel.locationState.value
        assertEquals(LocationResult.NotAvailable, currentState)
    }

    @Test
    fun `fetchCurrentLocation updates state to Error when an exception occurs`() = runTest {
        // Given
        val errorMessage = "Error interno de GPS"
        fakeLocationClient.resultToReturn = LocationResult.Error(errorMessage)

        // When
        viewModel.fetchCurrentLocation()
        testScheduler.advanceUntilIdle()

        // Then
        val currentState = viewModel.locationState.value
        assertEquals(LocationResult.Error(errorMessage), currentState)
    }
}

/**
 * Cliente falso (Fake) para simular el comportamiento de DefaultLocationClient en los tests unitarios.
 */
class FakeLocationClient : LocationClient {
    var resultToReturn: LocationResult = LocationResult.NotAvailable

    override suspend fun getCurrentLocation(): LocationResult {
        return resultToReturn
    }
}