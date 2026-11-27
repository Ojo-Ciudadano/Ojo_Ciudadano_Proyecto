package cl.uct.ojociudadano.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cl.uct.ojociudadano.domain.model.GeoCoordinate
import cl.uct.ojociudadano.domain.model.LocationResult
import cl.uct.ojociudadano.ui.theme.Spacing
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

internal object LocationPreviewTestTags {
    const val CARD = "location_preview_card"
    const val MAP = "location_preview_map"
    const val COORDINATES = "location_preview_coordinates"
    const val ADDRESS = "location_preview_address"
    const val SOURCE = "location_preview_source"
    const val PENDING = "location_preview_pending"
}

private const val PREVIEW_ZOOM = 16.0
private val PREVIEW_MAP_HEIGHT = 148.dp

/**
 * Tarjeta de resumen geográfico del formulario de nuevo reporte.
 *
 * Con una coordenada válida muestra un mini-mapa no interactivo centrado en el
 * punto, sus coordenadas formateadas, la dirección referencial disponible y el
 * origen de la ubicación. Toda la tarjeta abre el selector de mapa de #299 para
 * corregir el punto. Sin coordenadas se muestra un estado pendiente con la
 * acción que corresponda al problema actual.
 */
@Composable
internal fun LocationPreviewCard(
    state: CreateReportFormState,
    canEditForm: Boolean,
    onOpenPicker: () -> Unit,
    onRefreshLocation: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = state.locationPreviewContent()
    if (preview != null) {
        ConfirmedLocationCard(
            content = preview,
            enabled = canEditForm,
            onClick = onOpenPicker,
            modifier = modifier
        )
    } else {
        PendingLocationCard(
            isLoading = state.isLoadingLocation,
            errorMessage = state.locationError,
            location = state.location,
            enabled = canEditForm,
            onRefreshLocation = onRefreshLocation,
            onRequestLocationPermission = onRequestLocationPermission,
            onOpenLocationSettings = onOpenLocationSettings,
            modifier = modifier
        )
    }
}

@Composable
private fun ConfirmedLocationCard(
    content: LocationPreviewContent,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            Column {
                LocationPreviewMap(
                    coordinate = content.coordinate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_MAP_HEIGHT)
                )

                Column(
                    modifier = Modifier.padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
                ) {
                    Text(
                        text = "Ubicación confirmada",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = content.coordinatesLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(
                            LocationPreviewTestTags.COORDINATES
                        )
                    )
                    Text(
                        text = content.addressLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(LocationPreviewTestTags.ADDRESS)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Icon(
                            imageVector = if (content.source ==
                                LocationSelectionSource.MANUAL
                            ) {
                                Icons.Filled.Map
                            } else {
                                Icons.Filled.MyLocation
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = content.sourceLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .testTag(LocationPreviewTestTags.SOURCE)
                        )
                        if (enabled) {
                            Text(
                                text = "Toca para ajustar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // La capa táctil cubre también el mini-mapa: el gesto nunca llega a
            // osmdroid, por lo que el desplazamiento vertical del formulario
            // conserva su comportamiento y el toque abre el selector.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag(LocationPreviewTestTags.CARD)
                    .clickable(enabled = enabled, onClick = onClick)
            )
        }
    }
}

@Composable
private fun PendingLocationCard(
    isLoading: Boolean,
    errorMessage: String?,
    location: LocationResult,
    enabled: Boolean,
    onRefreshLocation: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val action = when {
        isLoading -> null
        location is LocationResult.MissingPermissions ->
            "Dar permiso" to onRequestLocationPermission
        location is LocationResult.NotAvailable ->
            "Activar ubicación" to onOpenLocationSettings
        else -> "Reintentar" to onRefreshLocation
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(LocationPreviewTestTags.PENDING),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Text(
                    text = if (isLoading) {
                        "Obteniendo ubicación..."
                    } else {
                        "Ubicación pendiente"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        isLoading -> "Espera mientras se obtiene tu posición actual."
                        errorMessage != null -> errorMessage
                        else -> "Aún no se ha fijado la ubicación del reporte."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isLoading && errorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (!isLoading) {
                    Text(
                        text = "Usa tu GPS o elige un punto en el mapa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            action?.let { (label, onClick) ->
                TextButton(onClick = onClick, enabled = enabled) {
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun LocationPreviewMap(
    coordinate: GeoCoordinate,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember(context) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", 0)
        )
        MapView(context).apply {
            // El mini-mapa solo representa el punto: los gestos y el foco se
            // desactivan para no competir con el scroll del formulario.
            setMultiTouchControls(false)
            setBuiltInZoomControls(false)
            isEnabled = false
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            controller.setZoom(PREVIEW_ZOOM)
            controller.setCenter(coordinate.toGeoPoint())
        }
    }
    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Ubicación del reporte"
            position = coordinate.toGeoPoint()
            mapView.overlays.add(this)
        }
    }

    DisposableEffect(mapView) {
        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(mapView, coordinate) {
        val point = coordinate.toGeoPoint()
        marker.position = point
        mapView.controller.setCenter(point)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.testTag(LocationPreviewTestTags.MAP)
    )
}

private fun GeoCoordinate.toGeoPoint(): GeoPoint =
    GeoPoint(latitude, longitude)
