package cl.uct.ojociudadano.ui.reports.evidence

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cl.uct.ojociudadano.data.model.Category
import cl.uct.ojociudadano.data.model.Incident
import cl.uct.ojociudadano.data.model.IncidentLocation
import cl.uct.ojociudadano.data.model.IncidentStatus
import cl.uct.ojociudadano.domain.model.Evidence
import cl.uct.ojociudadano.ui.reports.IncidentCard
import cl.uct.ojociudadano.ui.theme.OjoCiudadanoTheme
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import okio.Buffer
import okio.FileSystem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(DelicateCoilApi::class)
class IncidentEvidenceGalleryTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var imageLoader: ImageLoader? = null

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
        imageLoader?.shutdown()
        imageLoader = null
    }

    @Test
    fun emptyEvidenceListShowsOptionalMessage() {
        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(evidences = emptyList(), showEmptyMessage = true)
            }
        }

        compose.onNodeWithTag(IncidentEvidenceTestTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(IncidentEvidenceTestTags.GALLERY).assertDoesNotExist()
    }

    @Test
    fun emptyEvidenceListWithoutMessageRendersNothing() {
        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(evidences = emptyList())
            }
        }

        compose.onNodeWithTag(IncidentEvidenceTestTags.GALLERY).assertDoesNotExist()
        compose.onNodeWithTag(IncidentEvidenceTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun nonImageEvidenceIsFilteredOut() {
        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(
                    evidences = listOf(evidence("pdf", mimeType = "application/pdf")),
                    showEmptyMessage = true
                )
            }
        }

        compose.onNodeWithTag(IncidentEvidenceTestTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(IncidentEvidenceTestTags.photo("pdf")).assertDoesNotExist()
    }

    @Test
    fun multipleImagesRenderAllPhotosAndDescriptions() {
        installImageLoader { _, _ -> FakeOutcome.Success }
        val first = evidence("first")
        val second = evidence("second")

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(
                    evidences = listOf(first, second),
                    compact = true
                )
            }
        }

        waitForLoaded(first.id)
        waitForLoaded(second.id)
        compose.onNodeWithTag(IncidentEvidenceTestTags.photo(first.id), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(IncidentEvidenceTestTags.photo(second.id), useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription(
            "Evidencia fotográfica 1 de 2",
            useUnmergedTree = true
        ).assertExists()
        compose.onNodeWithContentDescription(
            "Evidencia fotográfica 2 de 2",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun nonImageEvidenceDoesNotChangeTheDisplayedCount() {
        installImageLoader { _, _ -> FakeOutcome.Success }
        val image = evidence("image")
        val pdf = evidence("pdf", mimeType = "application/pdf")

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(evidences = listOf(image, pdf), compact = true)
            }
        }

        waitForLoaded(image.id)
        compose.onNodeWithTag(IncidentEvidenceTestTags.photo(pdf.id), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription(
            "Evidencia fotográfica 1 de 1",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun loadingStateIsShownWhileEvidenceIsPending() {
        val gate = CompletableDeferred<Unit>()
        installImageLoader { _, _ -> FakeOutcome.WaitFor(gate) }

        try {
            compose.setContent {
                OjoCiudadanoTheme {
                    IncidentEvidenceGallery(evidences = listOf(evidence("loading")))
                }
            }

            compose.onNodeWithTag(
                IncidentEvidenceTestTags.loading("loading"),
                useUnmergedTree = true
            ).assertExists()

            gate.complete(Unit)
            waitForLoaded("loading")
        } finally {
            gate.complete(Unit)
        }
    }

    @Test
    fun failedPhotoShowsErrorAndRetryRecovers() {
        val server = installImageLoader { _, attempt ->
            if (attempt == 1) {
                FakeOutcome.Failure(IOException("fallo simulado"))
            } else {
                FakeOutcome.Success
            }
        }

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(evidences = listOf(evidence("retry")))
            }
        }

        waitForTag(IncidentEvidenceTestTags.error("retry"))
        compose.onNodeWithText("Reintentar", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(
            IncidentEvidenceTestTags.retry("retry"),
            useUnmergedTree = true
        ).performClick()

        waitForLoaded("retry")
        assertEquals(2, server.attemptsFor("retry"))
    }

    @Test
    fun oneFailedPhotoDoesNotHideAnotherPhoto() {
        installImageLoader { evidence, _ ->
            if (evidence.id == "broken") {
                FakeOutcome.Failure(IOException("fallo simulado"))
            } else {
                FakeOutcome.Success
            }
        }

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentEvidenceGallery(
                    evidences = listOf(evidence("ok"), evidence("broken")),
                    compact = true
                )
            }
        }

        waitForLoaded("ok")
        waitForTag(IncidentEvidenceTestTags.error("broken"))
        compose.onNodeWithTag(IncidentEvidenceTestTags.photo("ok"), useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(IncidentEvidenceTestTags.photo("broken"), useUnmergedTree = true).assertExists()
        compose.onNodeWithText("No disponible", useUnmergedTree = true).assertExists()
    }

    @Test
    fun incidentCardWithoutEvidenceStillRenders() {
        compose.setContent {
            OjoCiudadanoTheme {
                IncidentCard(incident = incident(evidences = emptyList()), onClick = {})
            }
        }

        compose.onNodeWithText("Incidente con evidencias").assertExists()
        compose.onNodeWithTag(IncidentEvidenceTestTags.GALLERY).assertDoesNotExist()
        compose.onNodeWithTag(IncidentEvidenceTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun incidentCardReusesCompactGallery() {
        installImageLoader { _, _ -> FakeOutcome.Success }
        val incident = incident(
            evidences = listOf(evidence("card-1"), evidence("card-2"))
        )

        compose.setContent {
            OjoCiudadanoTheme {
                IncidentCard(incident = incident, onClick = {})
            }
        }

        waitForLoaded("card-1")
        waitForLoaded("card-2")
        compose.onNodeWithTag(IncidentEvidenceTestTags.GALLERY, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Incidente con evidencias").assertExists()
    }

    @Test
    fun detailPresentationUsesTheSameGallery() {
        installImageLoader { _, _ -> FakeOutcome.Success }

        compose.setContent {
            OjoCiudadanoTheme {
                Column {
                    IncidentEvidenceGallery(
                        evidences = listOf(evidence("detail")),
                        compact = false
                    )
                }
            }
        }

        waitForLoaded("detail")
        compose.onNodeWithTag(IncidentEvidenceTestTags.GALLERY, useUnmergedTree = true).assertExists()
    }

    private fun installImageLoader(
        outcome: (Evidence, Int) -> FakeOutcome
    ): FakeEvidenceFetcherFactory {
        val factory = FakeEvidenceFetcherFactory(imageBytes, outcome)
        val loader = ImageLoader.Builder(compose.activity)
            .components { add(factory) }
            .diskCache(null)
            .build()
        imageLoader = loader
        SingletonImageLoader.setUnsafe(loader)
        return factory
    }

    private fun waitForLoaded(id: String) {
        waitForTag(IncidentEvidenceTestTags.loaded(id))
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private val imageBytes: ByteArray by lazy {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun evidence(
        id: String,
        mimeType: String = "image/png"
    ) = Evidence(
        id = id,
        incidentId = "incident-1",
        mimeType = mimeType,
        size = 1024L,
        createdAt = "2026-11-02T12:00:00.000Z",
        fileUrl = "/api/v1/evidencias/$id/archivo"
    )

    private fun incident(evidences: List<Evidence>) = Incident(
        id = "incident-1",
        title = "Incidente con evidencias",
        description = "Descripción utilizada exclusivamente en pruebas",
        category = Category("category", "Alumbrado", "", 1, false, null),
        status = IncidentStatus("status", "Nuevo", 1),
        location = IncidentLocation(
            id = "location",
            latitude = -38.7,
            longitude = -72.5,
            referenceAddress = "Plaza",
            commune = "Temuco",
            region = "Araucanía",
            sector = "Centro"
        ),
        reporterUserId = "citizen",
        priorityLevel = null,
        createdAt = "2026-11-02T12:00:00Z",
        updatedAt = "2026-11-02T12:00:00Z",
        evidences = evidences
    )
}

private sealed interface FakeOutcome {
    data object Success : FakeOutcome
    data class Failure(val error: Throwable) : FakeOutcome
    data class WaitFor(val gate: CompletableDeferred<Unit>) : FakeOutcome
}

private class FakeEvidenceFetcherFactory(
    private val bytes: ByteArray,
    private val outcome: (Evidence, Int) -> FakeOutcome
) : Fetcher.Factory<Evidence> {

    private val attempts = ConcurrentHashMap<String, AtomicInteger>()

    override fun create(
        data: Evidence,
        options: Options,
        imageLoader: ImageLoader
    ): Fetcher = object : Fetcher {
        override suspend fun fetch(): FetchResult? {
            val attempt = attempts.getOrPut(data.id) { AtomicInteger(0) }.incrementAndGet()
            when (val result = outcome(data, attempt)) {
                FakeOutcome.Success -> Unit
                is FakeOutcome.Failure -> throw result.error
                is FakeOutcome.WaitFor -> result.gate.await()
            }

            return SourceFetchResult(
                source = ImageSource(Buffer().write(bytes), FileSystem.SYSTEM),
                mimeType = "image/png",
                dataSource = DataSource.MEMORY
            )
        }
    }

    fun attemptsFor(id: String): Int = attempts[id]?.get() ?: 0
}
