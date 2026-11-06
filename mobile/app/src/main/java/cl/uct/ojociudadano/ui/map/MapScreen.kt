package cl.uct.ojociudadano.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Centro inicial: plaza de Temuco, Región de La Araucanía. */
private const val DEFAULT_LATITUDE = -38.7359
private const val DEFAULT_LONGITUDE = -72.5904
private const val DEFAULT_ZOOM = 10.0

internal object MapTestTags {
    const val LOADING = "map_state_loading"
    const val EMPTY = "map_state_empty"
    const val ERROR = "map_state_error"
    const val RETRY = "map_state_retry"
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel? = null
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: MapViewModel = mapViewModel ?: viewModel(factory = remember(application) {
        MapViewModelFactory(application.container.incidentRepository)
    })
    val state by model.uiState.collectAsState()
    val markers = (state as? MapUiState.Success)?.markers.orEmpty()

    Box(modifier.fillMaxSize()) {
        IncidentMap(
            markers = markers,
            modifier = Modifier.fillMaxSize()
        )
        MapStatusOverlay(
            state = state,
            onRetry = model::loadIncidents,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Composable
private fun IncidentMap(
    markers: List<IncidentMapMarker>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
            MapView(context).apply {
                setMultiTouchControls(true)
                controller.setZoom(DEFAULT_ZOOM)
                controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            }
        },
        update = { mapView ->
            syncIncidentMarkers(mapView, markers)
        }
    )
}

/**
 * Sincroniza los marcadores de incidentes sobre el [MapView].
 *
 * El identificador del incidente se guarda en `relatedObject` y se usa como
 * clave: los marcadores existentes se reutilizan, los que ya no corresponden se
 * eliminan y cualquier duplicado previo se descarta. Así las actualizaciones
 * normales del estado no acumulan marcadores.
 */
internal fun syncIncidentMarkers(
    mapView: MapView,
    desiredMarkers: List<IncidentMapMarker>
) {
    val desiredById = desiredMarkers.associateBy { it.incidentId }

    val currentById = HashMap<String, Marker>()
    val duplicateMarkers = ArrayList<Marker>()

    mapView.overlays.forEach { overlay ->
        if (overlay is Marker) {
            val incidentId = overlay.relatedObject as? String
            if (incidentId != null) {
                if (currentById.putIfAbsent(incidentId, overlay) != null) {
                    duplicateMarkers.add(overlay)
                }
            }
        }
    }

    var changed = duplicateMarkers.isNotEmpty()
    duplicateMarkers.forEach { mapView.overlays.remove(it) }

    currentById.forEach { (incidentId, marker) ->
        if (incidentId !in desiredById) {
            mapView.overlays.remove(marker)
            changed = true
        }
    }

    desiredById.forEach { (incidentId, data) ->
        val previous = currentById[incidentId]
        if (previous == null) {
            mapView.overlays.add(createIncidentMarker(mapView, data))
            changed = true
        } else {
            val position = previous.position
            if (position.latitude != data.latitude || position.longitude != data.longitude) {
                previous.position = GeoPoint(data.latitude, data.longitude)
                changed = true
            }
        }
    }

    if (changed) {
        mapView.invalidate()
    }
}

private fun createIncidentMarker(
    mapView: MapView,
    data: IncidentMapMarker
): Marker = Marker(mapView).apply {
    position = GeoPoint(data.latitude, data.longitude)
    relatedObject = data.incidentId
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
}

@Composable
private fun MapStatusOverlay(
    state: MapUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        when (state) {
            MapUiState.Loading -> MapStatusCard(Modifier.testTag(MapTestTags.LOADING)) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text(
                    "Cargando incidentes del mapa…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MapUiState.Empty -> MapStatusCard(Modifier.testTag(MapTestTags.EMPTY)) {
                Text(
                    "No hay incidentes con ubicación disponible.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is MapUiState.Error -> MapStatusCard(
                modifier = Modifier.testTag(MapTestTags.ERROR),
                isError = true
            ) {
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag(MapTestTags.RETRY)
                ) {
                    Text("Reintentar")
                }
            }

            is MapUiState.Success -> Unit
        }
    }
}

@Composable
private fun MapStatusCard(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}
