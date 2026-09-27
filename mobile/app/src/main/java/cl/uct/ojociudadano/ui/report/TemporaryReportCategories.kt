package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category

/**
 * Opciones locales temporales para el formulario. Deben reemplazarse por las
 * categorías entregadas por el repositorio cuando se integre esa funcionalidad.
 */
object TemporaryReportCategories {

    val options = listOf(
        Category(
            id = "temporary-public-lighting",
            name = "Alumbrado público",
            description = "",
            basePriority = 0,
            isCriticalEmergency = false,
            officialContactChannel = null
        ),
        Category(
            id = "temporary-waste",
            name = "Basura",
            description = "",
            basePriority = 0,
            isCriticalEmergency = false,
            officialContactChannel = null
        ),
        Category(
            id = "temporary-streets-and-sidewalks",
            name = "Calles y veredas",
            description = "",
            basePriority = 0,
            isCriticalEmergency = false,
            officialContactChannel = null
        ),
        Category(
            id = "temporary-green-areas",
            name = "Áreas verdes",
            description = "",
            basePriority = 0,
            isCriticalEmergency = false,
            officialContactChannel = null
        ),
        Category(
            id = "temporary-other",
            name = "Otros",
            description = "",
            basePriority = 0,
            isCriticalEmergency = false,
            officialContactChannel = null
        )
    )
}
