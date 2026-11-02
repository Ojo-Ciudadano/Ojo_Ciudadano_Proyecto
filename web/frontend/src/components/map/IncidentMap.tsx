import { useCallback, useEffect, useRef, useState } from 'react'
import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'
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
import './IncidentMap.css'

const EMPTY_MARKERS: readonly IncidentMapMarker[] = []

interface IncidentMapProps {
  /**
   * Capa de marcadores. La pantalla puede pasar datos en el futuro sin
   * reconstruir el mapa: el efecto solo sincroniza el `LayerGroup`.
   */
  markers?: readonly IncidentMapMarker[]
  className?: string
}

export function IncidentMap({ markers = EMPTY_MARKERS, className }: IncidentMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<L.Map | null>(null)
  const markersLayerRef = useRef<L.LayerGroup | null>(null)
  const [tileError, setTileError] = useState(false)
  const [retryKey, setRetryKey] = useState(0)

  const handleRetry = useCallback(() => {
    setTileError(false)
    setRetryKey((current) => current + 1)
  }, [])

  useEffect(() => {
    const container = containerRef.current
    if (!container || mapRef.current) {
      return
    }

    let map: L.Map
    try {
      map = L.map(container, {
        center: ARAUCANIA_CENTER,
        zoom: DEFAULT_ZOOM,
        minZoom: MIN_ZOOM,
        maxZoom: MAX_ZOOM,
        maxBounds: ARAUCANIA_BOUNDS,
        maxBoundsViscosity: 0.8,
        zoomControl: true,
        attributionControl: true,
        preferCanvas: true,
      })
    } catch {
      // Sin DOM/Leaflet disponible: se informa mediante el aviso de error.
      setTileError(true)
      return
    }

    mapRef.current = map
    let disposed = false

    const tiles = L.tileLayer(OSM_TILE_URL, {
      attribution: OSM_ATTRIBUTION,
      minZoom: MIN_ZOOM,
      maxZoom: MAX_ZOOM,
      crossOrigin: true,
    })

    // Un fallo aislado de un tile no debe bloquear el mapa: el aviso se
    // muestra solo cuando se acumulan errores consecutivos sin carga exitosa.
    let consecutiveTileErrors = 0
    tiles.on('tileerror', () => {
      if (disposed) {
        return
      }
      consecutiveTileErrors += 1
      if (consecutiveTileErrors >= 4) {
        setTileError(true)
      }
    })
    tiles.on('tileload', () => {
      if (disposed) {
        return
      }
      consecutiveTileErrors = 0
      setTileError(false)
    })
    tiles.addTo(map)

    // Capa dedicada a marcadores: se crea una sola vez y se reutiliza.
    const markersLayer = L.layerGroup().addTo(map)
    markersLayerRef.current = markersLayer

    const invalidateSize = () => {
      if (!disposed && mapRef.current === map) {
        map.invalidateSize()
      }
    }

    map.whenReady(() => {
      if (!disposed) {
        requestAnimationFrame(invalidateSize)
      }
    })

    let resizeObserver: ResizeObserver | null = null
    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(invalidateSize)
      resizeObserver.observe(container)
    } else {
      window.addEventListener('resize', invalidateSize)
    }

    return () => {
      disposed = true
      resizeObserver?.disconnect()
      window.removeEventListener('resize', invalidateSize)
      tiles.off()
      markersLayer.clearLayers()
      map.remove()
      markersLayerRef.current = null
      mapRef.current = null
    }
  }, [retryKey])

  // Sincroniza los marcadores sin recrear el mapa ni el contenedor.
  useEffect(() => {
    const layer = markersLayerRef.current
    if (!layer) {
      return
    }

    layer.clearLayers()

    for (const marcador of normalizarMarcadores(markers)) {
      const circle = L.circleMarker([marcador.latitud, marcador.longitud], {
        radius: 9,
        color: '#ffffff',
        weight: 2,
        fillColor: colorPorPrioridad(marcador.prioridad),
        fillOpacity: 0.9,
      })

      // Se construye el popup con nodos de texto para evitar inyección de HTML.
      const popup = document.createElement('div')
      popup.className = 'incident-map__popup'

      const titulo = document.createElement('strong')
      titulo.textContent = marcador.titulo
      popup.appendChild(titulo)

      if (marcador.descripcion) {
        const descripcion = document.createElement('p')
        descripcion.textContent = marcador.descripcion
        popup.appendChild(descripcion)
      }

      circle.bindPopup(popup)
      circle.addTo(layer)
    }
  }, [markers, retryKey])

  const rootClassName = ['incident-map', className].filter(Boolean).join(' ')

  return (
    <div className={rootClassName}>
      <div
        ref={containerRef}
        className="incident-map__canvas"
        role="application"
        aria-label="Mapa interactivo de incidentes de la Región de La Araucanía"
      />

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
