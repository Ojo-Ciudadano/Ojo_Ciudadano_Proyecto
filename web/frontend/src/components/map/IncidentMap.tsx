import {
  Component,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type MutableRefObject,
  type ReactNode,
} from 'react'
import * as L from 'leaflet'
import {
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  Tooltip,
  useMap,
} from 'react-leaflet'
import MarkerClusterGroup from 'react-leaflet-cluster'
import 'leaflet/dist/leaflet.css'
import 'react-leaflet-cluster/dist/assets/MarkerCluster.css'
import 'react-leaflet-cluster/dist/assets/MarkerCluster.Default.css'
import {
  ARAUCANIA_BOUNDS,
  ARAUCANIA_CENTER,
  DEFAULT_ZOOM,
  MAX_ZOOM,
  MIN_ZOOM,
  OSM_ATTRIBUTION,
  OSM_TILE_URL,
  TILE_ERROR_MESSAGE,
  colorPorPrioridad,
  normalizarMarcadores,
  type IncidentMapMarker,
} from './mapConfig'
import {
  crearContenidoPopup,
  crearContenidoTooltip,
  resolverIncidenteNavegacion,
} from './incidentPopup'
import {
  debeCerrarPopupMapa,
  sincronizarIncidenteSeleccionado,
} from './incidentMarkers'
import './IncidentMap.css'

const EMPTY_MARKERS: readonly IncidentMapMarker[] = []
const TILE_ERRORES_PARA_AVISO = 4
const RADIO_MARCADOR = 9

type SelectorMarcador = (markerId: string | null) => void

/** Identifica una sola operación de `zoomToShowLayer` y su generación. */
interface OperacionZoomPendiente {
  readonly id: string
  readonly token: number
}

interface IncidentMapProps {
  /**
   * Capa de marcadores. La pantalla puede pasar datos sin reconstruir el mapa:
   * React-Leaflet actualiza las capas declarativas en cada cambio.
   */
  markers?: readonly IncidentMapMarker[]
  /**
   * Acción invocada cuando el usuario solicita abrir el detalle del incidente
   * desde el popup del marcador seleccionado (#142).
   */
  onOpenIncidentDetail?: (incidentId: string) => void
  /** Identificador opcional del incidente cuyo popup se encuentra activo. */
  selectedMarkerId?: string | null
  /** Notifica cuando se abre o cierra el popup de un marcador. */
  onSelectMarker?: SelectorMarcador
  className?: string
}

/**
 * Icono DOM que reproduce el circleMarker original (radio 9, borde blanco de
 * 2 px y relleno por prioridad). MarkerCluster depende de `_icon` para
 * `zoomToShowLayer`, `getVisibleParent` y las animaciones, de modo que un
 * `L.Marker` con `DivIcon` es la alternativa compatible indicada para agrupar
 * sin perder la apariencia ni la reapertura de popups.
 */
function crearIconoIncidente(marcador: IncidentMapMarker): L.DivIcon {
  const circulo = document.createElement('span')
  circulo.className = 'incident-map__marker-circulo'
  circulo.style.backgroundColor = colorPorPrioridad(marcador.prioridad)

  return L.divIcon({
    className: 'incident-map__marker',
    html: circulo,
    iconSize: [RADIO_MARCADOR * 2, RADIO_MARCADOR * 2],
    iconAnchor: [RADIO_MARCADOR, RADIO_MARCADOR],
  })
}

/** Inserta el tooltip seguro de `incidentPopup.ts` dentro del Tooltip declarativo. */
function ContenidoTooltipSeguro({ marcador }: { marcador: IncidentMapMarker }) {
  const contenedorRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const contenedor = contenedorRef.current
    if (!contenedor) {
      return
    }
    contenedor.replaceChildren(crearContenidoTooltip(marcador, document))
    return () => contenedor.replaceChildren()
  }, [marcador])

  return <div ref={contenedorRef} />
}

