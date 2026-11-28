package cl.uct.ojociudadano.ui.notifications

import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.SessionRequiredException

/** Estado de la consulta de notificaciones del usuario autenticado. */
sealed interface NotificationsUiState {

    data object Loading : NotificationsUiState

    /** Hay notificaciones para mostrar; el listado conserva el orden de la capa de datos. */
    data object Success : NotificationsUiState

    /** El usuario todavía no tiene notificaciones: es un resultado válido, no un error. */
    data object Empty : NotificationsUiState

    /** Mensaje apto para el ciudadano; [canRetry] solo es cierto para fallos recuperables. */
    data class Error(
        val message: String,
        val canRetry: Boolean,
        val duringRefresh: Boolean = false
    ) : NotificationsUiState
}

/** Nunca expone diagnósticos del backend (stack traces, direcciones de base de datos). */
fun Throwable.notificationMessage(): String = when (this) {
    is SessionRequiredException -> message.orEmpty()
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para consultar tus notificaciones."
        in 500..599 -> "El servicio de notificaciones no está disponible (HTTP $statusCode). Intenta más tarde."
        else -> "No se pudo completar la consulta (HTTP $statusCode)."
    }
    is ApiException.Network, is java.io.IOException ->
        "No fue posible conectar de forma segura con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "La respuesta del servidor no se pudo interpretar. Intenta nuevamente."
}

/** Un fallo de autenticación o autorización no mejora reintentando con la misma sesión. */
fun Throwable.isNotificationRetryable(): Boolean = when (this) {
    is SessionRequiredException -> false
    is ApiException.Http -> statusCode !in setOf(401, 403)
    else -> true
}

/**
 * Mensaje de las acciones de marcado (#284): la consulta pudo haber funcionado
 * y solo falló la escritura, así que el texto habla de marcar, no de consultar.
 */
fun Throwable.notificationActionMessage(bulk: Boolean = false): String {
    val action = if (bulk) "marcar todas las notificaciones" else "marcar la notificación"
    val forbidden = if (bulk) "marcar tus notificaciones" else "modificar esta notificación"
    return when (this) {
        is SessionRequiredException -> message.orEmpty()
        is ApiException.Http -> when (statusCode) {
            401 -> "Tu sesión expiró. Inicia sesión nuevamente."
            403 -> "No tienes autorización para $forbidden."
            in 500..599 ->
                "No se pudo $action: el servicio no está disponible (HTTP $statusCode). Intenta más tarde."
            else -> "No se pudo $action (HTTP $statusCode)."
        }
        is ApiException.Network, is java.io.IOException ->
            "No fue posible conectar con el servidor. Revisa tu conexión e intenta nuevamente."
        else -> "No se pudo $action. Intenta nuevamente."
    }
}
