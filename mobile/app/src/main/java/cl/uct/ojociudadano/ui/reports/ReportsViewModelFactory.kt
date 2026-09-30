package cl.uct.ojociudadano.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.uct.ojociudadano.data.repository.IncidentRepository

class ReportsViewModelFactory(
    private val incidentRepository: IncidentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            return ReportsViewModel(incidentRepository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}