/** Inserta el popup seguro de `incidentPopup.ts` dentro del Popup declarativo. */
function ContenidoPopupSeguro({
  marcador,
  onVerDetalle,
}: {
  marcador: IncidentMapMarker
  onVerDetalle: (incidentId: string) => void
}) {
  const contenedorRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const contenedor = contenedorRef.current
    if (!contenedor) {
      return
    }
    contenedor.replaceChildren(
      crearContenidoPopup(marcador, document, { onVerDetalle }),
    )
    return () => contenedor.replaceChildren()
  }, [marcador, onVerDetalle])

  return <div ref={contenedorRef} />
}

interface MarcadorIncidenteProps {
  marcador: IncidentMapMarker
  registrar: (id: string, layer: L.Marker | null) => void
  onPopupOpen: (id: string) => void
  onPopupClose: (id: string) => void
  onVerDetalle: (incidentId: string) => void
}

function MarcadorIncidente({
  marcador,
  registrar,
  onPopupOpen,
  onPopupClose,
  onVerDetalle,
}: MarcadorIncidenteProps) {
  const asignarCapa = useCallback(
    (layer: L.Marker | null) => registrar(marcador.id, layer),
    [registrar, marcador.id],
  )

  const eventHandlers = useMemo(
    () => ({
      popupopen: () => onPopupOpen(marcador.id),
      popupclose: () => onPopupClose(marcador.id),
    }),
    [marcador.id, onPopupOpen, onPopupClose],
  )

  const icono = useMemo(() => crearIconoIncidente(marcador), [marcador])

  return (
    <Marker
      ref={asignarCapa}
      position={[marcador.latitud, marcador.longitud]}
      icon={icono}
      eventHandlers={eventHandlers}
    >
      <Tooltip direction="top" offset={[0, -8]}>
        <ContenidoTooltipSeguro marcador={marcador} />
      </Tooltip>
      <Popup>
        <ContenidoPopupSeguro
          marcador={marcador}
          onVerDetalle={onVerDetalle}
        />
      </Popup>
    </Marker>
  )
}

interface SincronizadorMapaProps {
  marcadores: readonly IncidentMapMarker[]
  selectedMarkerId?: string | null
  retryKey: number
  openPopupIncidentIdRef: MutableRefObject<string | null>
  isSyncingRef: MutableRefObject<boolean>
  markerLayersRef: MutableRefObject<Map<string, L.Marker>>
  clusterRef: MutableRefObject<L.MarkerClusterGroup | null>
  onSelectMarkerRef: MutableRefObject<SelectorMarcador | undefined>
  ultimoIdSolicitadoRef: MutableRefObject<string | null>
  ultimoIdMostradoRef: MutableRefObject<string | null>
  operacionPendienteRef: MutableRefObject<OperacionZoomPendiente | null>
  tokenOperacionRef: MutableRefObject<number>
}

