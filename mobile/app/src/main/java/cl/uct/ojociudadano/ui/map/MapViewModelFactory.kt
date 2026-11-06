package cl.uct.ojociudadano.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.uct.ojociudadano.data.repository.IncidentRepository

class MapViewModelFactory(
    private val incidentRepository: IncidentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(incidentRepository = incidentRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
