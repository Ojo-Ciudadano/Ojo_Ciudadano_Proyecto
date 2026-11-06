package cl.uct.ojociudadano.ui.map

/**
 * Opción disponible para filtrar los incidentes del mapa.
 *
 * [value] es el identificador estable del atributo y [label] el texto que se
 * muestra al usuario.
 */
data class MapFilterOption(
    val value: String,
    val label: String
)

/**
 * Filtros aplicados localmente sobre los incidentes ya cargados en el mapa.
 *
 * Un valor nulo representa «todos». Los filtros nunca modifican la consulta al
 * backend: solo determinan qué marcadores de [MapUiState.Success.markers] se
 * representan.
 */
data class MapIncidentFilters(
    val categoryId: String? = null,
    val statusId: String? = null
) {
    val isEmpty: Boolean
        get() = categoryId == null && statusId == null

    /** Cantidad de criterios activos, usada para indicar filtros aplicados. */
    val activeCount: Int
        get() = listOfNotNull(categoryId, statusId).size

    /** Indica si el incidente representado por [marker] cumple ambos criterios. */
    fun matches(marker: IncidentMapMarker): Boolean {
        if (categoryId != null && marker.summary.categoryId != categoryId) return false
        if (statusId != null && marker.summary.statusId != statusId) return false
        return true
    }
}

/** Aplica [filters] conservando el orden original de los marcadores. */
fun List<IncidentMapMarker>.filteredBy(filters: MapIncidentFilters): List<IncidentMapMarker> =
    if (filters.isEmpty) this else filter(filters::matches)

/**
 * Descarta selecciones que ya no existen en el conjunto cargado.
 *
 * Se usa al recargar el mapa para mantener un estado coherente sin perder los
 * filtros que siguen siendo válidos.
 */
fun MapIncidentFilters.validatedAgainst(
    categories: List<MapFilterOption>,
    statuses: List<MapFilterOption>
): MapIncidentFilters = copy(
    categoryId = categoryId?.takeIf { id -> categories.any { it.value == id } },
    statusId = statusId?.takeIf { id -> statuses.any { it.value == id } }
)

/** Categorías disponibles en los incidentes cargados, sin duplicados. */
fun List<IncidentMapMarker>.availableCategoryOptions(): List<MapFilterOption> =
    mapNotNull { marker ->
        marker.summary.categoryId?.let { id ->
            MapFilterOption(value = id, label = marker.summary.categoryName ?: id)
        }
    }
        .distinctBy { it.value }
        .sortedBy { it.label.lowercase() }

/** Estados disponibles en los incidentes cargados, sin duplicados. */
fun List<IncidentMapMarker>.availableStatusOptions(): List<MapFilterOption> =
    mapNotNull { marker ->
        marker.summary.statusId?.let { id ->
            MapFilterOption(value = id, label = marker.summary.statusName ?: id)
        }
    }
        .distinctBy { it.value }
        .sortedBy { it.label.lowercase() }
