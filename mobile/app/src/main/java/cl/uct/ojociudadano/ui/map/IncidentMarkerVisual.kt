package cl.uct.ojociudadano.ui.map

import java.text.Normalizer
import java.util.Locale

/**
 * Estado del incidente interpretado para la representación del marcador.
 *
 * El catálogo real del backend no se modifica: [backendName] conserva el texto
 * recibido y se usa para resolver el color del pin mediante los tonos ya
 * definidos por el sistema de diseño. [UNKNOWN] cubre respuestas parciales o
 * valores que no pertenecen al catálogo conocido.
 */
enum class IncidentMarkerStatus(val backendName: String, val legendLabel: String) {
    NEW("Nuevo", "Nuevo"),
    IN_REVIEW("En revisión", "En revisión"),
    IN_ATTENTION("En atención", "En atención"),
    RESOLVED("Resuelto", "Resuelto"),
    CLOSED("Cerrado", "Cerrado"),
    UNKNOWN("", "Sin estado");

    companion object {
        /** Estados que se muestran en la leyenda del mapa, en orden de avance. */
        val legendEntries: List<IncidentMarkerStatus> = listOf(
            NEW,
            IN_REVIEW,
            IN_ATTENTION,
            RESOLVED,
            CLOSED
        )

        /** Interpreta el nombre del estado sin distinguir mayúsculas ni tildes. */
        fun fromBackendName(name: String?): IncidentMarkerStatus {
            val normalized = name.normalizedForComparison()
            return when (normalized) {
                "nuevo" -> NEW
                "en revision" -> IN_REVIEW
                "en atencion" -> IN_ATTENTION
                "resuelto" -> RESOLVED
                "cerrado" -> CLOSED
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Nivel de prioridad del incidente (1 mínima … 5 crítica).
 *
 * [glyph] es el símbolo que se dibuja dentro del pin. [UNASSIGNED] conserva los
 * casos sin prioridad registrada, incluidos valores fuera del rango del API.
 */
enum class IncidentMarkerPriority(
    val level: Int?,
    val legendLabel: String,
    val glyph: String
) {
    MINIMUM(1, "Mínima", "1"),
    LOW(2, "Baja", "2"),
    MEDIUM(3, "Media", "3"),
    HIGH(4, "Alta", "4"),
    CRITICAL(5, "Crítica", "5"),
    UNASSIGNED(null, "Sin prioridad", "–");

    companion object {
        /** Niveles que se muestran en la leyenda del mapa, en orden ascendente. */
        val legendEntries: List<IncidentMarkerPriority> = listOf(
            MINIMUM,
            LOW,
            MEDIUM,
            HIGH,
            CRITICAL
        )

        fun fromLevel(level: Int?): IncidentMarkerPriority =
            entries.firstOrNull { it.level != null && it.level == level } ?: UNASSIGNED
    }
}

/**
 * Apariencia que debe tener un marcador del mapa.
 *
 * El color del pin representa el estado y el glifo central representa la
 * prioridad. [selected] agrega el contorno de selección sin cambiar el
 * significado del marcador.
 */
data class IncidentMarkerVisual(
    val status: IncidentMarkerStatus,
    val priority: IncidentMarkerPriority,
    val selected: Boolean = false
) {
    /** Devuelve la misma apariencia con el estado de selección indicado. */
    fun withSelection(selected: Boolean): IncidentMarkerVisual =
        if (this.selected == selected) this else copy(selected = selected)
}

/** Deriva la apariencia del marcador desde el resumen ya cargado del incidente. */
fun IncidentMapSummary.toMarkerVisual(selected: Boolean = false): IncidentMarkerVisual =
    IncidentMarkerVisual(
        status = IncidentMarkerStatus.fromBackendName(statusName),
        priority = IncidentMarkerPriority.fromLevel(priorityLevel),
        selected = selected
    )

/** Apariencia del marcador, incluido su estado de selección actual. */
fun IncidentMapMarker.visual(selected: Boolean = false): IncidentMarkerVisual =
    summary.toMarkerVisual(selected)

private fun String?.normalizedForComparison(): String {
    val lowerCase = this?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .trim()
}

private val combiningMarks = Regex("\\p{M}+")