/** Mantiene el viewport, la selección y el popup sincronizados con las props. */
function SincronizadorMapa({
  marcadores,
  selectedMarkerId,
  retryKey,
  openPopupIncidentIdRef,
  isSyncingRef,
  markerLayersRef,
  clusterRef,
  onSelectMarkerRef,
  ultimoIdSolicitadoRef,
  ultimoIdMostradoRef,
  operacionPendienteRef,
  tokenOperacionRef,
}: SincronizadorMapaProps) {
  const map = useMap()

  useEffect(() => {
    const contenedor = map.getContainer()
    contenedor.setAttribute('role', 'application')
    contenedor.setAttribute(
      'aria-label',
      'Mapa interactivo de incidentes de la Región de La Araucanía',
    )

    const invalidateSize = () => map.invalidateSize()
    const frame = requestAnimationFrame(invalidateSize)

    let resizeObserver: ResizeObserver | null = null
    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(invalidateSize)
      resizeObserver.observe(contenedor)
    } else {
      window.addEventListener('resize', invalidateSize)
    }

    return () => {
      cancelAnimationFrame(frame)
      resizeObserver?.disconnect()
      window.removeEventListener('resize', invalidateSize)
      // MarkerCluster no permite cancelar zoomToShowLayer: invalidar el token
      // hace inofensivo cualquier callback que llegue tras desmontar/reintentar.
      tokenOperacionRef.current += 1
      operacionPendienteRef.current = null
      ultimoIdSolicitadoRef.current = null
      ultimoIdMostradoRef.current = null
    }
  }, [
    map,
    retryKey,
    tokenOperacionRef,
    operacionPendienteRef,
    ultimoIdSolicitadoRef,
    ultimoIdMostradoRef,
  ])

  useEffect(() => {
    const idAbierto = openPopupIncidentIdRef.current
    const idActivo = selectedMarkerId !== undefined ? selectedMarkerId : idAbierto
    const idVigente = sincronizarIncidenteSeleccionado(idActivo, marcadores)

    // Un cambio de selección invalida la operación anterior aunque todavía no
    // se haya abierto ningún popup (por ejemplo, durante una animación).
    if (ultimoIdSolicitadoRef.current !== idActivo) {
      ultimoIdSolicitadoRef.current = idActivo
      tokenOperacionRef.current += 1
      operacionPendienteRef.current = null
      ultimoIdMostradoRef.current = null
    }

    // Solo se cierra el popup cuando el marcador abierto desaparece o deja de
    // ser la selección vigente. Una selección cuyo marcador aún no llegó desde
    // la API no debe interpretarse como deselección.
    const popupObsoleto =
      idAbierto !== null &&
      (idAbierto !== idVigente || debeCerrarPopupMapa(idAbierto, marcadores))

    if (popupObsoleto) {
      tokenOperacionRef.current += 1
      operacionPendienteRef.current = null
      ultimoIdMostradoRef.current = null

      // Evita que el popupclose provocado por esta sincronización notifique
      // una deselección duplicada (#143).
      isSyncingRef.current = true
      map.closePopup()
      isSyncingRef.current = false
      openPopupIncidentIdRef.current = null
      if (idAbierto === idActivo && selectedMarkerId !== null) {
        onSelectMarkerRef.current?.(null)
      }
    } else {
      openPopupIncidentIdRef.current = idVigente
    }

    if (!idVigente) {
      return
    }

    // MEDIA-1: si la selección no cambió y su popup ya está abierto, una
    // actualización de `marcadores` no debe mover el viewport ni reabrirlo.
    if (
      ultimoIdMostradoRef.current === idVigente &&
      openPopupIncidentIdRef.current === idVigente
    ) {
      return
    }

    const capa = markerLayersRef.current.get(idVigente)
    if (!capa) {
      // El marcador seleccionado todavía no se registró en esta instancia;
      // se reintentará cuando llegue en una actualización posterior.
      return
    }

    const cluster = clusterRef.current
    if (!cluster || !map.hasLayer(cluster)) {
      capa.openPopup()
      ultimoIdMostradoRef.current = idVigente
      return
    }

    // Comprobación pública de visibilidad: si la capa individual ya está en el
    // mapa y dentro de los límites, se abre sin pasar por zoomToShowLayer().
    if (map.hasLayer(capa) && map.getBounds().contains(capa.getLatLng())) {
      capa.openPopup()
      ultimoIdMostradoRef.current = idVigente
      return
    }

    // MEDIA-2: una sola operación vigente por marcador. Los callbacks se
    // identifican con un token que se invalida al cambiar selección, cerrar el
    // popup, eliminar el marcador, desmontar o reintentar.
    if (operacionPendienteRef.current?.id === idVigente) {
      return
    }

    tokenOperacionRef.current += 1
    const token = tokenOperacionRef.current
    operacionPendienteRef.current = { id: idVigente, token }

    // Si el incidente quedó dentro de un clúster, MarkerCluster amplía el zoom
    // (o hace spiderfy) antes de abrir el popup.
    cluster.zoomToShowLayer(capa, () => {
      const pendiente = operacionPendienteRef.current
      // Solo la generación vigente puede abrir el popup: así un callback de una
      // selección anterior o de un mapa ya remontado queda sin efecto.
      if (!pendiente || pendiente.token !== token || pendiente.id !== idVigente) {
        return
      }
      operacionPendienteRef.current = null
      if (openPopupIncidentIdRef.current !== idVigente) {
        return
      }
      capa.openPopup()
      ultimoIdMostradoRef.current = idVigente
    })
  }, [
    map,
    marcadores,
    selectedMarkerId,
    retryKey,
    openPopupIncidentIdRef,
    isSyncingRef,
    markerLayersRef,
    clusterRef,
    onSelectMarkerRef,
    ultimoIdSolicitadoRef,
    ultimoIdMostradoRef,
    operacionPendienteRef,
    tokenOperacionRef,
  ])

  return null
}

