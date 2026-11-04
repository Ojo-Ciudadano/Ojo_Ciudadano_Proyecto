package cl.uct.ojociudadano.data.model

/** Resultado paginado expuesto por la capa de datos (sin filtrar tipos de red). */
data class Paginated<T>(
    val items: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)
