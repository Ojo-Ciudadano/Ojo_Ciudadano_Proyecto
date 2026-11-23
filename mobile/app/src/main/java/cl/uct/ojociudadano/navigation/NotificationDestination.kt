package cl.uct.ojociudadano.navigation

import cl.uct.ojociudadano.data.session.SessionState

/**
 * Resultado de pulsar una notificación.
 *
 * Se modela aparte de la navegación para que la pantalla pueda decidir sin
 * repetir la validación del identificador.
 */
sealed interface NotificationClickOutcome {

    /** El incidente es utilizable: marcar como leída y navegar. */
    data class Open(val incidentId: String) : NotificationClickOutcome

    /** La notificación trae una referencia, pero no sirve como ruta. */
    data object InvalidIncident : NotificationClickOutcome

    /** No hay incidente asociado: la tarjeta no ofrece acción. */
    data object NoIncident : NotificationClickOutcome
}

/**
 * Un identificador es utilizable si no está vacío y no rompe la ruta
 * (`incident/{id}` o `admin/incidents/{id}`).
 */
fun isValidIncidentId(incidentId: String?): Boolean {
    val normalized = incidentId?.trim() ?: return false
    return normalized.isNotEmpty() && normalized.none { it == '/' || it == '?' || it == '#' }
}

fun notificationClickOutcome(incidentId: String?): NotificationClickOutcome {
    if (incidentId.isNullOrBlank()) return NotificationClickOutcome.NoIncident
    val normalized = incidentId.trim()
    return if (isValidIncidentId(normalized)) {
        NotificationClickOutcome.Open(normalized)
    } else {
        NotificationClickOutcome.InvalidIncident
    }
}

/**
 * Resuelve a qué detalle navegar desde una notificación.
 *
 * Devuelve `null` cuando la notificación no trae un incidente utilizable, para
 * que la tarjeta no ofrezca una acción que terminaría en una ruta inválida.
 * El destino respeta el rol de la sesión y reutiliza las pantallas existentes.
 */
fun notificationIncidentRoute(state: SessionState, incidentId: String?): String? {
    val normalized = incidentId?.trim()?.takeIf(::isValidIncidentId) ?: return null

    return when (resolveInitialDestination(state)) {
        InitialDestination.ADMINISTRATOR -> "admin/incidents/$normalized"
        else -> "incident/$normalized"
    }
}
