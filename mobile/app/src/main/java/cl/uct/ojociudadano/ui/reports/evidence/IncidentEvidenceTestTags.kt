package cl.uct.ojociudadano.ui.reports.evidence

/** Etiquetas estables para verificar la galería de evidencias en pruebas de UI. */
object IncidentEvidenceTestTags {
    const val GALLERY = "incident_evidence_gallery"
    const val EMPTY = "incident_evidence_empty"

    private const val PHOTO_PREFIX = "incident_evidence_photo_"
    private const val LOADING_PREFIX = "incident_evidence_loading_"
    private const val LOADED_PREFIX = "incident_evidence_loaded_"
    private const val ERROR_PREFIX = "incident_evidence_error_"
    private const val RETRY_PREFIX = "incident_evidence_retry_"

    fun photo(id: String): String = "$PHOTO_PREFIX$id"

    fun loading(id: String): String = "$LOADING_PREFIX$id"

    fun loaded(id: String): String = "$LOADED_PREFIX$id"

    fun error(id: String): String = "$ERROR_PREFIX$id"

    fun retry(id: String): String = "$RETRY_PREFIX$id"
}
