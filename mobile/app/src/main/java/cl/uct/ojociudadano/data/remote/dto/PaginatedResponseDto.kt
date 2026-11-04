package cl.uct.ojociudadano.data.remote.dto

/**
 * Contrato de paginación del backend (`GET /usuarios`): `{ data, meta }`.
 * No confundir con `{ items, total, page, totalPages }`.
 */
data class PaginatedResponseDto<T>(
    val data: List<T>,
    val meta: PaginationMetaDto
)

data class PaginationMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)
