package cl.uct.ojociudadano.data.media

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale

/**
 * Construye la parte multipart de una evidencia.
 *
 * Centraliza tres detalles que el backend valida y que antes vivían dispersos
 * en la capa de datos:
 * - el nombre del campo debe ser [FIELD_NAME] (`archivo`), tal como lo publica
 *   el contrato del endpoint;
 * - cada parte debe declarar el MIME real de la fotografía, porque Multer
 *   rechaza las que llegan como `application/octet-stream`;
 * - el nombre de archivo debe llevar una extensión coherente con ese MIME.
 */
object EvidenceMultipartFactory {

    /** Campo multipart/form-data que espera el endpoint de evidencias. */
    const val FIELD_NAME = "archivo"

    private val EXTENSIONS = mapOf(
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp"
    )

    private val UNSAFE_FILE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

    /** Normaliza el MIME a minúsculas y sin parámetros (`; charset=...`). */
    fun normalizedMimeType(mimeType: String): String =
        mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)

    /** Extensión asociada al MIME, o `null` si el backend no lo acepta. */
    fun extensionFor(mimeType: String): String? =
        EXTENSIONS[normalizedMimeType(mimeType)]

    /**
     * Nombre de archivo seguro y con extensión coherente.
     *
     * Se descarta cualquier extensión previa para no producir nombres como
     * `foto.png.jpg` cuando la compresión cambia el formato.
     */
    fun fileNameFor(baseName: String?, mimeType: String): String {
        val extension = extensionFor(mimeType)
            ?: throw IllegalArgumentException(
                "Formato de archivo no soportado por el backend. Use JPEG, PNG o WebP"
            )

        val safeBase = baseName ?: DEFAULT_BASE_NAME
        val sanitized = safeBase
            .substringBeforeLast('.', safeBase)
            .replace(UNSAFE_FILE_NAME_CHARS, "_")
            .trim('_', '.')
            .ifBlank { DEFAULT_BASE_NAME }

        return "$sanitized.$extension"
    }

    /**
     * Crea la parte multipart de [file] para el campo [FIELD_NAME].
     *
     * @throws IllegalArgumentException si el archivo está vacío o el MIME no
     * es uno de los aceptados por el backend.
     */
    fun create(file: File, mimeType: String): MultipartBody.Part {
        require(file.isFile && file.length() > 0L) {
            "La evidencia está vacía o no es legible"
        }

        val normalizedMime = normalizedMimeType(mimeType)
        val mediaType = normalizedMime.toMediaTypeOrNull()
            ?: throw IllegalArgumentException(
                "El tipo MIME de la evidencia no es válido: $mimeType"
            )
        if (extensionFor(normalizedMime) == null) {
            throw IllegalArgumentException(
                "Formato de archivo no soportado por el backend. Use JPEG, PNG o WebP"
            )
        }

        val requestBody = file.asRequestBody(mediaType)
        val fileName = fileNameFor(file.name, normalizedMime)

        return MultipartBody.Part.createFormData(FIELD_NAME, fileName, requestBody)
    }

    private const val DEFAULT_BASE_NAME = "evidence"
}
