package cl.uct.ojociudadano.data.media

import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import java.io.File

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
     * Copia a la caché temporal una fotografía existente en el dispositivo.
     *
     * La URI entregada por un selector de contenido suele tener un permiso de
     * lectura temporal. Al copiar su contenido, la evidencia deja de depender
     * de ese permiso y puede reutilizarse mientras se completa el formulario.
     *
     * La copia se interrumpe si el contenido supera el tamaño máximo permitido,
     * incluso cuando el proveedor no informa el tamaño de antemano. El formato
     * se determina a partir del contenido real del archivo, no del MIME
     * declarado por el proveedor.
     *
     * @param contentUri URI `content://` de la fotografía seleccionada.
     * @return la evidencia asociada al archivo copiado.
     * @throws PhotoImportException si el contenido no es una imagen válida,
     * está vacío, es demasiado grande o no puede leerse.
     */
    suspend fun import(contentUri: String): IncidentPhotoEvidence

    /**
     * Indica si el archivo temporal identificado por [fileName] sigue disponible
     * y contiene datos.
     *
     * Se usa para detectar referencias fotográficas que dejaron de ser válidas
     * mientras el formulario permanece abierto.
     */
    fun exists(fileName: String): Boolean

    /**
     * Entrega el archivo temporal identificado por [fileName] para que la capa
     * de datos pueda subirlo al backend.
     *
     * @return el archivo si sigue disponible y contiene datos, o `null` si ya
     * no existe, está vacío o no es legible.
     */
    fun file(fileName: String): File?

    /**
     * Elimina el archivo temporal identificado por [fileName].
     */
    fun delete(fileName: String)

    /**
     * Elimina las evidencias temporales más antiguas que [maxAgeMillis].
     *
     * Se usa para limpiar archivos huérfanos que quedaron en la caché después
     * de un cierre abrupto o de la muerte del proceso. La limpieza es de mejor
     * esfuerzo: si un archivo no puede borrarse, se conserva.
     *
     * @return la cantidad de archivos eliminados.
     */
    fun cleanupExpired(
        maxAgeMillis: Long = TEMP_EVIDENCE_MAX_AGE_MILLIS
    ): Int
}

/**
 * Antigüedad a partir de la cual una evidencia temporal se considera huérfana.
 *
 * Se mantiene en 24 horas para no borrar archivos recientes que podrían
 * pertenecer a una captura o selección todavía en curso.
 */
const val TEMP_EVIDENCE_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
