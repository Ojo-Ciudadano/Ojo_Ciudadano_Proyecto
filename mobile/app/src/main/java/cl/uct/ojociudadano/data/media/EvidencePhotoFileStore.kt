package cl.uct.ojociudadano.data.media

import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence

/**
 * Gestiona los archivos temporales de las fotografías de evidencia.
 *
 * No conoce la capa de red: únicamente reserva, completa y elimina archivos
 * locales mientras el formulario permanece abierto.
 */
interface EvidencePhotoFileStore {

    /**
     * Reserva un archivo vacío y entrega la URI con la que la cámara escribirá
     * la fotografía capturada.
     */
    fun create(): PendingPhotoCapture

    /**
     * Construye la evidencia a partir del archivo escrito por la cámara.
     *
     * @return la evidencia recuperada, o `null` si la cámara no escribió
     * contenido en el archivo reservado.
     */
    fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence?

    /**
     * Elimina el archivo temporal identificado por [fileName].
     */
    fun delete(fileName: String)
}
