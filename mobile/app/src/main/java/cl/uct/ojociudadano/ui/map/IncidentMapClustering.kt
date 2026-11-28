package cl.uct.ojociudadano.ui.map

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** Radio en dp dentro del cual dos marcadores se consideran parte del mismo clúster. */
internal const val CLUSTER_RADIUS_DP = 56.0

/** Niveles de zoom que se acercan al tocar un clúster. */
internal const val CLUSTER_ZOOM_STEP = 2.0

/**
 * Cantidad máxima de acercamientos que se intentan antes de ofrecer el listado
 * de incidentes del clúster.
 *
 * Los incidentes que comparten exactamente la misma coordenada nunca se separan
 * por mucho que se acerque la cámara, por lo que este límite evita un ciclo
 * indefinido de zoom.
 */
internal const val MAX_CLUSTER_ZOOM_JUMPS = 3

/** Duración de la animación al enfocar un clúster. */
internal const val CLUSTER_ZOOM_DURATION_MILLIS = 500L

private const val TILE_SIZE = 256.0
private const val MAX_MERCATOR_LATITUDE = 85.05112878

/**
 * Grupo de marcadores que se dibujan como un único pin con su cantidad.
 *
 * [latitude] y [longitude] representan el centro geográfico del grupo y se usan
 * como posición del marcador de clúster. El orden de [markers] conserva el
 * orden original de los incidentes cargados para que el dibujo sea estable.
 */
data class IncidentCluster(
    val markers: List<IncidentMapMarker>,
    val latitude: Double,
    val longitude: Double
) {
    val count: Int
        get() = markers.size

    /** Identificadores ordenados que identifican de forma estable al clúster. */
    val incidentIds: List<String>
        get() = markers.map { it.incidentId }.sorted()
}

/**
 * Agrupa los marcadores que quedan a menos de [clusterRadiusPixels] entre sí.
 *
 * La distancia se calcula sobre la proyección Web Mercator al [zoomLevel]
 * actual, por lo que el mismo conjunto de incidentes se separa naturalmente a
 * medida que la cámara se acerca. La agrupación es determinista: cada marcador
 * se incorpora al clúster más cercano que ya cumpla el radio y, si no existe,
 * inicia uno nuevo.
 *
 * Un radio menor o igual a cero desactiva la agrupación. En ese caso cada
 * marcador se devuelve como un clúster unitario, lo que permite reutilizar la
 * misma ruta de dibujo sin alterar el comportamiento de mapas que no agrupan.
 */
fun clusterIncidentMarkers(
    markers: List<IncidentMapMarker>,
    zoomLevel: Double,
    clusterRadiusPixels: Double
): List<IncidentCluster> {
    if (markers.isEmpty()) return emptyList()
    if (clusterRadiusPixels <= 0.0 ||
        !clusterRadiusPixels.isFinite() ||
        !zoomLevel.isFinite()
    ) {
        return markers.map { marker ->
            IncidentCluster(
                markers = listOf(marker),
                latitude = marker.latitude,
                longitude = marker.longitude
            )
        }
    }

    val worldSize = TILE_SIZE * 2.0.pow(zoomLevel.coerceIn(0.0, 30.0))
    val projected = markers.map { marker -> marker to marker.toWorldPixel(worldSize) }

    val cellSize = clusterRadiusPixels
    val cells = HashMap<CellKey, MutableList<ClusterAccumulator>>()
    val accumulators = ArrayList<ClusterAccumulator>(markers.size)

    for ((marker, point) in projected) {
        val cellX = floor(point.x / cellSize).toLong()
        val cellY = floor(point.y / cellSize).toLong()

        var nearest: ClusterAccumulator? = null
        var nearestDistance = Double.MAX_VALUE

        // El centro de un clúster puede desplazarse hasta un radio desde su
        // primer marcador; revisar dos celdas alrededor cubre ese margen.
        for (deltaX in -2..2) {
            for (deltaY in -2..2) {
                cells[CellKey(cellX + deltaX, cellY + deltaY)]?.forEach { candidate ->
                    val distance = candidate.distanceTo(point)
                    if (distance <= clusterRadiusPixels && distance < nearestDistance) {
                        nearest = candidate
                        nearestDistance = distance
                    }
                }
            }
        }

        if (nearest != null) {
            nearest.add(marker, point)
        } else {
            val accumulator = ClusterAccumulator(marker, point)
            accumulators.add(accumulator)
            cells.getOrPut(CellKey(cellX, cellY)) { ArrayList() }.add(accumulator)
        }
    }

    return accumulators.map { it.toCluster() }
}

