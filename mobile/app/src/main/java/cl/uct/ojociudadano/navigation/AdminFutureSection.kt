package cl.uct.ojociudadano.navigation

/** Secciones previstas en docs/CAMBIO_ROLES.md, todavía sin implementación Android. */
enum class AdminFutureSection(val id: String, val title: String, val description: String) {
    USERS("users", "Gestión de usuarios", "Administración de usuarios y sus roles en la plataforma."),
    STATISTICS("statistics", "Estadísticas", "Consulta de estadísticas para apoyar la gestión de incidentes."),
    REPORTS("reports", "Reportes", "Consulta de reportes para el seguimiento administrativo."),
    MODERATION("moderation", "Moderación de contenido", "Revisión y moderación del contenido de la plataforma."),
    SETTINGS("settings", "Configuración administrativa", "Configuración del sistema Ojo Ciudadano.");

    val route get() = "admin/coming-soon/$id"

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id }
    }
}
