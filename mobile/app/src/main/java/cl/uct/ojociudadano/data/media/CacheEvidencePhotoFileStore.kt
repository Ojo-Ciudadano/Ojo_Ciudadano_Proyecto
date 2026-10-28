package cl.uct.ojociudadano.data.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
    private val maxImportBytes: Long = MAX_IMPORT_BYTES,
    private val clock: () -> Long = System::currentTimeMillis
) : EvidencePhotoFileStore {

    init {
        require(maxImportBytes > 0L) { "El tamaño máximo de importación debe ser positivo" }
    }

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

    override suspend fun import(contentUri: String): IncidentPhotoEvidence {
        val uri = contentUri.toUri()

        EvidencePhotoContentCopier.requireDeclaredSizeWithinLimit(
            declaredSize = queryDeclaredSize(uri),
            maxBytes = maxImportBytes
        )

        val directory = try {
            ensureDirectory()
        } catch (exception: IOException) {
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo preparar el almacenamiento temporal de la fotografía",
                exception
            )
        }

        val baseName = "evidence_${clock()}_${UUID.randomUUID()}"
        val temporaryFile = File(directory, "$baseName.$PARTIAL_EXTENSION")
        var finalFile: File? = null

        try {
            val source = appContext.contentResolver.openInputStream(uri)
                ?: throw PhotoImportException(
                    PhotoImportFailure.UNREADABLE,
                    "No se pudo acceder a la fotografía seleccionada"
                )

            source.use { input ->
                temporaryFile.outputStream().use { output ->
                    EvidencePhotoContentCopier.copy(
                        input = input,
                        output = output,
                        maxBytes = maxImportBytes
                    )
                }
            }

            if (!temporaryFile.isFile || temporaryFile.length() <= 0L) {
                throw PhotoImportException(
                    PhotoImportFailure.EMPTY_FILE,
                    "La fotografía seleccionada está vacía"
                )
            }

            val content = ImageContentInspector.inspect(temporaryFile)
            val destination = File(directory, "$baseName.${content.extension}")

            if (!temporaryFile.renameTo(destination)) {
                throw PhotoImportException(
                    PhotoImportFailure.UNREADABLE,
                    "No se pudo recuperar la fotografía seleccionada"
                )
            }

            finalFile = destination
            val localUri = FileProvider.getUriForFile(appContext, authority, destination)

            return IncidentPhotoEvidence(
                localUri = localUri.toString(),
                fileName = destination.name,
                mimeType = content.mimeType,
                sizeBytes = destination.length(),
                capturedAt = destination.lastModified().takeIf { it > 0L } ?: clock()
            )
        } catch (exception: PhotoImportException) {
            temporaryFile.delete()
            finalFile?.delete()
            throw exception
        } catch (exception: SecurityException) {
            temporaryFile.delete()
            finalFile?.delete()
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo acceder a la fotografía seleccionada",
                exception
            )
        } catch (exception: IOException) {
            temporaryFile.delete()
            finalFile?.delete()
            throw PhotoImportException(
                PhotoImportFailure.UNREADABLE,
                "No se pudo recuperar la fotografía seleccionada",
                exception
            )
        } catch (commonFailure: Throwable) {
            temporaryFile.delete()
            finalFile?.delete()
            throw commonFailure
        }
    }

    override fun exists(fileName: String): Boolean =
        runCatching {
            val file = fileFor(fileName)
            file.isFile && file.length() > 0L
        }.getOrDefault(false)

    override fun delete(fileName: String) {
        runCatching { fileFor(fileName).delete() }
    }

    override fun cleanupExpired(maxAgeMillis: Long): Int {
        val directory = evidenceDirectory

        if (!directory.isDirectory) {
            return 0
        }

        val expirationThreshold = clock() - maxAgeMillis

        return directory.listFiles()
            ?.count { file ->
                file.isFile &&
                    file.lastModified() < expirationThreshold &&
                    runCatching { file.delete() }.getOrDefault(false)
            }
            ?: 0
    }

    /**
     * Devuelve el tamaño informado por el proveedor, o `null` cuando no está
     * disponible. El límite real se sigue aplicando durante la copia.
     */
    private fun queryDeclaredSize(uri: Uri): Long? =
        runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex).takeIf { it > 0L }
                } else {
                    null
                }
            }
        }.getOrNull()

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
        const val MAX_IMPORT_BYTES = 20L * 1024L * 1024L

        private const val PARTIAL_EXTENSION = "part"
    }
}
