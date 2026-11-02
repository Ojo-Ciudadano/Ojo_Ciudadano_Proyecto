package cl.uct.ojociudadano.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.IncidentRepository

class CreateReportViewModelFactory(
    private val categoryRepository: CategoryRepository,
    private val incidentRepository: IncidentRepository,
    private val photoFileStore: EvidencePhotoFileStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateReportViewModel::class.java)) {
            return CreateReportViewModel(
                categoryRepository = categoryRepository,
                incidentRepository = incidentRepository,
                photoFileStore = photoFileStore
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
