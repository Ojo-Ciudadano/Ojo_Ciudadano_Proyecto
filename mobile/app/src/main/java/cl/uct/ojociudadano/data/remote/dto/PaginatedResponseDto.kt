package cl.uct.ojociudadano.data.remote.dto

data class PaginatedResponseDto<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)
