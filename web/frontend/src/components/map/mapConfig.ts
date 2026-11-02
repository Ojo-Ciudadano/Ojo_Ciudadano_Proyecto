import type { LatLngBoundsExpression, LatLngTuple } from 'leaflet'

/**
 * Configuración y utilidades puras del mapa de incidentes (#139).
 *
 * Este módulo no depende del DOM ni de Leaflet en tiempo de ejecución, por lo
 * que puede probarse directamente con la suite `node:test` del frontend.
 */

/** Centro inicial: plaza de Temuco, Región de La Araucanía. */
export const ARAUCANIA_CENTER: LatLngTuple = [-38.7359, -72.5904]

/** Zoom inicial: cubre el área metropolitana de Temuco y comunas cercanas. */
export const DEFAULT_ZOOM = 10

export const MIN_ZOOM = 8
export const MAX_ZOOM = 18

/** Límites aproximados de la Región de La Araucanía para acotar el paneo. */
export const ARAUCANIA_BOUNDS: LatLngBoundsExpression = [
  [-39.7, -73.9],
  [-37.4, -71.0],
]

/** Capa base estándar de OpenStreetMap con su atribución obligatoria. */
export const OSM_TILE_URL = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
export const OSM_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'

export const TILE_ERROR_MESSAGE =
  'No se pudo cargar la cartografía base. Revisa tu conexión e inténtalo nuevamente.'

/** Color de respaldo cuando el incidente no informa prioridad. */
export const DEFAULT_MARKER_COLOR = '#64748b'

/** Paleta por nivel de prioridad (1 = más urgente). */
export const PRIORITY_COLORS: Readonly<Record<number, string>> = {
  1: '#dc2626',
  2: '#f97316',
  3: '#1d4ed8',
}

/**
 * Marcador que la pantalla podrá recibir en el futuro. Se define aquí para que
 * la capa de marcadores y el resto de la UI compartan el mismo contrato.
 */
export interface IncidentMapMarker {
  id: string
  latitud: number
  longitud: number
  titulo: string
  descripcion?: string | null
  prioridad?: number | null
}

/** Verifica que las coordenadas sean números finitos dentro del rango válido. */
export function esCoordenadaValida(latitud: unknown, longitud: unknown): boolean {
  if (typeof latitud !== 'number' || typeof longitud !== 'number') {
    return false
  }
  if (!Number.isFinite(latitud) || !Number.isFinite(longitud)) {
    return false
  }
  return latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180
}

/**
 * Descarta marcadores inválidos o duplicados por `id` antes de dibujarlos.
 * Mantiene el orden de entrada para que la capa sea determinista.
 */
export function normalizarMarcadores(
  marcadores?: readonly IncidentMapMarker[] | null,
): IncidentMapMarker[] {
  if (!Array.isArray(marcadores)) {
    return []
  }

  const ids = new Set<string>()
  const resultado: IncidentMapMarker[] = []

  for (const marcador of marcadores) {
    if (!marcador || typeof marcador.id !== 'string' || marcador.id.trim() === '') {
      continue
    }
    if (!esCoordenadaValida(marcador.latitud, marcador.longitud)) {
      continue
    }
    if (ids.has(marcador.id)) {
      continue
    }
    ids.add(marcador.id)
    resultado.push(marcador)
  }

  return resultado
}

/** Devuelve el color del marcador según su nivel de prioridad. */
export function colorPorPrioridad(prioridad?: number | null): string {
  if (typeof prioridad !== 'number' || !Number.isFinite(prioridad)) {
    return DEFAULT_MARKER_COLOR
  }
  return PRIORITY_COLORS[Math.trunc(prioridad)] ?? DEFAULT_MARKER_COLOR
}
