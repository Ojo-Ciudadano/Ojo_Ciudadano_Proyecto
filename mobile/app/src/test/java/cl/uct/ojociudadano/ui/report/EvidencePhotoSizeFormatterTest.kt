package cl.uct.ojociudadano.ui.report

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EvidencePhotoSizeFormatterTest {

    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `reports an unknown size when the file has no content`() {
        assertEquals("Tamaño desconocido", formatEvidencePhotoSize(0L))
        assertEquals("Tamaño desconocido", formatEvidencePhotoSize(-1L))
    }

    @Test
    fun `formats sizes below one kilobyte as bytes`() {
        assertEquals("512 B", formatEvidencePhotoSize(512L))
        assertEquals("1023 B", formatEvidencePhotoSize(1023L))
    }

    @Test
    fun `formats kilobytes with one decimal`() {
        assertEquals("1.0 KB", formatEvidencePhotoSize(1024L))
        assertEquals("1.5 KB", formatEvidencePhotoSize(1536L))
    }

    @Test
    fun `formats megabytes with one decimal`() {
        assertEquals("1.2 MB", formatEvidencePhotoSize(1_258_291L))
        assertEquals("5.0 MB", formatEvidencePhotoSize(5L * 1024L * 1024L))
    }
}
