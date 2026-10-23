package cl.uct.ojociudadano.data.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import java.io.File
import java.io.IOException
import java.util.Locale
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

    override fun import(contentUri: String): IncidentPhotoEvidence {
        val uri = contentUri.toUri()
        val mimeType = try {
            resolveImageMimeType(uri)
        } catch (exception: PhotoImportException) {
            throw exception
        } catch (exception: Exception) {
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo acceder a la fotografía seleccionada",
                exception
            )
        }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: DEFAULT_EXTENSION

        val directory = try {
            ensureDirectory()
        } catch (exception: IOException) {
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo preparar el almacenamiento temporal de la fotografía",
                exception
            )
        }

        val fileName = "evidence_${clock()}_${UUID.randomUUID()}.$extension"
        val file = File(directory, fileName)

        try {
            val source = appContext.contentResolver.openInputStream(uri)
                ?: throw PhotoImportException(
                    PhotoImportFailure.UNREADABLE,
                    "No se pudo acceder a la fotografía seleccionada"
                )

            source.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!file.isFile || file.length() <= 0L) {
                throw PhotoImportException(
                    PhotoImportFailure.EMPTY_FILE,
                    "La fotografía seleccionada está vacía"
                )
            }

            val localUri = FileProvider.getUriForFile(appContext, authority, file)

            return IncidentPhotoEvidence(
                localUri = localUri.toString(),
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = file.length(),
                capturedAt = file.lastModified().takeIf { it > 0L } ?: clock()
            )
        } catch (exception: PhotoImportException) {
            file.delete()
            throw exception
        } catch (exception: SecurityException) {
            file.delete()
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo acceder a la fotografía seleccionada",
                exception
            )
        } catch (exception: IOException) {
            file.delete()
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo recuperar la fotografía seleccionada",
                exception
            )
        } catch (commonFailure: Throwable) {
            file.delete()
            throw commonFailure
        }
    }

    override fun delete(fileName: String) {
        runCatching { fileFor(fileName).delete() }
    }

    private fun resolveImageMimeType(uri: Uri): String {
        val declared = appContext.contentResolver.getType(uri)
            ?.lowercase(Locale.ROOT)
            ?.substringBefore(';')
            ?.takeUnless { it == IMAGE_WILDCARD_MIME_TYPE }

        if (declared != null && declared.startsWith(IMAGE_MIME_PREFIX)) {
            return declared
        }

        val inferred = mimeTypeFromDisplayName(uri)

        if (inferred != null && inferred.startsWith(IMAGE_MIME_PREFIX)) {
            return inferred
        }

        throw PhotoImportException(
            PhotoImportFailure.NOT_AN_IMAGE,
            "El archivo seleccionado no es una imagen"
        )
    }

    private fun mimeTypeFromDisplayName(uri: Uri): String? {
        val displayName = queryDisplayName(uri) ?: return null
        val extension = displayName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
            .takeIf { it.isNotEmpty() }
            ?: return null

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun queryDisplayName(uri: Uri): String? =
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
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

        private const val DEFAULT_EXTENSION = "jpg"
        private const val IMAGE_MIME_PREFIX = "image/"
        private const val IMAGE_WILDCARD_MIME_TYPE = "image/*"
    }
}
