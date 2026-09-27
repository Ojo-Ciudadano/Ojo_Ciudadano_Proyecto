package cl.uct.ojociudadano.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.uct.ojociudadano.data.repository.CategoryRepository

class CreateReportViewModelFactory(
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateReportViewModel::class.java)) {
            return CreateReportViewModel(categoryRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
