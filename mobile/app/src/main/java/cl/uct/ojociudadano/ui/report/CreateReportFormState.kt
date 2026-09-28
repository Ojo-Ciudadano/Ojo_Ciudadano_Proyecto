package cl.uct.ojociudadano.ui.report

data class CreateReportFormState(
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val categoryName: String = "",
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null
)
