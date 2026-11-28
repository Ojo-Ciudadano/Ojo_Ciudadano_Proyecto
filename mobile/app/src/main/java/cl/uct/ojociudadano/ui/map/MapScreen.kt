package cl.uct.ojociudadano.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.uct.ojociudadano.OjoCiudadanoApplication
import cl.uct.ojociudadano.ui.permission.rememberLocationPermissionLauncher
import cl.uct.ojociudadano.ui.reports.incidentDate
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import cl.uct.ojociudadano.ui.components.IncidentPriorityBadge
import cl.uct.ojociudadano.ui.components.IncidentStatusBadge
import cl.uct.ojociudadano.ui.theme.Spacing

/** Centro inicial: plaza de Temuco, Región de La Araucanía. */
private const val DEFAULT_LATITUDE = -38.7359
private const val DEFAULT_LONGITUDE = -72.5904
private const val DEFAULT_ZOOM = 10.0

internal object MapTestTags {
    const val LOADING = "map_state_loading"
    const val EMPTY = "map_state_empty"
    const val ERROR = "map_state_error"
    const val RETRY = "map_state_retry"
    const val SUMMARY = "map_marker_summary"
    const val SUMMARY_CLOSE = "map_marker_summary_close"
    const val SUMMARY_DETAIL = "map_marker_summary_detail"
    const val FILTERS = "map_filters"
    const val FILTER_RESET = "map_filters_reset"
    const val FILTERS_HIDE = "map_filters_hide"
    const val FILTERS_TOGGLE = "map_filters_toggle"
    const val FILTER_VISIBLE_COUNT = "map_filters_visible_count"
    const val FILTER_CATEGORY_PREFIX = "map_filter_category_"
    const val FILTER_STATUS_PREFIX = "map_filter_status_"
    const val FILTERED_EMPTY = "map_state_filtered_empty"
    const val FILTERED_EMPTY_RESET = "map_state_filtered_empty_reset"
    const val LEGEND_TOGGLE = "map_legend_toggle"
    const val LEGEND_PANEL = "map_legend_panel"
    const val LEGEND_CLOSE = "map_legend_close"
    const val LEGEND_STATUS_PREFIX = "map_legend_status_"
    const val LEGEND_PRIORITY_PREFIX = "map_legend_priority_"
    const val CENTER_LOCATION_FAB = "map_center_location_fab"
    const val LOCATION_SNACKBAR = "map_location_snackbar"
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onIncidentDetail: (String) -> Unit = {},
    mapViewModel: MapViewModel? = null
) {
    val application = LocalContext.current.applicationContext as OjoCiudadanoApplication
    val model: MapViewModel = mapViewModel ?: viewModel(factory = remember(application) {
        MapViewModelFactory(
            incidentRepository = application.container.incidentRepository,
            locationClient = application.container.locationClient
        )
    })
    val state by model.uiState.collectAsState()
    val userLocation by model.userLocation.collectAsState()
    val isLocatingUser by model.isLocatingUser.collectAsState()
    val success = state as? MapUiState.Success
    val markers = success?.visibleMarkers.orEmpty()
    val selectedMarker = success?.selectedMarker
    var legendExpanded by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val locationPermissionLauncher = rememberLocationPermissionLauncher(
        onPermissionResult = { granted ->
            if (granted) {
                model.centerOnUserLocation()
            } else {
                model.onLocationPermissionDenied()
            }
        },
        onReturnFromSettings = model::centerOnUserLocation
    )

    LaunchedEffect(model) {
        model.userMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier.fillMaxSize()) {
        IncidentMap(
            markers = markers,
            selectedIncidentId = selectedMarker?.incidentId,
            userLocation = userLocation,
            onMarkerClick = model::selectIncident,
            onMapClick = model::clearSelection,
            modifier = Modifier.fillMaxSize()
        )
        if (success != null) {
            val hasFilters =
                success.categoryOptions.isNotEmpty() || success.statusOptions.isNotEmpty()
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 480.dp)
                    .padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapLegendToggle(
                        expanded = legendExpanded,
                        onClick = { legendExpanded = !legendExpanded }
                    )
                    if (hasFilters && !success.filtersVisible) {
                        MapFilterToggle(
                            activeFilterCount = success.filters.activeCount,
                            onClick = { model.setFiltersVisible(true) }
                        )
                    }
                }
                if (legendExpanded) {
                    MapLegendPanel(
                        onClose = { legendExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (hasFilters && success.filtersVisible) {
                    MapIncidentFilterBar(
                        filters = success.filters,
                        categoryOptions = success.categoryOptions,
                        statusOptions = success.statusOptions,
                        visibleCount = success.visibleMarkers.size,
                        totalCount = success.markers.size,
                        onCategorySelected = model::selectCategory,
                        onStatusSelected = model::selectStatus,
                        onReset = model::clearFilters,
                        onHide = { model.setFiltersVisible(false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        MapStatusOverlay(
            state = state,
            onRetry = model::loadIncidents,
            onClearFilters = model::clearFilters,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium)
        )
        selectedMarker?.let { marker ->
            IncidentSummaryCard(
                marker = marker,
                onClose = model::clearSelection,
                onOpenDetail = { requestedIncidentId ->
                    success?.selectedDetailIncidentId(requestedIncidentId)?.let(onIncidentDetail)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.medium)
            )
        }
        MapCenterLocationFab(
            isLoading = isLocatingUser,
            onClick = {
                if (locationPermissionLauncher.isGranted()) {
                    model.centerOnUserLocation()
                } else {
                    locationPermissionLauncher.request()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Spacing.medium)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.medium)
                .testTag(MapTestTags.LOCATION_SNACKBAR)
        )
    }
}

@Composable
private fun IncidentMap(
    markers: List<IncidentMapMarker>,
    selectedIncidentId: String?,
    userLocation: MapUserLocation?,
    onMarkerClick: (String) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentMarkers by rememberUpdatedState(markers)
    val currentSelectedIncidentId by rememberUpdatedState(selectedIncidentId)
    val incidentIconCache = remember(context) {
        IncidentMarkerIconProvider.cacheFor(context)
    }
    val clusterIconCache = remember(context) {
        IncidentMarkerIconProvider.clusterCacheFor(context)
    }
    val clusterRadiusPx = remember(context) {
        CLUSTER_RADIUS_DP * context.resources.displayMetrics.density
    }
    val mapView = remember(context) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", 0)
        )
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            // Se agrega antes que los marcadores: al despachar eventos en orden
            // inverso, un toque sobre un marcador se consume primero y solo los
            // toques sobre el mapa vacío llegan a este receptor.
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    currentOnMapClick()
                    return false
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))
        }
    }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean = false

            override fun onZoom(event: ZoomEvent): Boolean {
                // El agrupamiento depende del zoom: al acercar o alejar la
                // cámara se recalculan los clústeres con los marcadores y
                // filtros vigentes.
                syncIncidentMarkers(
                    mapView = mapView,
                    desiredMarkers = currentMarkers,
                    onMarkerClick = currentOnMarkerClick,
                    selectedIncidentId = currentSelectedIncidentId,
                    iconCache = incidentIconCache,
                    clusterIconCache = clusterIconCache,
                    clusterRadiusPx = clusterRadiusPx
                )
                return false
            }
        }
        mapView.addMapListener(listener)

        onDispose {
            mapView.removeMapListener(listener)
            mapView.onDetach()
        }
    }

    LaunchedEffect(mapView, userLocation?.focusRequestId) {
        val target = userLocation ?: return@LaunchedEffect
        mapView.post {
            animateMapToUserLocation(mapView, target.coordinate)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { currentMap ->
            syncIncidentMarkers(
                mapView = currentMap,
                desiredMarkers = markers,
                onMarkerClick = onMarkerClick,
                selectedIncidentId = selectedIncidentId,
                iconCache = incidentIconCache,
                clusterIconCache = clusterIconCache,
                clusterRadiusPx = clusterRadiusPx
            )
            syncUserLocationMarker(currentMap, userLocation)
        }
    )
}

