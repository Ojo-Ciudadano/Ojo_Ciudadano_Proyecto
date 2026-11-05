package cl.uct.ojociudadano.ui.reports.evidence

import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.session.SessionRequiredException
import java.io.IOException

/**
 * Traduce fallos de descarga de una evidencia a mensajes aptos para la
 * ciudadanía, sin exponer detalles internos del backend ni del storage.
 */
internal fun evidencePhotoErrorMessage(error: Throwable): String = when (error) {
    is SessionRequiredException -> "Tu sesión expiró. Inicia sesión nuevamente."

    is ApiException.Http -> when (error.statusCode) {
        401 -> "Tu sesión expiró. Inicia sesión nuevamente."
        403 -> "No tienes autorización para ver esta fotografía."
        404 -> "La fotografía ya no está disponible."
        503 -> "El almacenamiento de evidencias aún no está disponible."
        in 500..599 -> "El servicio de evidencias no está disponible temporalmente."
        else -> "No se pudo cargar la fotografía (HTTP ${error.statusCode})."
    }

    is ApiException.Network, is IOException ->
        "No fue posible conectar con el servidor. Revisa tu conexión e intenta nuevamente."

    else -> "No se pudo cargar la fotografía."
}
