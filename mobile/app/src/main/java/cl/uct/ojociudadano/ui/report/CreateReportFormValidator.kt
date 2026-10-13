package cl.uct.ojociudadano.ui.report

/**
 * Aplica las reglas locales del formulario antes de que exista un envío remoto.
 */
class CreateReportFormValidator {

    fun validate(state: CreateReportFormState): CreateReportFormState {
        return state.copy(
            titleError = state.title.requiredFieldError("El título es obligatorio"),
            descriptionError = state.description.requiredFieldError("La descripción es obligatoria"),
            categoryError = if (state.selectedCategory == null) "La categoría es obligatoria" else null
        )
    }
}

private fun String?.requiredFieldError(errorMessage: String): String? =
    if (isNullOrBlank()) errorMessage else null
