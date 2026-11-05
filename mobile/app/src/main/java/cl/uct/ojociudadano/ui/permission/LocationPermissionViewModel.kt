package cl.uct.ojociudadano.ui.permission

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LocationPermissionUiState {
    data object Idle : LocationPermissionUiState
    data object Granted : LocationPermissionUiState
    data object Denied : LocationPermissionUiState
    data object PermanentlyDenied : LocationPermissionUiState
}

class LocationPermissionViewModel : ViewModel() {

    private val _permissionState = MutableStateFlow<LocationPermissionUiState>(LocationPermissionUiState.Idle)
    val permissionState: StateFlow<LocationPermissionUiState> = _permissionState.asStateFlow()

    fun updatePermissionState(isGranted: Boolean, permanentlyDenied: Boolean = false) {
        _permissionState.value = when {
            isGranted -> LocationPermissionUiState.Granted
            permanentlyDenied -> LocationPermissionUiState.PermanentlyDenied
            else -> LocationPermissionUiState.Denied
        }
    }
}