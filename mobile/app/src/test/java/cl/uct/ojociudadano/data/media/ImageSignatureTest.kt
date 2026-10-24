package cl.uct.ojociudadano.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageSignatureTest {

    @Test
    fun `detects a jpeg image`() {
        val signature = detectImageSignature(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        )

        assertEquals("image/jpeg", signature?.mimeType)
        assertEquals("jpg", signature?.extension)
    }

    @Test
    fun `detects a png image`() {
        val signature = detectImageSignature(pngHeader())

        assertEquals("image/png", signature?.mimeType)
        assertEquals("png", signature?.extension)
    }

    @Test
    fun `detects a gif image`() {
        val signature = detectImageSignature("GIF89a".toByteArray())

        assertEquals("image/gif", signature?.mimeType)
        assertEquals("gif", signature?.extension)
    }

    @Test
    fun `detects a webp image`() {
        val signature = detectImageSignature(
            "RIFF".toByteArray() + ByteArray(4) + "WEBP".toByteArray()
        )

        assertEquals("image/webp", signature?.mimeType)
        assertEquals("webp", signature?.extension)
    }

    @Test
    fun `detects a bmp image`() {
        val signature = detectImageSignature("BM".toByteArray())

        assertEquals("image/bmp", signature?.mimeType)
        assertEquals("bmp", signature?.extension)
    }

    @Test
    fun `detects a heif image`() {
        val signature = detectImageSignature(
            byteArrayOf(0, 0, 0, 24) + "ftyp".toByteArray() + "heic".toByteArray()
        )

        assertEquals("image/heif", signature?.mimeType)
        assertEquals("heic", signature?.extension)
    }

    @Test
    fun `rejects content without an image signature`() {
        assertNull(detectImageSignature("no es una imagen".toByteArray()))
    }

    @Test
    fun `rejects a truncated png header`() {
        assertNull(
            detectImageSignature(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
            )
        )
    }

    private fun pngHeader(): ByteArray = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A
    )
}
