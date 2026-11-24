package cl.uct.ojociudadano.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.domain.repository.LocationClient
import cl.uct.ojociudadano.ui.reports.incidentMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Obtiene los incidentes desde la capa de datos y los transforma en marcadores.
 *
 * La pantalla solo consume [uiState]; no conoce Retrofit ni el API Gateway.
 * Los filtros de categoría y estado se aplican en memoria sobre los marcadores
 * cargados, por lo que cambiar una selección no realiza consultas adicionales.
 *
 * La ubicación del usuario se mantiene fuera de [uiState]: centrar la cámara es
 * una acción independiente de la carga de incidentes y no debe ocultar los
 * estados de carga o error del mapa.
 */
class MapViewModel(
    private val incidentRepository: IncidentRepository,
    private val locationClient: LocationClient = UnavailableLocationClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _userLocation = MutableStateFlow<MapUserLocation?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _isLocatingUser = MutableStateFlow(false)
    val isLocatingUser = _isLocatingUser.asStateFlow()

    /**
     * Mensajes de una sola lectura para informar fallos de ubicación sin
     * reemplazar el contenido del mapa.
     */
    private val userMessageChannel = Channel<String>(Channel.BUFFERED)
    val userMessages = userMessageChannel.receiveAsFlow()

    private var loadJob: Job? = null
    private var locationJob: Job? = null
    private var filters = MapIncidentFilters()

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        val previous = _uiState.value as? MapUiState.Success
        val previousSelectedId = previous?.selectedIncidentId
        val previousFiltersVisible = previous?.filtersVisible ?: false
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading

            try {
                val markers = incidentRepository.getIncidents().toMapMarkers()
                if (markers.isEmpty()) {
                    _uiState.value = MapUiState.Empty
                    return@launch
                }

                filters = filters.validatedAgainst(
                    categories = markers.availableCategoryOptions(),
                    statuses = markers.availableStatusOptions()
                )
                val success = MapUiState.Success(
                    markers = markers,
                    selectedIncidentId = previousSelectedId,
                    filters = filters,
                    filtersVisible = previousFiltersVisible
                )
                _uiState.value = success.copy(
                    selectedIncidentId = previousSelectedId?.takeIf { selectedId ->
                        success.visibleMarkers.any { it.incidentId == selectedId }
                    }
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                runCatching {
                    Log.e(
                        "MapViewModel",
                        "Error al cargar incidentes del mapa: " +
                            "type=${exception::class.java.name}, " +
                            "message=${exception.message}, " +
                            "cause=${exception.cause}",
                        exception
                    )
                }
                _uiState.value = MapUiState.Error(exception.incidentMessage())
            }
        }
    }

    /** Filtra por categoría; [categoryId] nulo vuelve a mostrar todas las categorías. */
    fun selectCategory(categoryId: String?) {
        updateFilters { current -> current.copy(categoryId = categoryId) }
    }

    /** Filtra por estado; [statusId] nulo vuelve a mostrar todos los estados. */
    fun selectStatus(statusId: String?) {
        updateFilters { current -> current.copy(statusId = statusId) }
    }

    /** Restablece todos los filtros y vuelve a representar los incidentes cargados. */
    fun clearFilters() {
        updateFilters { MapIncidentFilters() }
    }

    /**
     * Muestra u oculta los controles de filtrado sin modificar los criterios
     * seleccionados ni realizar nuevas consultas.
     */
    fun setFiltersVisible(visible: Boolean) {
        val current = _uiState.value as? MapUiState.Success ?: return
        if (current.filtersVisible == visible) return

        _uiState.value = current.copy(filtersVisible = visible)
    }

    /**
     * Marca como seleccionado el incidente representado por [incidentId].
     *
     * La información resumida ya vive en [MapUiState.Success.markers], por lo
     * que seleccionar un marcador no dispara consultas adicionales. Solo se
     * pueden seleccionar marcadores visibles con los filtros vigentes.
     */
    fun selectIncident(incidentId: String) {
        val current = _uiState.value as? MapUiState.Success ?: return
        if (current.visibleMarkers.none { it.incidentId == incidentId }) return

        _uiState.value = current.copy(selectedIncidentId = incidentId)
    }

    /** Limpia la selección actual sin modificar los marcadores cargados. */
    fun clearSelection() {
        val current = _uiState.value as? MapUiState.Success ?: return
        if (current.selectedIncidentId == null) return

        _uiState.value = current.copy(selectedIncidentId = null)
    }

    /**
     * Solicita la ubicación GPS actual y publica una nueva petición de enfoque
     * cuando la lectura es válida.
     *
     * Solo se admite una consulta simultánea. Un permiso denegado, el GPS
     * desactivado o una lectura inválida se informan mediante [userMessages] y
     * no modifican los incidentes ni el estado de la cámara.
     */
    fun centerOnUserLocation() {
        if (locationJob?.isActive == true) return

        locationJob = viewModelScope.launch {
            _isLocatingUser.value = true
            try {
                val result = try {
                    locationClient.getCurrentLocation()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    LocationResult.Error(
                        exception.message ?: MapLocationErrors.UNKNOWN
                    )
                }

                when (result) {
                    is LocationResult.Success -> {
                        val coordinate = GeoCoordinate(
                            latitude = result.latitude,
                            longitude = result.longitude
                        )
                        if (coordinate.isValid) {
                            val nextFocusRequestId =
                                (_userLocation.value?.focusRequestId ?: 0L) + 1L
                            _userLocation.value = MapUserLocation(
                                coordinate = coordinate,
                                focusRequestId = nextFocusRequestId
                            )
                        } else {
                            userMessageChannel.trySend(
                                MapLocationErrors.INVALID_COORDINATES
                            )
                        }
                    }

                    else -> userMessageChannel.trySend(
                        MapLocationErrors.messageFor(result)
                            ?: MapLocationErrors.UNKNOWN
                    )
                }
            } finally {
                _isLocatingUser.value = false
            }
        }
    }

    /**
     * Informa que el usuario no concedió el permiso de ubicación sin cerrar ni
     * vaciar el mapa.
     */
    fun onLocationPermissionDenied() {
        userMessageChannel.trySend(MapLocationErrors.MISSING_PERMISSIONS)
    }

    private inline fun updateFilters(
        transform: (MapIncidentFilters) -> MapIncidentFilters
    ) {
        val current = _uiState.value as? MapUiState.Success ?: return
        val next = transform(filters).validatedAgainst(
            categories = current.categoryOptions,
            statuses = current.statusOptions
        )
        if (next == filters) return

        filters = next
        val updated = current.copy(filters = next)
        _uiState.value = updated.copy(
            selectedIncidentId = updated.selectedIncidentId?.takeIf { selectedId ->
                updated.visibleMarkers.any { it.incidentId == selectedId }
            }
        )
    }
}

/**
 * Implementación de respaldo para construcciones que no necesitan ubicación,
 * como las pruebas existentes del mapa. La aplicación siempre inyecta el GPS
 * real desde [cl.uct.ojociudadano.di.AppContainer].
 */
internal object UnavailableLocationClient : LocationClient {
    override suspend fun getCurrentLocation(): LocationResult =
        LocationResult.NotAvailable
}
