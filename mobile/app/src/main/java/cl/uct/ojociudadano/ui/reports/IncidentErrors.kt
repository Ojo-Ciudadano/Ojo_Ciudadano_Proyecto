package cl.uct.ojociudadano.ui.reports

import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.SessionRequiredException

/** Never expose backend diagnostics (database addresses, stack traces) in citizen UI. */
fun Throwable.incidentMessage(): String = when (this) {
    is SessionRequiredException -> message.orEmpty()
    is ApiException.Http -> when (statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para consultar estos incidentes."
        404 -> "El incidente ya no está disponible."
        in 500..599 -> "El servicio de incidentes no está disponible (HTTP $statusCode). Intenta más tarde."
        else -> "No se pudo completar la consulta (HTTP $statusCode)."
    }
    is ApiException.Network, is java.io.IOException -> "No fue posible conectar de forma segura con el servidor. Revisa tu conexión e intenta nuevamente."
    else -> "La respuesta del servidor no se pudo interpretar. Intenta nuevamente."
}