/**
 * Sincroniza los marcadores de incidentes y sus clústeres sobre el [MapView].
 *
 * El identificador del incidente se guarda en `relatedObject` y se usa como
 * clave: los marcadores existentes se reutilizan, los que ya no corresponden se
 * eliminan y cualquier duplicado previo se descarta. Así las actualizaciones
 * normales del estado no acumulan marcadores.
 *
 * Cuando [clusterRadiusPx] es mayor que cero, los incidentes cercanos se
 * representan con un único pin que muestra su cantidad. La selección se
 * conserva en [MapUiState.Success.selectedIncidentId]: si el zoom actual
 * permite ver el pin separado, se dibuja resaltado al final para quedar por
 * encima del resto.
 *
 * Los íconos se obtienen de [iconCache] y [clusterIconCache], por lo que solo se
 * renderiza un pin nuevo cuando cambia el estado, la prioridad, la selección o
 * la cantidad de incidentes agrupados.
 */
internal fun syncIncidentMarkers(
    mapView: MapView,
    desiredMarkers: List<IncidentMapMarker>,
    onMarkerClick: (String) -> Unit = {},
    selectedIncidentId: String? = null,
    iconCache: IncidentMarkerIconCache = IncidentMarkerIconProvider.cacheFor(mapView.context),
    clusterIconCache: IncidentClusterIconCache =
        IncidentMarkerIconProvider.clusterCacheFor(mapView.context),
    clusterRadiusPx: Double = 0.0
) {
    val desiredById = desiredMarkers.associateBy { it.incidentId }
    val selectedId = selectedIncidentId?.takeIf { it in desiredById }

    val clusters = clusterIncidentMarkers(
        markers = desiredById.values.toList(),
        zoomLevel = mapView.zoomLevelDouble,
        clusterRadiusPixels = clusterRadiusPx
    )
    val groupedClusters = clusters.filter { it.count > 1 }
    val individualById = LinkedHashMap<String, IncidentMapMarker>()
    clusters.filter { it.count == 1 }.forEach { cluster ->
        val marker = cluster.markers.single()
        individualById[marker.incidentId] = marker
    }

    val currentById = HashMap<String, Marker>()
    val duplicateMarkers = ArrayList<Marker>()
    val currentClusters = HashMap<List<String>, IncidentClusterMarker>()
    val duplicateClusters = ArrayList<IncidentClusterMarker>()

    mapView.overlays.forEach { overlay ->
        if (overlay is IncidentClusterMarker) {
            if (currentClusters.putIfAbsent(overlay.clusterIds, overlay) != null) {
                duplicateClusters.add(overlay)
            }
        } else if (overlay is Marker) {
            val incidentId = overlay.relatedObject as? String
            if (incidentId != null) {
                if (currentById.putIfAbsent(incidentId, overlay) != null) {
                    duplicateMarkers.add(overlay)
                }
            }
        }
    }

    var changed = duplicateMarkers.isNotEmpty() || duplicateClusters.isNotEmpty()
    duplicateMarkers.forEach { mapView.overlays.remove(it) }
    duplicateClusters.forEach { mapView.overlays.remove(it) }

    currentById.forEach { (incidentId, marker) ->
        if (incidentId !in individualById) {
            mapView.overlays.remove(marker)
            changed = true
        }
    }

    individualById.forEach { (incidentId, data) ->
        val visual = data.visual(selected = incidentId == selectedId)
        val previous = currentById[incidentId]
        when {
            previous == null -> {
                mapView.overlays.add(
                    createIncidentMarker(mapView, data, visual, iconCache, onMarkerClick)
                )
                changed = true
            }

            previous !is IncidentMarker -> {
                // Reemplaza marcadores creados por versiones anteriores para que
                // también adopten el ícono personalizado y el estado de selección.
                mapView.overlays.remove(previous)
                mapView.overlays.add(
                    createIncidentMarker(mapView, data, visual, iconCache, onMarkerClick)
                )
                changed = true
            }

            else -> {
                previous.setOnMarkerClickListener { _, _ ->
                    onMarkerClick(incidentId)
                    true
                }
                val position = previous.position
                if (position.latitude != data.latitude || position.longitude != data.longitude) {
                    previous.position = GeoPoint(data.latitude, data.longitude)
                    changed = true
                }
                if (previous.visual != visual) {
                    previous.setIcon(iconCache.iconFor(visual))
                    previous.visual = visual
                    changed = true
                }
            }
        }
    }

    val desiredClustersByKey = groupedClusters.associateBy { it.incidentIds }
    currentClusters.forEach { (key, marker) ->
        if (key !in desiredClustersByKey) {
            mapView.overlays.remove(marker)
            changed = true
        }
    }

    desiredClustersByKey.forEach { (key, cluster) ->
        val previous = currentClusters[key]
        if (previous == null) {
            mapView.overlays.add(
                createIncidentClusterMarker(
                    mapView = mapView,
                    cluster = cluster,
                    iconCache = clusterIconCache,
                    clusterRadiusPx = clusterRadiusPx
                )
            )
            changed = true
        } else {
            previous.setOnMarkerClickListener { _, _ ->
                zoomIntoIncidentCluster(mapView, cluster, clusterRadiusPx)
                true
            }
            val position = previous.position
            if (position.latitude != cluster.latitude ||
                position.longitude != cluster.longitude
            ) {
                previous.position = GeoPoint(cluster.latitude, cluster.longitude)
                changed = true
            }
            if (previous.count != cluster.count) {
                previous.count = cluster.count
                previous.title = clusterTitle(cluster.count)
                previous.setIcon(clusterIconCache.iconFor(cluster.count))
                changed = true
            }
        }
    }

    selectedId?.let { id ->
        val selectedOverlay = mapView.overlays.firstOrNull { overlay ->
            overlay is Marker && overlay.relatedObject == id
        }
        if (selectedOverlay != null && mapView.overlays.lastOrNull() !== selectedOverlay) {
            mapView.overlays.remove(selectedOverlay)
            mapView.overlays.add(selectedOverlay)
            changed = true
        }
    }

    if (changed) {
        mapView.invalidate()
    }
}

