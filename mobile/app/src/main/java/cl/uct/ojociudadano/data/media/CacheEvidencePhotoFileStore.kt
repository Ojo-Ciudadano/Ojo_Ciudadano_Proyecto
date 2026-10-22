package cl.uct.ojociudadano.data.media

import android.content.Context
import androidx.core.content.FileProvider
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Implementación que escribe las fotografías en la caché privada de la app.
 *
 * Los archivos son temporales y se comparten con la aplicación de cámara a
 * través de [FileProvider], por lo que no se requieren permisos de
 * almacenamiento ni se persiste evidencia de forma definitiva.
 */
class CacheEvidencePhotoFileStore(
    context: Context,
    private val authority: String = "${context.packageName}.fileprovider",
    private val directoryName: String = EVIDENCE_DIRECTORY_NAME,
    private val clock: () -> Long = System::currentTimeMillis
) : EvidencePhotoFileStore {

    private val appContext = context.applicationContext

    private val evidenceDirectory: File
        get() = File(appContext.cacheDir, directoryName)

    override fun create(): PendingPhotoCapture {
        val directory = ensureDirectory()
        val fileName = "evidence_${clock()}_${UUID.randomUUID()}.jpg"
        val file = File(directory, fileName)

        if (!file.createNewFile()) {
            throw IOException("No fue posible preparar el archivo de la fotografía")
        }

        val contentUri = FileProvider.getUriForFile(appContext, authority, file)

        return PendingPhotoCapture(
            localUri = contentUri.toString(),
            fileName = fileName,
            mimeType = JPEG_MIME_TYPE
        )
    }

    override fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence? {
        val file = fileFor(pending.fileName)

        if (!file.isFile || file.length() <= 0L) {
            return null
        }

        return pending.toEvidence(
            sizeBytes = file.length(),
            capturedAt = file.lastModified().takeIf { it > 0L } ?: clock()
        )
    }

    override fun delete(fileName: String) {
        runCatching { fileFor(fileName).delete() }
    }

    private fun ensureDirectory(): File {
        val directory = evidenceDirectory

        if (directory.isDirectory) {
            return directory
        }

        if (!directory.mkdirs()) {
            throw IOException("No fue posible preparar el directorio de la fotografía")
        }

        return directory
    }

    private fun fileFor(fileName: String): File =
        File(evidenceDirectory, File(fileName).name)

    companion object {
        const val EVIDENCE_DIRECTORY_NAME = "evidences"
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
