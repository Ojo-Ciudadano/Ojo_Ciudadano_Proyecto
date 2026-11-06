package cl.uct.ojociudadano.data.model

/**
 * Filtros opcionales para la consulta administrativa de incidentes.
 *
 * Cada campo nulo se omite de la petición (Retrofit descarta los `@Query` nulos),
 * de modo que al backend solo se envían los criterios realmente seleccionados.
 */
data class IncidentFilters(
    val estadoId: String? = null,
    val categoriaId: String? = null,
    val comuna: String? = null
) {
    val isEmpty: Boolean
        get() = estadoId == null && categoriaId == null && comuna == null
}
