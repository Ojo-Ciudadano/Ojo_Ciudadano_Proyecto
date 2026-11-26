package cl.uct.ojociudadano.ui.map.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.ui.map.applyMapAppearance
import cl.uct.ojociudadano.ui.theme.LocalDarkTheme
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.Locale
import cl.uct.ojociudadano.ui.theme.Spacing

internal object ManualLocationPickerTestTags {
    const val DIALOG = "manual_location_picker"
    const val MAP = "manual_location_picker_map"
    const val COORDINATES = "manual_location_picker_coordinates"
    const val CONFIRM = "manual_location_picker_confirm"
    const val CANCEL = "manual_location_picker_cancel"
}

/**
 * Selector modal de ubicación sobre el mapa de la aplicación.
 *
 * La mira permanece fija en el centro de la pantalla: al arrastrar el mapa se
 * actualiza la coordenada seleccionada. Un toque sobre cualquier punto también
 * mueve la cámara y coloca la mira en ese lugar. Confirmar entrega la
 * coordenada al formulario; cancelar no modifica el estado previo.
 */
@Composable
fun ManualLocationPickerDialog(
    initialCoordinate: GeoCoordinate,
    onConfirm: (GeoCoordinate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val darkTheme = LocalDarkTheme.current
    val mapBackground = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    var selectedCoordinate by remember { mutableStateOf(initialCoordinate) }

    val mapView = remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", 0)
        )
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(ManualLocationPickerDefaults.INITIAL_ZOOM)
            controller.setCenter(
                GeoPoint(
                    initialCoordinate.latitude,
                    initialCoordinate.longitude
                )
            )
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    selectedCoordinate = currentMapCoordinate()
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    selectedCoordinate = currentMapCoordinate()
                    return false
                }
            })
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    controller.setCenter(p)
                    selectedCoordinate = GeoCoordinate(p.latitude, p.longitude)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))
        }
    }

    DisposableEffect(mapView) {
        onDispose { mapView.onDetach() }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag(ManualLocationPickerTestTags.DIALOG),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Seleccionar ubicación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { mapView },
                        update = { it.applyMapAppearance(darkTheme, mapBackground) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(ManualLocationPickerTestTags.MAP)
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Mira de ubicación",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.page),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Mueve el mapa o toca un punto para ajustar la mira.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = coordinateLabel(selectedCoordinate),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag(
                                ManualLocationPickerTestTags.COORDINATES
                            )
                        )
                        if (!selectedCoordinate.isValid) {
                            Text(
                                text = "Las coordenadas seleccionadas no son válidas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(ManualLocationPickerTestTags.CANCEL)
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = { onConfirm(selectedCoordinate) },
                                enabled = selectedCoordinate.isValid,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(ManualLocationPickerTestTags.CONFIRM)
                            ) {
                                Text("Confirmar")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun MapView.currentMapCoordinate(): GeoCoordinate {
    val center = mapCenter
    return GeoCoordinate(center.latitude, center.longitude)
}

private fun coordinateLabel(coordinate: GeoCoordinate): String = buildString {
    append("Latitud: ")
    append(formatCoordinate(coordinate.latitude))
    append("\nLongitud: ")
    append(formatCoordinate(coordinate.longitude))
}

private fun formatCoordinate(value: Double): String =
    String.format(Locale.US, "%.6f", value)
