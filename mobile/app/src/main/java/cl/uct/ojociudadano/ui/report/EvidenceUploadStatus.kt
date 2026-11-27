package cl.uct.ojociudadano.ui.report

/**
 * Estado de subida de una fotografía dentro del formulario de reporte.
 *
 * El estado vive en el ViewModel y se asocia a cada evidencia por su
 * `fileName`, de modo que la rejilla pueda mostrar qué fotografías ya se
 * subieron y cuáles siguen pendientes sin depender del estado global del envío.
 */
enum class EvidenceUploadStatus {

    /** La fotografía pertenece al formulario y aún no se ha intentado subir. */
    PENDING,

    /** La petición de la fotografía está en curso. */
    UPLOADING,

    /** El backend confirmó la evidencia; el archivo local sigue disponible. */
    COMPLETED,

    /** La subida falló y la fotografía puede reintentarse. */
    ERROR;

    /**
     * Indica si la fotografía todavía puede eliminarse del formulario.
     *
     * Una evidencia en curso o ya confirmada no debe desaparecer de la rejilla:
     * la primera quedaría en un estado inconsistente y la segunda ya existe en
     * el backend, por lo que solo se limpia al finalizar el flujo.
     */
    val canRemove: Boolean
        get() = this == PENDING || this == ERROR
}
