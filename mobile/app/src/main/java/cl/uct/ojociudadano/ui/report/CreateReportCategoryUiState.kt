package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category

data class CreateReportCategoryUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
