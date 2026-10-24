package cl.uct.ojociudadano.data.media

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EvidencePhotoContentCopierTest {

    @Test
    fun `rejects a declared size above the limit`() {
        val exception = runCatching {
            EvidencePhotoContentCopier.requireDeclaredSizeWithinLimit(
                declaredSize = 17L,
                maxBytes = 16L
            )
        }.exceptionOrNull()

        assertEquals(
            PhotoImportFailure.FILE_TOO_LARGE,
            (exception as PhotoImportException).failure
        )
    }

    @Test
    fun `accepts a declared size equal to the limit`() {
        EvidencePhotoContentCopier.requireDeclaredSizeWithinLimit(
            declaredSize = 16L,
            maxBytes = 16L
        )
    }

    @Test
    fun `accepts content when the provider does not declare its size`() {
        EvidencePhotoContentCopier.requireDeclaredSizeWithinLimit(
            declaredSize = null,
            maxBytes = 16L
        )
    }

    @Test
    fun `rejects a stream without declared size when it exceeds the limit`() = runTest {
        val output = ByteArrayOutputStream()

        try {
            EvidencePhotoContentCopier.copy(
                input = ByteArrayInputStream(ByteArray(17)),
                output = output,
                maxBytes = 16L
            )
            fail("Se esperaba PhotoImportException")
        } catch (exception: PhotoImportException) {
            assertEquals(PhotoImportFailure.FILE_TOO_LARGE, exception.failure)
        }
    }

    @Test
    fun `copies a stream whose size is exactly the limit`() = runTest {
        val output = ByteArrayOutputStream()

        EvidencePhotoContentCopier.copy(
            input = ByteArrayInputStream(ByteArray(16) { it.toByte() }),
            output = output,
            maxBytes = 16L
        )

        assertEquals(16, output.size())
    }
}
