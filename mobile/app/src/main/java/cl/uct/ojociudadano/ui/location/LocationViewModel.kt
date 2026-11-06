package cl.uct.ojociudadano.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.domain.repository.LocationClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationClient: LocationClient
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationResult>(LocationResult.NotAvailable)
    val locationState: StateFlow<LocationResult> = _locationState.asStateFlow()

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = locationClient.getCurrentLocation()
        }
    }
}