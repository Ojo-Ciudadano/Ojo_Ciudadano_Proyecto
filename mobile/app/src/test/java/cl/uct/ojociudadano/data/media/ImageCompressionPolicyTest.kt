package cl.uct.ojociudadano.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCompressionPolicyTest {

    @Test
    fun `keeps a small image as is`() {
        val (width, height) = ImageCompressionPolicy.scaledDimensions(800, 600)

        assertEquals(800, width)
        assertEquals(600, height)
    }

    @Test
    fun `scales the largest side preserving aspect ratio`() {
        val (width, height) = ImageCompressionPolicy.scaledDimensions(4_000, 3_000)

        assertEquals(2_048, width)
        assertEquals(1_536, height)
    }

    @Test
    fun `scales portrait photos by their tallest side`() {
        val (width, height) = ImageCompressionPolicy.scaledDimensions(3_000, 4_000)

        assertEquals(1_536, width)
        assertEquals(2_048, height)
    }

    @Test
    fun `never upscales a photo below the maximum`() {
        val (width, height) = ImageCompressionPolicy.scaledDimensions(320, 200)

        assertEquals(320, width)
        assertEquals(200, height)
    }

    @Test
    fun `computes a power of two sample size for huge photos`() {
        assertEquals(1, ImageCompressionPolicy.computeInSampleSize(2_048, 1_536))
        assertEquals(2, ImageCompressionPolicy.computeInSampleSize(4_000, 3_000))
        assertEquals(4, ImageCompressionPolicy.computeInSampleSize(8_000, 6_000))
        assertEquals(8, ImageCompressionPolicy.computeInSampleSize(16_000, 12_000))
    }

    @Test
    fun `decides to compress only when the photo is heavy or oversized`() {
        assertFalse(
            ImageCompressionPolicy.shouldCompress(
                sizeBytes = 200 * 1024,
                width = 1_200,
                height = 900
            )
        )
        assertTrue(
            ImageCompressionPolicy.shouldCompress(
                sizeBytes = 3 * 1024 * 1024,
                width = 1_200,
                height = 900
            )
        )
        assertTrue(
            ImageCompressionPolicy.shouldCompress(
                sizeBytes = 200 * 1024,
                width = 4_000,
                height = 3_000
            )
        )
    }

    @Test
    fun `decides to compress oversized photos by the configured maximum`() {
        assertFalse(
            ImageCompressionPolicy.shouldCompress(
                sizeBytes = 200 * 1024,
                width = 1_000,
                height = 800,
                maxDimension = 1_024
            )
        )
        assertTrue(
            ImageCompressionPolicy.shouldCompress(
                sizeBytes = 200 * 1024,
                width = 1_200,
                height = 800,
                maxDimension = 1_024
            )
        )
        assertFalse(
            ImageCompressionPolicy.shouldCompress(
                sizeBytes = 200 * 1024,
                width = 1_024,
                height = 800,
                maxDimension = 1_024
            )
        )
    }

    @Test
    fun `downscales geometry without losing the aspect ratio`() {
        val (width, height) = ImageCompressionPolicy.downscale(2_048, 1_536)

        assertEquals(1_741, width)
        assertEquals(1_306, height)
    }
}