/**
 * Calcula el zoom al que debería acercarse la cámara al tocar [cluster].
 *
 * Se avanza al menos [CLUSTER_ZOOM_STEP] niveles y, si el grupo cabe en un
 * espacio menor que el radio de agrupación, se calcula el zoom necesario para
 * separar sus marcadores. Así un clúster denso no obliga a tocar varias veces.
 */
fun clusterZoomTarget(
    cluster: IncidentCluster,
    currentZoom: Double,
    clusterRadiusPixels: Double,
    maxZoom: Double
): Double {
    val minimumTarget = currentZoom + CLUSTER_ZOOM_STEP
    if (cluster.count < 2 || clusterRadiusPixels <= 0.0) {
        return minimumTarget.coerceAtMost(maxZoom)
    }

    val worldSize = TILE_SIZE * 2.0.pow(currentZoom.coerceIn(0.0, 30.0))
    val points = cluster.markers.map { it.toWorldPixel(worldSize) }
    var maxDistance = 0.0
    for (index in points.indices) {
        for (other in index + 1 until points.size) {
            maxDistance = max(maxDistance, points[index].distanceTo(points[other]))
        }
    }

    if (maxDistance <= 0.0) return minimumTarget.coerceAtMost(maxZoom)

    // Dos marcadores solo pueden compartir clúster si su distancia es menor o
    // igual a dos veces el radio (ambos caben dentro del centro del grupo). Se
    // apunta un poco más allá para que el zoom los separe de una vez.
    val requiredFactor = (clusterRadiusPixels * 2.2) / maxDistance
    val requiredDelta = if (requiredFactor > 1.0) {
        ceil(ln(requiredFactor) / ln(2.0))
    } else {
        0.0
    }
    return max(minimumTarget, currentZoom + requiredDelta).coerceAtMost(maxZoom)
}

/** Acción que corresponde al siguiente toque sobre un clúster. */
internal enum class ClusterTapDecision {
    ZOOM,
    SHOW_INCIDENTS
}

/**
 * Decide si el siguiente toque debe acercar la cámara o abrir el listado.
 *
 * Se permiten hasta [MAX_CLUSTER_ZOOM_JUMPS] acercamientos. Si la cámara ya no
 * puede acercarse más, se muestra el listado de inmediato para no dejar al
 * usuario atrapado en un clúster que nunca se separará.
 */
internal fun clusterTapDecision(
    previousZoomJumps: Int,
    canZoomFurther: Boolean
): ClusterTapDecision =
    if (!canZoomFurther || previousZoomJumps >= MAX_CLUSTER_ZOOM_JUMPS) {
        ClusterTapDecision.SHOW_INCIDENTS
    } else {
        ClusterTapDecision.ZOOM
    }

private data class CellKey(val x: Long, val y: Long)

private data class WorldPixel(val x: Double, val y: Double) {
    fun distanceTo(other: WorldPixel): Double {
        val deltaX = x - other.x
        val deltaY = y - other.y
        return kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
    }
}

private class ClusterAccumulator(
    private val first: IncidentMapMarker,
    firstPoint: WorldPixel
) {
    private val markers = ArrayList<IncidentMapMarker>()
    private var latitudeSum = 0.0
    private var longitudeSum = 0.0
    private var worldXSum = 0.0
    private var worldYSum = 0.0

    init {
        add(first, firstPoint)
    }

    fun add(marker: IncidentMapMarker, point: WorldPixel) {
        markers.add(marker)
        latitudeSum += marker.latitude
        longitudeSum += marker.longitude
        worldXSum += point.x
        worldYSum += point.y
    }

    fun distanceTo(point: WorldPixel): Double =
        centerPoint().distanceTo(point)

    fun toCluster(): IncidentCluster = IncidentCluster(
        markers = markers.toList(),
        latitude = latitudeSum / markers.size,
        longitude = longitudeSum / markers.size
    )

    private fun centerPoint(): WorldPixel = WorldPixel(
        x = worldXSum / markers.size,
        y = worldYSum / markers.size
    )
}

private fun IncidentMapMarker.toWorldPixel(worldSize: Double): WorldPixel {
    val safeLongitude = longitude.coerceIn(-180.0, 180.0)
    val safeLatitude = latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE)
    val x = (safeLongitude + 180.0) / 360.0 * worldSize
    val sinLatitude = sin(safeLatitude * PI / 180.0)
    val y = (0.5 - ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * PI)) * worldSize
    return WorldPixel(x = x, y = y)
}
