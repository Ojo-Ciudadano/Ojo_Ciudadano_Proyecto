package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.domain.model.Evidence
import coil3.key.Keyer
import coil3.request.Options

/** Clave estable para reutilizar la misma evidencia entre listado y detalle. */
class EvidenceImageKeyer : Keyer<Evidence> {

    override fun key(data: Evidence, options: Options): String = cacheKey(data)

    companion object {
        fun cacheKey(data: Evidence): String = "evidence:${data.id}"
    }
}