/** Marcador que recuerda la apariencia aplicada para evitar re-renderizarla. */
private class IncidentMarker(mapView: MapView) : Marker(mapView) {
    var visual: IncidentMarkerVisual? = null
}

/**
 * Marcador que representa varios incidentes agrupados.
 *
 * [clusterIds] identifica de forma estable la composición del grupo; si cambia
 * por un filtro o por el zoom, el marcador anterior se retira y se crea uno
 * nuevo para no dejar conteos ni posiciones fantasma.
 */
internal class IncidentClusterMarker(mapView: MapView) : Marker(mapView) {
    var clusterIds: List<String> = emptyList()
    var count: Int = 0
}

private fun createIncidentClusterMarker(
    mapView: MapView,
    cluster: IncidentCluster,
    iconCache: IncidentClusterIconCache,
    clusterRadiusPx: Double
): IncidentClusterMarker = IncidentClusterMarker(mapView).apply {
    position = GeoPoint(cluster.latitude, cluster.longitude)
    clusterIds = cluster.incidentIds
    count = cluster.count
    title = clusterTitle(cluster.count)
    snippet = "Toca para acercar"
    setIcon(iconCache.iconFor(cluster.count))
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setOnMarkerClickListener { _, _ ->
        zoomIntoIncidentCluster(mapView, cluster, clusterRadiusPx)
        true
    }
}

