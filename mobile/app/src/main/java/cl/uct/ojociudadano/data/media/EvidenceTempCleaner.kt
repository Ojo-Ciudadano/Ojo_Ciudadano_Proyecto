package cl.uct.ojociudadano.data.media

import java.io.File

/**
 * Barre los temporales de evidencias que quedaron en la caché tras un cierre
 * abrupto, la muerte del proceso o un formulario abandonado (#280).
 *
 * Cubre el directorio de fotografías del formulario y el directorio de
 * compresión previa al envío. La limpieza es de mejor esfuerzo: un archivo que
 * no puede borrarse se conserva para el siguiente arranque.
 */
class EvidenceTempCleaner(
    private val evidencesDirectory: File,
    private val uploadsDirectory: File,
    private val clock: () -> Long = System::currentTimeMillis
) {

    /**
     * Elimina los archivos de ambos directorios más antiguos que
     * [maxAgeMillis]. Se usa al arrancar la app.
     *
     * @return la cantidad de archivos eliminados.
     */
    fun cleanupExpired(maxAgeMillis: Long = TEMP_EVIDENCE_MAX_AGE_MILLIS): Int =
        purgeExpired(evidencesDirectory, maxAgeMillis) +
            purgeExpired(uploadsDirectory, maxAgeMillis)

    /**
     * Elimina todos los archivos de ambos directorios, sin importar su
     * antigüedad. Se usa al descartar o completar el formulario.
     *
     * @return la cantidad de archivos eliminados.
     */
    fun purgeAll(): Int = purgeAll(evidencesDirectory) + purgeAll(uploadsDirectory)

    private fun purgeExpired(directory: File, maxAgeMillis: Long): Int {
        if (!directory.isDirectory) {
            return 0
        }

        val threshold = clock() - maxAgeMillis

        return directory.listFiles().orEmpty().count { file ->
            file.isFile &&
                file.lastModified() < threshold &&
                runCatching { file.delete() }.getOrDefault(false)
        }
    }

    private fun purgeAll(directory: File): Int {
        if (!directory.isDirectory) {
            return 0
        }

        return directory.listFiles().orEmpty().count { file ->
            file.isFile && runCatching { file.delete() }.getOrDefault(false)
        }
    }

    companion object {
        /** Directorio temporal del compresor previo al envío de evidencias. */
        const val UPLOADS_DIRECTORY_NAME = "evidence-upload"
    }
}
