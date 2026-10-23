package cl.uct.ojociudadano.data.media

import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.util.Locale

/**
 * Contenido real de una imagen importada.
 */
internal data class ImageContent(
    val mimeType: String,
    val extension: String
)

/**
 * Valida que un archivo contenga una imagen real y determina su MIME.
 *
 * La validación combina la firma de los bytes iniciales con la decodificación
 * de las dimensiones mediante [BitmapFactory]. Así se rechaza tanto contenido
 * que no es una imagen como archivos corruptos que solo declaran un MIME
 * válido o usan una extensión de imagen.
 */
internal object ImageContentInspector {

    private const val HEADER_SIZE = 16

    fun inspect(file: File): ImageContent {
        val signature = detectImageSignature(file.readHeader())
            ?: throw PhotoImportException(
                PhotoImportFailure.NOT_AN_IMAGE,
                "El archivo seleccionado no es una imagen"
            )

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val decoded = try {
            BitmapFactory.decodeFile(file.absolutePath, options)
            true
        } catch (exception: Exception) {
            false
        }

        if (!decoded || options.outWidth <= 0 || options.outHeight <= 0) {
            if (
                signature.canFallbackWhenPlatformCannotDecode &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            ) {
                return ImageContent(
                    mimeType = signature.mimeType,
                    extension = signature.extension
                )
            }

            throw PhotoImportException(
                PhotoImportFailure.INVALID_IMAGE,
                "La fotografía seleccionada no contiene una imagen válida"
            )
        }

        return contentForMimeType(options.outMimeType)
            ?: ImageContent(
                mimeType = signature.mimeType,
                extension = signature.extension
            )
    }

    private fun File.readHeader(): ByteArray {
        val header = ByteArray(HEADER_SIZE)

        inputStream().use { input ->
            val read = input.read(header)

            return when {
                read <= 0 -> ByteArray(0)
                read == HEADER_SIZE -> header
                else -> header.copyOf(read)
            }
        }
    }

    private fun contentForMimeType(mimeType: String?): ImageContent? {
        val normalized = mimeType?.lowercase(Locale.ROOT) ?: return null

        return when (normalized) {
            "image/jpeg" -> ImageContent("image/jpeg", "jpg")
            "image/png" -> ImageContent("image/png", "png")
            "image/webp" -> ImageContent("image/webp", "webp")
            "image/gif" -> ImageContent("image/gif", "gif")
            "image/bmp", "image/x-ms-bmp" -> ImageContent("image/bmp", "bmp")
            "image/heif", "image/heic" -> ImageContent(normalized, "heic")
            else -> null
        }
    }
}
