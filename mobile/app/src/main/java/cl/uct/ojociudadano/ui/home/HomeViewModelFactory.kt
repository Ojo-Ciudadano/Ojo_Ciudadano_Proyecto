package cl.uct.ojociudadano.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.uct.ojociudadano.data.repository.IncidentRepository

class HomeViewModelFactory (
    private val incidentRepository: IncidentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(incidentRepository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}