interface MapErrorBoundaryProps {
  children: ReactNode
  onMapError: () => void
}

/** Conserva el aviso de error original cuando Leaflet no puede inicializarse. */
class MapErrorBoundary extends Component<
  MapErrorBoundaryProps,
  { fallo: boolean }
> {
  state = { fallo: false }

  static getDerivedStateFromError(): { fallo: boolean } {
    return { fallo: true }
  }

  componentDidCatch(): void {
    this.props.onMapError()
  }

  render(): ReactNode {
    return this.state.fallo ? null : this.props.children
  }
}

export function IncidentMap({
  markers = EMPTY_MARKERS,
  onOpenIncidentDetail,
  selectedMarkerId,
  onSelectMarker,
  className,
}: IncidentMapProps) {
  const openPopupIncidentIdRef = useRef<string | null>(null)
  const isSyncingRef = useRef(false)
  const markerLayersRef = useRef(new Map<string, L.Marker>())
  const clusterRef = useRef<L.MarkerClusterGroup | null>(null)
  const ultimoIdSolicitadoRef = useRef<string | null>(null)
  const ultimoIdMostradoRef = useRef<string | null>(null)
  const operacionPendienteRef = useRef<OperacionZoomPendiente | null>(null)
  const tokenOperacionRef = useRef(0)
  const erroresTilesConsecutivosRef = useRef(0)
  const onSelectMarkerRef = useRef(onSelectMarker)
  const onOpenDetailRef = useRef(onOpenIncidentDetail)
  const [tileError, setTileError] = useState(false)
  const [retryKey, setRetryKey] = useState(0)

  const marcadoresNormalizados = useMemo(
    () => normalizarMarcadores(markers),
    [markers],
  )

  useEffect(() => {
    onSelectMarkerRef.current = onSelectMarker
  }, [onSelectMarker])

  useEffect(() => {
    onOpenDetailRef.current = onOpenIncidentDetail
  }, [onOpenIncidentDetail])

  const handleRetry = useCallback(() => {
    // MEDIA-3: se reinician solo referencias internas del mapa. La selección
    // controlada (`selectedMarkerId`) no se toca y se reaplicará al remontar.
    openPopupIncidentIdRef.current = null
    ultimoIdSolicitadoRef.current = null
    ultimoIdMostradoRef.current = null
    operacionPendienteRef.current = null
    tokenOperacionRef.current += 1
    markerLayersRef.current = new Map<string, L.Marker>()
    clusterRef.current = null
    setTileError(false)
    erroresTilesConsecutivosRef.current = 0
    setRetryKey((current) => current + 1)
  }, [])

  const registrarMarcador = useCallback((id: string, layer: L.Marker | null) => {
    if (layer) {
      markerLayersRef.current.set(id, layer)
    } else {
      markerLayersRef.current.delete(id)
    }
  }, [])

  const manejarPopupOpen = useCallback((id: string) => {
    openPopupIncidentIdRef.current = id
    ultimoIdSolicitadoRef.current = id
    ultimoIdMostradoRef.current = id
    // Un popup abierto por el usuario sustituye cualquier zoom pendiente.
    tokenOperacionRef.current += 1
    operacionPendienteRef.current = null
    onSelectMarkerRef.current?.(id)
  }, [])

  const manejarPopupClose = useCallback((id: string) => {
    if (isSyncingRef.current) {
      return
    }
    if (openPopupIncidentIdRef.current === id) {
      // El cierre manual también invalida cualquier zoom pendiente.
      tokenOperacionRef.current += 1
      operacionPendienteRef.current = null
      openPopupIncidentIdRef.current = null
      onSelectMarkerRef.current?.(null)
    }
  }, [])

  const manejarVerDetalle = useCallback((incidenteSolicitadoId: string) => {
    const idDestino = resolverIncidenteNavegacion(
      { incidenteSeleccionadoId: openPopupIncidentIdRef.current },
      incidenteSolicitadoId,
    )
    if (idDestino) {
      onOpenDetailRef.current?.(idDestino)
    }
  }, [])

  const manejarTileError = useCallback(() => {
    erroresTilesConsecutivosRef.current += 1
    if (erroresTilesConsecutivosRef.current >= TILE_ERRORES_PARA_AVISO) {
      setTileError(true)
    }
  }, [])

  const manejarTileLoad = useCallback(() => {
    erroresTilesConsecutivosRef.current = 0
    setTileError(false)
  }, [])

  const manejarFalloMapa = useCallback(() => setTileError(true), [])

  const rootClassName = ['incident-map', className].filter(Boolean).join(' ')

  return (
    <div className={rootClassName}>
      <MapErrorBoundary key={retryKey} onMapError={manejarFalloMapa}>
        <MapContainer
          className="incident-map__canvas"
          center={ARAUCANIA_CENTER}
          zoom={DEFAULT_ZOOM}
          minZoom={MIN_ZOOM}
          maxZoom={MAX_ZOOM}
          maxBounds={ARAUCANIA_BOUNDS}
          maxBoundsViscosity={0.8}
          zoomControl
          attributionControl
          preferCanvas
        >
          <TileLayer
            url={OSM_TILE_URL}
            attribution={OSM_ATTRIBUTION}
            minZoom={MIN_ZOOM}
            maxZoom={MAX_ZOOM}
            crossOrigin
            eventHandlers={{
              tileerror: manejarTileError,
              tileload: manejarTileLoad,
            }}
          />

          <MarkerClusterGroup
            ref={clusterRef}
            showCoverageOnHover={false}
          >
            {marcadoresNormalizados.map((marcador) => (
              <MarcadorIncidente
                key={marcador.id}
                marcador={marcador}
                registrar={registrarMarcador}
                onPopupOpen={manejarPopupOpen}
                onPopupClose={manejarPopupClose}
                onVerDetalle={manejarVerDetalle}
              />
            ))}
          </MarkerClusterGroup>

          <SincronizadorMapa
            marcadores={marcadoresNormalizados}
            selectedMarkerId={selectedMarkerId}
            retryKey={retryKey}
            openPopupIncidentIdRef={openPopupIncidentIdRef}
            isSyncingRef={isSyncingRef}
            markerLayersRef={markerLayersRef}
            clusterRef={clusterRef}
            onSelectMarkerRef={onSelectMarkerRef}
            ultimoIdSolicitadoRef={ultimoIdSolicitadoRef}
            ultimoIdMostradoRef={ultimoIdMostradoRef}
            operacionPendienteRef={operacionPendienteRef}
            tokenOperacionRef={tokenOperacionRef}
          />
        </MapContainer>
      </MapErrorBoundary>

      {tileError && (
        <div className="incident-map__error" role="alert">
          <p>{TILE_ERROR_MESSAGE}</p>
          <button type="button" className="incident-map__retry" onClick={handleRetry}>
            Reintentar
          </button>
        </div>
      )}
    </div>
  )
}
