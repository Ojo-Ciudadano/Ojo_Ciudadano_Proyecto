package cl.uct.ojociudadano.ui.reports

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.data.repository.IncidentRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IncidentPhotoState(val loading: Boolean = true, val bitmap: Bitmap? = null, val error: String? = null)

/** Authenticated evidence stays in memory; never put JWTs in URLs or public image caches. */
class IncidentPhotoViewModel(private val repository: IncidentRepository, private val id: String) : ViewModel() {
    private val _state = MutableStateFlow(IncidentPhotoState())
    val state = _state.asStateFlow()
    init { load() }
    fun load() {
        viewModelScope.launch {
            _state.value = IncidentPhotoState()
            try {
                val evidence = repository.getEvidencesByIncident(id).firstOrNull { it.mimeType.startsWith("image/") }
                val bitmap = if (evidence == null) null else withContext(Dispatchers.IO) {
                    repository.downloadEvidenceFile(evidence.id).use { body ->
                        // Bound memory even if the server misreports the content length.
                        val bytes = body.byteStream().use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 10 * 1024 * 1024) { "Evidence too large" }
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        require(bytes.size <= 10 * 1024 * 1024) { "Evidence too large" }
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        val options = BitmapFactory.Options().apply {
                            while (bounds.outWidth / inSampleSize > 1200 || bounds.outHeight / inSampleSize > 1200) inSampleSize *= 2
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            ?: error("Invalid image")
                    }
                }
                _state.value = IncidentPhotoState(loading = false, bitmap = bitmap)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { _state.value = IncidentPhotoState(loading = false, error = "No se pudo cargar la evidencia.") }
        }
    }
}

@Composable
fun IncidentPhoto(id: String) {
    val app = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: IncidentPhotoViewModel = viewModel(key = "evidence:$id", factory = remember(app, id) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = IncidentPhotoViewModel(app.container.incidentRepository, id) as T
        }
    })
    val state by model.state.collectAsState()
    state.bitmap?.let {
        Image(it.asImageBitmap(), "Fotografía del incidente", Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
    }
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (state.error != null) TextButton(onClick = model::load) { Text("Evidencia no disponible · Reintentar") }
}
