import {
  esCoordenadaValida,
  normalizarMarcadores,
  type IncidentMapMarker,
} from './mapConfig'
import { textoOpcional } from './textoOpcional'
import type { Incidente } from '../../types/incidente'

export interface ErrorMapa {
  mensaje: string
  permiteReintentar: boolean
}

function numeroOpcional(valor: unknown): number | undefined {
  if (typeof valor === 'number') {
    return Number.isFinite(valor) ? valor : undefined
  }

  if (typeof valor === 'string' && valor.trim() !== '') {
    const numero = Number(valor)
    return Number.isFinite(numero) ? numero : undefined
  }

  return undefined
}

/**
 * Convierte un incidente de la API en un marcador del mapa.
 *
 * Devuelve `null` cuando el incidente no tiene `id` o cuando sus coordenadas
 * son ausentes, no numéricas o están fuera de rango. Un incidente inválido no
 * interrumpe el resto del listado.
 */
export function mapearIncidenteAMarcador(
  incidente: Incidente | null | undefined,
): IncidentMapMarker | null {
  if (!incidente || typeof incidente.id !== 'string' || incidente.id.trim() === '') {
    return null
  }

  const ubicacion = incidente.ubicacion
  if (!ubicacion) {
    return null
  }

  const latitud = numeroOpcional(ubicacion.latitud)
  const longitud = numeroOpcional(ubicacion.longitud)

  if (!esCoordenadaValida(latitud, longitud)) {
    return null
  }

  const titulo =
    textoOpcional(incidente.titulo) ??
    textoOpcional(ubicacion.direccionReferencia) ??
    `Incidente ${incidente.id}`

  const descripcion =
    textoOpcional(incidente.descripcion) ??
    textoOpcional(ubicacion.direccionReferencia) ??
    null

  const categoria = textoOpcional(incidente.categoria?.nombre) ?? null
  const estado = textoOpcional(incidente.estado?.nombre) ?? null
  const fechaCreacion = textoOpcional(incidente.fechaCreacion) ?? null

  return {
    id: incidente.id,
    latitud: latitud as number,
    longitud: longitud as number,
    titulo,
    descripcion,
    categoria,
    estado,
    fechaCreacion,
    prioridad:
      typeof incidente.nivelPrioridad === 'number'
        ? incidente.nivelPrioridad
        : null,
  }
}

/**
 * Normaliza un listado completo de incidentes en marcadores listos para
 * `IncidentMap`. Descarta inválidos y duplicados por `id`, conservando el
 * orden entregado por la API.
 */
export function mapearIncidentesAMarcadores(
  incidentes?: readonly Incidente[] | null,
): IncidentMapMarker[] {
  if (!Array.isArray(incidentes)) {
    return []
  }

  const marcadores: IncidentMapMarker[] = []

  for (const incidente of incidentes) {
    const marcador = mapearIncidenteAMarcador(incidente)
    if (marcador) {
      marcadores.push(marcador)
    }
  }

  return normalizarMarcadores(marcadores)
}

function extraerStatus(error: unknown): number | undefined {
  if (typeof error !== 'object' || error === null) {
    return undefined
  }

  const candidato = error as {
    statusCode?: unknown
    status?: unknown
    response?: { status?: unknown } | null
  }

  for (const valor of [
    candidato.statusCode,
    candidato.response?.status,
    candidato.status,
  ]) {
    const numero = Number(valor)
    if (Number.isFinite(numero)) {
      return numero
    }
  }

  return undefined
}

/** Traduce errores de la consulta de incidentes a un mensaje para la pantalla. */
export function interpretarErrorMapa(error: unknown): ErrorMapa {
  const statusCode = extraerStatus(error)

  if (statusCode === 401) {
    return {
      mensaje:
        'Tu sesión no es válida. Inicia sesión nuevamente para ver el mapa.',
      permiteReintentar: false,
    }
  }

  if (statusCode === 403) {
    return {
      mensaje: 'No tienes permisos para consultar los incidentes del mapa.',
      permiteReintentar: false,
    }
  }

  return {
    mensaje:
      'No se pudieron cargar los incidentes georreferenciados. Inténtalo nuevamente.',
    permiteReintentar: true,
  }
}
