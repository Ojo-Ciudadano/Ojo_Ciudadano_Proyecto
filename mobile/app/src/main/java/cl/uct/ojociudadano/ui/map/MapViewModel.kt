package cl.uct.ojociudadano.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.ui.reports.incidentMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Obtiene los incidentes desde la capa de datos y los transforma en marcadores.
 *
 * La pantalla solo consume [uiState]; no conoce Retrofit ni el API Gateway.
 */
class MapViewModel(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadIncidents()
    }

    fun loadIncidents() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading

            try {
                val markers = incidentRepository.getIncidents().toMapMarkers()
                _uiState.value = if (markers.isEmpty()) {
                    MapUiState.Empty
                } else {
                    MapUiState.Success(markers)
                }
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
}
