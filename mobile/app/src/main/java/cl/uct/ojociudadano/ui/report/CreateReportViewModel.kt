package cl.uct.ojociudadano.ui.report

import androidx.lifecycle.ViewModel

class CreateReportViewModel : ViewModel() {

    fun validateForm(state: CreateReportFormState): CreateReportFormState {
        return state.copy(
            titleError = if (state.title.isBlank()) "El título es obligatorio" else null,
            descriptionError = if (state.description.isBlank()) "La descripción es obligatoria" else null,
            categoryError = if (state.categoryId.isNullOrBlank()) "La categoría es obligatoria" else null
        )
    }
}
