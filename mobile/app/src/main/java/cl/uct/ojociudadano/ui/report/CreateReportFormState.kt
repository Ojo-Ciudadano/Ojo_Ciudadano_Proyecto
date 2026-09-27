package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category

data class CreateReportFormState(
    val title: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null
) {

    fun updateTitle(title: String): CreateReportFormState =
        copy(title = title, titleError = null)

    fun updateDescription(description: String): CreateReportFormState =
        copy(description = description, descriptionError = null)

    fun updateCategory(category: Category): CreateReportFormState =
        copy(selectedCategory = category, categoryError = null)
}
