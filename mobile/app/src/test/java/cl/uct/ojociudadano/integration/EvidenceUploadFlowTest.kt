package cl.uct.ojociudadano.integration

import cl.uct.ojociudadano.data.media.EvidencePhotoFileStore
import cl.uct.ojociudadano.data.media.PendingPhotoCapture
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.IncidentPhotoEvidence
import cl.uct.ojociudadano.data.remote.RetrofitClient
import cl.uct.ojociudadano.data.repository.CategoryRepository
import cl.uct.ojociudadano.data.repository.RemoteIncidentRepository
import cl.uct.ojociudadano.data.session.DefaultSessionManager
import cl.uct.ojociudadano.data.session.TestSessionStorage
import cl.uct.ojociudadano.data.session.testSession
import cl.uct.ojociudadano.ui.report.CreateReportSubmissionState
import cl.uct.ojociudadano.ui.report.CreateReportViewModel
import cl.uct.ojociudadano.ui.report.EvidenceUploadFailureKind
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifica que el formulario use la capa de datos real de evidencias después de
 * crear el incidente, incluyendo el identificador devuelto por el backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EvidenceUploadFlowTest {

    private val server = MockWebServer()
    private val session = testSession()
    private val category = Category(
        id = "c0000000-0000-4000-8000-000000000003",
        name = "Alumbrado público",
        description = "",
        basePriority = 1,
        isCriticalEmergency = false,
        officialContactChannel = null
    )

    private lateinit var temporaryDirectory: File
    private lateinit var firstFile: File
    private lateinit var secondFile: File
    private lateinit var photoStore: InMemoryEvidencePhotoFileStore

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server.start()

        temporaryDirectory = Files.createTempDirectory("evidence-upload-flow").toFile()
        firstFile = File(temporaryDirectory, "evidence_first.jpg").apply {
            writeBytes(ByteArray(16) { 1 })
        }
        secondFile = File(temporaryDirectory, "evidence_second.jpg").apply {
            writeBytes(ByteArray(16) { 2 })
        }
        photoStore = InMemoryEvidencePhotoFileStore(
            mutableMapOf(firstFile.name to firstFile, secondFile.name to secondFile)
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
        temporaryDirectory.deleteRecursively()
    }

    @Test
    fun `creates the incident and uploads all evidences with the created id`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(incidentJson())
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(evidenceJson("evidence-1"))
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(evidenceJson("evidence-2"))
        )

        val viewModel = createViewModel()

        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(category)
        seedPhoto(viewModel, firstFile)
        seedPhoto(viewModel, secondFile)

        viewModel.createReport()
        awaitTerminalState(viewModel)

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = INCIDENT_ID,
                uploadedEvidenceCount = 2
            ),
            viewModel.submissionState
        )
        assertTrue(viewModel.formState.evidencePhotos.isEmpty())
        assertFalse(firstFile.exists())
        assertFalse(secondFile.exists())

        val createRequest = takeRequest()
        assertEquals("POST", createRequest.method)
        assertEquals("/api/v1/incidentes", createRequest.path)
        assertEquals(
            "Bearer ${session.accessToken}",
            createRequest.getHeader("Authorization")
        )

        for (fileName in listOf(firstFile.name, secondFile.name)) {
            val uploadRequest = takeRequest()
            assertEquals("POST", uploadRequest.method)
            assertEquals(
                "/api/v1/incidentes/$INCIDENT_ID/evidencias",
                uploadRequest.path
            )
            assertEquals(
                "Bearer ${session.accessToken}",
                uploadRequest.getHeader("Authorization")
            )
            assertTrue(
                uploadRequest.getHeader("Content-Type")
                    .orEmpty()
                    .startsWith("multipart/form-data")
            )

            val body = uploadRequest.body.readUtf8()
            assertTrue(body.contains("name=\"file\""))
            assertTrue(body.contains(fileName))
        }

        assertEquals(3, server.requestCount)
    }

    @Test
    fun `uncertain upload requires confirmation and reuses the created incident`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(incidentJson())
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":"storage"}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(evidenceJson("evidence-2"))
        )

        val viewModel = createViewModel()
        viewModel.onTitleChange("Poste dañado")
        viewModel.onDescriptionChange("El poste bloquea la vereda")
        viewModel.onCategorySelected(category)
        seedPhoto(viewModel, firstFile)
        seedPhoto(viewModel, secondFile)

        viewModel.createReport()
        awaitTerminalState(viewModel)

        val failed =
            viewModel.submissionState as CreateReportSubmissionState.EvidenceUploadFailed
        assertEquals(1, failed.uploadedEvidenceCount)
        assertEquals(1, failed.pendingEvidence.size)
        assertEquals(
            EvidenceUploadFailureKind.POSSIBLY_UPLOADED,
            failed.pendingEvidence.single().failureKind
        )
        assertTrue(failed.hasUncertainEvidence)
        assertEquals(3, server.requestCount)
        assertEquals(listOf(firstFile.name), viewModel.formState.evidencePhotos.map { it.fileName })

        viewModel.createReport()

        assertEquals(3, server.requestCount)

        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(evidenceJson("evidence-3"))
        )
        viewModel.confirmRetryAfterPossibleDuplicate()
        awaitTerminalState(viewModel)

        assertEquals(
            CreateReportSubmissionState.Success(
                incidentId = INCIDENT_ID,
                uploadedEvidenceCount = 2
            ),
            viewModel.submissionState
        )
        assertEquals(4, server.requestCount)
        assertTrue(viewModel.formState.evidencePhotos.isEmpty())

        val requests = List(4) { takeRequest() }
        assertEquals("POST", requests.first().method)
        assertEquals("/api/v1/incidentes", requests.first().path)
        assertEquals(
            3,
            requests.drop(1).count {
                it.path == "/api/v1/incidentes/$INCIDENT_ID/evidencias"
            }
        )
        assertEquals(1, requests.count { it.path == "/api/v1/incidentes" })
    }

    private suspend fun createViewModel(): CreateReportViewModel {
        val manager = DefaultSessionManager(
            sessionStorage = TestSessionStorage(session),
            ioDispatcher = Dispatchers.Unconfined
        )
        manager.restore()
        val api = RetrofitClient.createApiService(
            baseUrl = server.url("/").toString(),
            tokenProvider = manager,
            onUnauthorized = manager::invalidateIfCurrent
        )
        return CreateReportViewModel(
            categoryRepository = FakeCategoryRepository(listOf(category)),
            incidentRepository = RemoteIncidentRepository(api, manager),
            photoFileStore = photoStore,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    private fun seedPhoto(viewModel: CreateReportViewModel, file: File) {
        val token = viewModel.onPhotoCaptureStarted()
        viewModel.onPhotoCaptured(
            token,
            IncidentPhotoEvidence(
                localUri = "content://evidences/${file.name}",
                fileName = file.name,
                mimeType = "image/jpeg",
                sizeBytes = file.length(),
                capturedAt = 1_700_000_000_000L
            )
        )
    }

    private suspend fun awaitTerminalState(viewModel: CreateReportViewModel) {
        withTimeout(10_000) {
            while (
                viewModel.submissionState !is CreateReportSubmissionState.Success &&
                viewModel.submissionState !is CreateReportSubmissionState.EvidenceUploadFailed &&
                viewModel.submissionState !is CreateReportSubmissionState.Error
            ) {
                delay(10)
            }
        }
    }

    private fun takeRequest(): RecordedRequest =
        checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))

    private fun incidentJson(): String = """{
        "id":"$INCIDENT_ID",
        "titulo":"Poste dañado",
        "descripcion":"El poste bloquea la vereda",
        "categoriaId":"${category.id}",
        "estadoId":"estado",
        "ubicacionId":"ubicacion",
        "usuarioReportaId":"${session.userId}",
        "nivelPrioridad":null,
        "fechaCreacion":"2026-10-30T00:00:00.000Z",
        "fechaActualizacion":"2026-10-30T00:00:00.000Z",
        "categoria":{
            "id":"${category.id}",
            "nombre":"Alumbrado público",
            "descripcion":"",
            "prioridadBase":1,
            "esEmergenciaCritica":false
        },
        "estado":{"id":"estado","nombre":"Nuevo","orden":1},
        "ubicacion":{
            "id":"ubicacion",
            "latitud":-38.7359,
            "longitud":-72.5904,
            "direccionReferencia":"Plaza",
            "comuna":"Temuco",
            "region":"Araucanía",
            "sector":"Centro"
        }
    }"""

    private fun evidenceJson(id: String): String = """{
        "id":"$id",
        "incidenteId":"$INCIDENT_ID",
        "tipoMime":"image/jpeg",
        "tamanoArchivo":16,
        "fechaCreacion":"2026-10-30T00:00:01.000Z",
        "url":"/evidencias/$id/archivo"
    }"""

    private class FakeCategoryRepository(
        private val categories: List<Category>
    ) : CategoryRepository {
        override suspend fun getCategories(): List<Category> = categories
    }

    private class InMemoryEvidencePhotoFileStore(
        private val files: MutableMap<String, File>
    ) : EvidencePhotoFileStore {

        override fun create(): PendingPhotoCapture = error("No usado en esta prueba")

        override fun complete(pending: PendingPhotoCapture): IncidentPhotoEvidence? = null

        override suspend fun import(contentUri: String): IncidentPhotoEvidence =
            error("No usado en esta prueba")

        override fun exists(fileName: String): Boolean =
            files[fileName]?.let { it.isFile && it.length() > 0L } == true

        override fun file(fileName: String): File? =
            files[fileName]?.takeIf { it.isFile && it.length() > 0L }

        override fun delete(fileName: String) {
            files.remove(fileName)?.delete()
        }

        override fun cleanupExpired(maxAgeMillis: Long): Int = 0
    }

    private companion object {
        const val INCIDENT_ID = "d0000000-0000-4000-8000-000000000091"
    }
}
