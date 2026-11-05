package cl.uct.ojociudadano.data.model

/** Canonical names and rules from docs/MATRIZ_TRANSICIONES_INCIDENTES.md (#116). */
object IncidentStatusTransitions {
    private val destinations = mapOf(
        "Nuevo" to listOf("En revisión"),
        "En revisión" to listOf("En atención"),
        "En atención" to listOf("Resuelto"),
        "Resuelto" to listOf("En atención", "Cerrado"),
        "Cerrado" to emptyList()
    )

    fun availableFrom(current: String): List<String> = destinations[current].orEmpty()
    fun isCanonical(status: String): Boolean = status in destinations
}
