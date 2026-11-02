package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.data.model.CreateIncidentInput
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.remote.error.ApiException
import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.domain.model.Evidence
import coil3.decode.DataSource
import coil3.fetch.SourceFetchResult
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceImageFetcherTest {

    @Test
    fun `downloads the evidence through the repository and exposes its mime type`() = runTest {
        val repository = FakeIncidentRepository {
            "imagen".toByteArray().toResponseBody("image/jpeg".toMediaType())
        }
        val fetcher = EvidenceImageFetcher(
            evidence = sampleEvidence(),
            incidentRepository = repository,
            fileSystem = FileSystem.SYSTEM
        )

        val result = fetcher.fetch() as SourceFetchResult

        assertEquals("evidencia-1", repository.requestedId)
        assertEquals("image/jpeg", result.mimeType)
        assertEquals(DataSource.NETWORK, result.dataSource)
        try {
            assertEquals("imagen", result.source.source().readUtf8())
        } finally {
            result.source.close()
        }
    }

    @Test
    fun `rejects an evidence body that exceeds the defensive limit`() = runTest {
        val repository = FakeIncidentRepository {
            "demasiado-grande".toByteArray().toResponseBody("image/png".toMediaType())
        }
        val fetcher = EvidenceImageFetcher(
            evidence = sampleEvidence().copy(mimeType = "image/png"),
            incidentRepository = repository,
            fileSystem = FileSystem.SYSTEM,
            maxBytes = 4
        )

        val result = fetcher.fetch() as SourceFetchResult

        try {
            assertThrows(IOException::class.java) {
                result.source.source().readUtf8()
            }
        } finally {
            result.source.close()
        }
    }

    @Test
    fun `closes the response body when the image source is closed`() = runTest {
        val closed = AtomicBoolean(false)
        val body = TrackingResponseBody(
            bytes = "imagen".toByteArray(),
            mediaType = "image/jpeg".toMediaType(),
            closed = closed
        )
        val repository = FakeIncidentRepository { body }
        val fetcher = EvidenceImageFetcher(
            evidence = sampleEvidence(),
            incidentRepository = repository,
            fileSystem = FileSystem.SYSTEM
        )

        val result = fetcher.fetch() as SourceFetchResult
        result.source.close()

        assertTrue(closed.get())
    }

    @Test
    fun `propagates repository errors without producing an image source`() {
        val repository = FakeIncidentRepository {
            throw ApiException.Network(IOException("sin conexión"))
        }
        val fetcher = EvidenceImageFetcher(
            evidence = sampleEvidence(),
            incidentRepository = repository,
            fileSystem = FileSystem.SYSTEM
        )

        assertThrows(ApiException.Network::class.java) {
            runBlocking { fetcher.fetch() }
        }
    }

    @Test
    fun `only supports evidence whose mime type is an image`() {
        assertTrue(EvidenceImageFetcher.Factory.supports(sampleEvidence()))
        assertTrue(
            EvidenceImageFetcher.Factory.supports(
                sampleEvidence().copy(mimeType = "IMAGE/WEBP")
            )
        )
        assertFalse(
            EvidenceImageFetcher.Factory.supports(
                sampleEvidence().copy(mimeType = "application/pdf")
            )
        )
    }

    private class TrackingResponseBody(
        private val bytes: ByteArray,
        private val mediaType: MediaType,
        private val closed: AtomicBoolean
    ) : ResponseBody() {

        override fun contentType(): MediaType = mediaType

        override fun contentLength(): Long = bytes.size.toLong()

        override fun source(): BufferedSource {
            val delegate = Buffer().write(bytes)
            return object : ForwardingSource(delegate) {
                override fun close() {
                    closed.set(true)
                    super.close()
                }
            }.buffer()
        }
    }

    private class FakeIncidentRepository(
        private val onDownload: suspend (String) -> ResponseBody
    ) : IncidentRepository {

        var requestedId: String? = null
            private set

        override suspend fun getIncidents(): List<Incident> = error("No usado")

        override suspend fun getCurrentUserIncidents(): List<Incident> = error("No usado")

        override suspend fun getIncidentById(id: String): Incident = error("No usado")

        override suspend fun createIncident(input: CreateIncidentInput): Incident =
            error("No usado")

        override suspend fun uploadEvidence(
            incidenteId: String,
            file: java.io.File,
            mimeType: String
        ): Evidence = error("No usado")

        override suspend fun getEvidencesByIncident(incidenteId: String): List<Evidence> =
            error("No usado")

        override suspend fun getEvidenceById(id: String): Evidence = error("No usado")

        override suspend fun downloadEvidenceFile(id: String): ResponseBody {
            requestedId = id
            return onDownload(id)
        }
    }

    private fun sampleEvidence() = Evidence(
        id = "evidencia-1",
        incidentId = "incidente-1",
        mimeType = "image/jpeg",
        size = 6L,
        createdAt = "2026-11-02T12:00:00.000Z",
        fileUrl = "/api/v1/evidencias/evidencia-1/archivo"
    )
}