private fun clusterTitle(count: Int): String =
    if (count == 1) "1 incidente agrupado" else "$count incidentes agrupados"

/**
 * Acerca la cámara al centro de [cluster] hasta un zoom que permita separar sus
 * marcadores. La animación dispara el [MapListener] del mapa, que recalcula los
 * clústeres con el nuevo nivel de zoom.
 */
internal fun zoomIntoIncidentCluster(
    mapView: MapView,
    cluster: IncidentCluster,
    clusterRadiusPx: Double = CLUSTER_RADIUS_DP * mapView.context.resources.displayMetrics.density
) {
    val targetZoom = clusterZoomTarget(
        cluster = cluster,
        currentZoom = mapView.zoomLevelDouble,
        clusterRadiusPixels = clusterRadiusPx,
        maxZoom = mapView.maxZoomLevel
    )
    mapView.controller.animateTo(
        GeoPoint(cluster.latitude, cluster.longitude),
        targetZoom,
        CLUSTER_ZOOM_DURATION_MILLIS
    )
}

private fun createIncidentMarker(
    mapView: MapView,
    data: IncidentMapMarker,
    visual: IncidentMarkerVisual,
    iconCache: IncidentMarkerIconCache,
    onMarkerClick: (String) -> Unit
): Marker = IncidentMarker(mapView).apply {
    position = GeoPoint(data.latitude, data.longitude)
    relatedObject = data.incidentId
    this.visual = visual
    setIcon(iconCache.iconFor(visual))
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    setOnMarkerClickListener { _, _ ->
        onMarkerClick(data.incidentId)
        true
    }
}

