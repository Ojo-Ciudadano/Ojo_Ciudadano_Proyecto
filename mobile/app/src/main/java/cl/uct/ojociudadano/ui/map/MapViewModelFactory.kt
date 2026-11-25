package cl.uct.ojociudadano.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.domain.repository.LocationClient

class MapViewModelFactory(
    private val incidentRepository: IncidentRepository,
    private val locationClient: LocationClient = UnavailableLocationClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(
                incidentRepository = incidentRepository,
                locationClient = locationClient
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
