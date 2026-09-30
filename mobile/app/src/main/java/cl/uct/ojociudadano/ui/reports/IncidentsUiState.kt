package cl.uct.ojociudadano.ui.reports

import cl.uct.ojociudadano.data.model.Incident

sealed interface IncidentsUiState {

    data object Loading : IncidentsUiState

    data object Success : IncidentsUiState

    data object Empty : IncidentsUiState

    data class Error(
        val message: String
    ) : IncidentsUiState
}