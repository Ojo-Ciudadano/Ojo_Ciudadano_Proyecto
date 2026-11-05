package cl.uct.ojociudadano.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    // Posición inicial razonable (ej. centro de una ciudad o país por defecto, zoom inicial)
    val defaultZoom = 15.0
    val initialLat = -33.4489 // Ejemplo: Santiago de Chile (ajusta según tu contexto)
    val initialLon = -70.6693

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
            MapView(context).apply {
                setMultiTouchControls(true)
                controller.setZoom(defaultZoom)
                controller.setCenter(GeoPoint(initialLat, initialLon))
            }
        },
        update = { mapView ->
            // Preparado para recibir futuros marcadores o lógica de incidentes en una Issue posterior
        }
    )
}
