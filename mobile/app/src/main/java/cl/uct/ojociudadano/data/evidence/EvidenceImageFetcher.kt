package cl.uct.ojociudadano.data.evidence

import cl.uct.ojociudadano.data.repository.IncidentRepository
import cl.uct.ojociudadano.domain.model.Evidence
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import java.io.IOException
import okio.Buffer
import okio.FileSystem
import okio.ForwardingSource
import okio.Source
import okio.buffer

/** Límite defensivo alineado con el máximo aceptado por el backend. */
const val MAX_EVIDENCE_IMAGE_BYTES = 10L * 1024L * 1024L

/**
 * Descarga una evidencia fotográfica a través de la capa de datos del incidente.
 *
 * La UI nunca conoce el repositorio ni el endpoint: Coil entrega la evidencia y
 * este fetcher obtiene los bytes autenticados desde [IncidentRepository].
 */
class EvidenceImageFetcher(
    private val evidence: Evidence,
    private val incidentRepository: IncidentRepository,
    private val fileSystem: FileSystem,
    private val maxBytes: Long = MAX_EVIDENCE_IMAGE_BYTES
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val body = incidentRepository.downloadEvidenceFile(evidence.id)

        return try {
            val source = LimitedSource(body.source(), maxBytes)
            SourceFetchResult(
                source = ImageSource(source.buffer(), fileSystem),
                mimeType = evidence.mimeType.takeIf { it.isNotBlank() },
                dataSource = DataSource.NETWORK
            )
        } catch (error: Throwable) {
            body.close()
            throw error
        }
    }

    class Factory(
        private val incidentRepository: IncidentRepository
    ) : Fetcher.Factory<Evidence> {

        override fun create(
            data: Evidence,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            if (!supports(data)) {
                return null
            }

            return EvidenceImageFetcher(
                evidence = data,
                incidentRepository = incidentRepository,
                fileSystem = options.fileSystem
            )
        }

        companion object {
            fun supports(data: Evidence): Boolean =
                data.mimeType.startsWith("image/", ignoreCase = true)
        }
    }
}

/**
 * Evita descargar indefinidamente si el servidor no respeta el tamaño declarado.
 * El [ImageSource] cierra la cadena y, con ella, el [okhttp3.ResponseBody] original.
 */
private class LimitedSource(
    delegate: Source,
    private val maxBytes: Long
) : ForwardingSource(delegate) {

    private var bytesRead = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        val read = super.read(sink, byteCount)
        if (read == -1L) {
            return read
        }

        bytesRead += read
        if (bytesRead > maxBytes) {
            throw IOException("La evidencia excede el límite de $maxBytes bytes")
        }

        return read
    }
}