/**
 * Tarjeta con la información resumida del incidente seleccionado.
 *
 * Solo pinta los campos disponibles; si la respuesta viene parcial muestra un
 * mensaje controlado en lugar de filas vacías.
 */
@Composable
internal fun IncidentSummaryCard(
    marker: IncidentMapMarker,
    onClose: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = marker.summary
    val hasDetails = listOfNotNull(
        summary.title,
        summary.description,
        summary.statusName,
        summary.createdAt,
        summary.referenceAddress
    ).isNotEmpty()

    Surface(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .testTag(MapTestTags.SUMMARY),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    summary.categoryName ?: "Incidente seleccionado",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.testTag(MapTestTags.SUMMARY_CLOSE)
                ) {
                    Text("Cerrar")
                }
            }

            summary.title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            summary.statusName?.let {
                IncidentStatusBadge(it)
            }

            IncidentPriorityBadge(summary.priorityLevel)

            summary.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            summary.referenceAddress?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            summary.createdAt?.let {
                Text(
                    "Reportado el ${incidentDate(it)}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (!hasDetails) {
                Text(
                    "El incidente no tiene información resumida disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { onOpenDetail(marker.incidentId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(MapTestTags.SUMMARY_DETAIL)
            ) {
                Text("Ver detalle del incidente")
            }
        }
    }
}

@Composable
private fun MapStatusOverlay(
    state: MapUiState,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredEmpty = (state as? MapUiState.Success)?.isFilteredEmpty == true
    Box(
        modifier = modifier,
        contentAlignment = if (state == MapUiState.Loading || filteredEmpty) {
            Alignment.Center
        } else {
            Alignment.TopCenter
        }
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

            is MapUiState.Success -> if (state.isFilteredEmpty) {
                MapStatusCard(Modifier.testTag(MapTestTags.FILTERED_EMPTY)) {
                    Text(
                        "Ningún incidente coincide con los filtros seleccionados.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag(MapTestTags.FILTERED_EMPTY_RESET)
                    ) {
                        Text("Restablecer filtros")
                    }
                }
            }
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
        shape = MaterialTheme.shapes.medium,
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            content()
        }
    }
}
