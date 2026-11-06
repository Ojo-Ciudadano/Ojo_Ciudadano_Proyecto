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
        val canRetry: Boolean
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
