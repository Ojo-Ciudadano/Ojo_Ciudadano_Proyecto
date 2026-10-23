package cl.uct.ojociudadano.data.media

import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Copia el contenido de una fotografía respetando un tamaño máximo.
 *
 * El límite se aplica mientras se leen los bytes, por lo que también protege
 * contra proveedores que no informan el tamaño del archivo o que lo informan
 * de forma incorrecta.
 */
internal object EvidencePhotoContentCopier {

    private const val BUFFER_SIZE = 8 * 1024

    /**
     * Rechaza de inmediato un archivo cuyo tamaño declarado ya supera el
     * límite, evitando reservar espacio en la caché.
     */
    fun requireDeclaredSizeWithinLimit(declaredSize: Long?, maxBytes: Long) {
        if (declaredSize != null && declaredSize > maxBytes) {
            throw fileTooLarge()
        }
    }

    /**
     * Copia [input] en [output] y falla si el total supera [maxBytes].
     *
     * La comprobación de cancelación se hace en cada bloque para que una
     * importación obsoleta no siga consumiendo recursos innecesariamente.
     */
    suspend fun copy(input: InputStream, output: OutputStream, maxBytes: Long) {
        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytes = 0L

        while (true) {
            coroutineContext.ensureActive()

            val read = input.read(buffer)
            if (read == -1) {
                return
            }

            totalBytes += read

            if (totalBytes > maxBytes) {
                throw fileTooLarge()
            }

            output.write(buffer, 0, read)
        }
    }

    private fun fileTooLarge() = PhotoImportException(
        PhotoImportFailure.FILE_TOO_LARGE,
        "La fotografía seleccionada supera el tamaño máximo permitido"
    )
}
