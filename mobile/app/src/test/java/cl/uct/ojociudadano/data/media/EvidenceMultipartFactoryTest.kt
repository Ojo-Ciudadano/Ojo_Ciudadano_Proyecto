package cl.uct.ojociudadano.data.media

import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EvidenceMultipartFactoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `uses the field name and media type expected by the backend`() {
        val file = temporaryFolder.newFile("foto.jpg").apply { writeBytes(ByteArray(32) { 1 }) }

        val part = EvidenceMultipartFactory.create(file, "image/jpeg")

        assertEquals("archivo", EvidenceMultipartFactory.FIELD_NAME)
        val disposition = part.headers?.get("Content-Disposition").orEmpty()
        assertTrue(disposition, disposition.contains("name=\"archivo\""))
        assertTrue(disposition, disposition.contains("filename=\"foto.jpg\""))
        assertEquals("image/jpeg", part.body.contentType().toString())
        assertEquals(32L, part.body.contentLength())
    }

    @Test
    fun `normalizes mime type parameters and letter case`() {
        assertEquals("image/jpeg", EvidenceMultipartFactory.normalizedMimeType("IMAGE/JPEG; charset=binary"))
        assertEquals("image/png", EvidenceMultipartFactory.normalizedMimeType(" image/png "))
    }

    @Test
    fun `renames the part with the extension of the resulting format`() {
        assertEquals("foto.jpg", EvidenceMultipartFactory.fileNameFor("foto.png", "image/jpeg"))
        assertEquals("evidencia.png", EvidenceMultipartFactory.fileNameFor("evidencia", "image/png"))
        assertEquals("evidence.jpg", EvidenceMultipartFactory.fileNameFor(null, "image/jpeg"))
    }

    @Test
    fun `sanitizes file names that could break the header`() {
        val name = EvidenceMultipartFactory.fileNameFor("foto con espacios/ñ.jpg", "image/jpeg")

        assertEquals("foto_con_espacios.jpg", name)
    }

    @Test
    fun `rejects empty files`() {
        val file = temporaryFolder.newFile("vacia.jpg")

        assertThrows(IllegalArgumentException::class.java) {
            EvidenceMultipartFactory.create(file, "image/jpeg")
        }
    }

    @Test
    fun `rejects mime types outside the backend contract`() {
        val file = temporaryFolder.newFile("foto.heic").apply { writeBytes(ByteArray(16) { 1 }) }

        assertThrows(IllegalArgumentException::class.java) {
            EvidenceMultipartFactory.create(file, "image/heic")
        }
    }

    @Test
    fun `creates a single part with a readable body`() {
        val file = temporaryFolder.newFile("foto.webp").apply { writeBytes(ByteArray(64) { 7 }) }

        val part: MultipartBody.Part = EvidenceMultipartFactory.create(file, "image/webp")

        assertNotNull(part.headers)
        assertEquals("image/webp", part.body.contentType().toString())
        assertTrue(part.headers?.get("Content-Disposition").orEmpty().endsWith("filename=\"foto.webp\""))
    }

    @Test
    fun `keeps the original when the name already carries an extension`() {
        val folder = temporaryFolder.newFolder("evidencias")
        val file = File(folder, "captura.jpeg").apply { writeBytes(ByteArray(8) { 3 }) }

        val part = EvidenceMultipartFactory.create(file, "image/jpeg")

        assertTrue(part.headers?.get("Content-Disposition").orEmpty().contains("filename=\"captura.jpg\""))
    }
}
