package cl.uct.ojociudadano.data.media

/**
 * Formato de imagen reconocido a partir de los bytes iniciales del archivo.
 *
 * [canFallbackWhenPlatformCannotDecode] permite aceptar contenedores que la
 * versión de Android no puede decodificar, como HEIF en API 26–27.
 */
internal data class ImageSignature(
    val mimeType: String,
    val extension: String,
    val canFallbackWhenPlatformCannotDecode: Boolean = false
)

/**
 * Detecta el formato real de una imagen sin confiar en el MIME declarado por el
 * proveedor ni en la extensión del archivo.
 */
internal fun detectImageSignature(header: ByteArray): ImageSignature? {
    if (header.matchesBytes(0xFF, 0xD8, 0xFF)) {
        return JPEG_SIGNATURE
    }

    if (
        header.matchesBytes(
            0x89,
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A
        )
    ) {
        return PNG_SIGNATURE
    }

    if (header.matchesAscii("GIF87a") || header.matchesAscii("GIF89a")) {
        return GIF_SIGNATURE
    }

    if (header.matchesAscii("RIFF") && header.matchesAscii("WEBP", offset = 8)) {
        return WEBP_SIGNATURE
    }

    if (header.matchesAscii("BM")) {
        return BMP_SIGNATURE
    }

    if (header.matchesAscii("ftyp", offset = 4) && header.hasHeifBrand()) {
        return HEIF_SIGNATURE
    }

    return null
}

private fun ByteArray.matchesBytes(vararg expected: Int): Boolean =
    matchesBytesAt(offset = 0, *expected)

private fun ByteArray.matchesBytesAt(offset: Int, vararg expected: Int): Boolean {
    if (size < offset + expected.size) {
        return false
    }

    return expected.indices.all { index ->
        this[offset + index].toInt() and 0xFF == expected[index]
    }
}

private fun ByteArray.matchesAscii(value: String, offset: Int = 0): Boolean {
    if (size < offset + value.length) {
        return false
    }

    return value.indices.all { index ->
        this[offset + index].toInt() and 0xFF == value[index].code
    }
}

private fun ByteArray.hasHeifBrand(): Boolean =
    HEIF_BRANDS.any { brand -> matchesAscii(brand, offset = 8) }

private val JPEG_SIGNATURE = ImageSignature(
    mimeType = "image/jpeg",
    extension = "jpg"
)

private val PNG_SIGNATURE = ImageSignature(
    mimeType = "image/png",
    extension = "png"
)

private val GIF_SIGNATURE = ImageSignature(
    mimeType = "image/gif",
    extension = "gif"
)

private val WEBP_SIGNATURE = ImageSignature(
    mimeType = "image/webp",
    extension = "webp"
)

private val BMP_SIGNATURE = ImageSignature(
    mimeType = "image/bmp",
    extension = "bmp"
)

private val HEIF_SIGNATURE = ImageSignature(
    mimeType = "image/heif",
    extension = "heic",
    canFallbackWhenPlatformCannotDecode = true
)

private val HEIF_BRANDS = setOf(
    "heic",
    "heix",
    "hevc",
    "hevx",
    "heis",
    "hevm",
    "hevs",
    "mif1",
    "msf1"
)
