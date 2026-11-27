package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.domain.model.Evidence
import coil3.key.Keyer
import coil3.request.Options

/**
 * Clave estable para reutilizar la misma evidencia entre listado y detalle.
 *
 * [scope] identifica a la sesión que descargó la imagen: así una cuenta no
 * reutiliza miniaturas decodificadas por otra aunque la caché de disco
 * sobreviva a un cierre incompleto (#280).
 */
class EvidenceImageKeyer(
    private val scope: () -> String = { "" }
) : Keyer<Evidence> {

    override fun key(data: Evidence, options: Options): String = cacheKey(data, scope())

    companion object {
        fun cacheKey(data: Evidence, scope: String = ""): String =
            if (scope.isBlank()) {
                "evidence:${data.id}"
            } else {
                "$scope:evidence:${data.id}"
            }
    }
}
