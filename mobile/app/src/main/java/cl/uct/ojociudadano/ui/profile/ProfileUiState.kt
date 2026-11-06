package cl.uct.ojociudadano.ui.profile

import cl.uct.ojociudadano.data.model.UserProfile
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.SessionRequiredException

/** Estado de la consulta del perfil del usuario autenticado. */
sealed interface ProfileUiState {

    data object Loading : ProfileUiState

    /** El perfil llegó desde `/usuarios/me`. */
    data class Success(val profile: UserProfile) : ProfileUiState

    /** Mensaje apto para el usuario; [canRetry] solo es cierto para fallos recuperables. */
    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : ProfileUiState
}

/** Nunca expone diagnósticos del backend (stack traces, direcciones de base de datos). */
fun Throwable.profileMessage(): String = when (this) {
    is SessionRequiredException -> message.orEmpty()
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para consultar este perfil."
        404 -> "No encontramos tu perfil en el sistema. Contacta al equipo de soporte si el problema persiste."
        in 500..599 -> "El servicio de perfiles no está disponible (HTTP $statusCode). Intenta más tarde."
        else -> "No se pudo completar la consulta del perfil (HTTP $statusCode)."
    }
    is ApiException.Network, is java.io.IOException ->
        "No fue posible conectar de forma segura con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "La respuesta del servidor no se pudo interpretar. Intenta nuevamente."
}

/** Una sesión vencida, sin permisos o un perfil inexistente no mejora reintentando. */
fun Throwable.isProfileRetryable(): Boolean = when (this) {
    is SessionRequiredException -> false
    is ApiException.Http -> statusCode !in setOf(401, 403, 404)
    else -> true
}
