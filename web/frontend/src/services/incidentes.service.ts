import { apiClient } from './api'
import type {
  FiltrosIncidentes,
  Incidente,
} from '../types/incidente'
import type {
  AsignacionActiva,
  IncidenteDetalle,
  RegistroHistorialIncidente,
} from '../types/incidenteDetalle'
import type { EstadoIncidente } from '../config/transicionesIncidente'

/**
 * Normaliza el payload de `GET /incidentes`.
 *
 * El contrato actual del backend entrega un arreglo plano de incidentes, por lo
 * que no hay envelopes ni metadatos de paginación que interpretar. Devuelve
 * `null` cuando el payload no es un listado válido.
 */
export function normalizarIncidentes(payload: unknown): Incidente[] | null {
  if (!Array.isArray(payload)) {
    return null
  }

  return payload as Incidente[]
}

export async function obtenerIncidentes(
  signal?: AbortSignal,
  filtros: Partial<FiltrosIncidentes> = {},
): Promise<Incidente[]> {
  const params: Record<string, string> = {}

  if (filtros.estadoId) {
    params.estadoId = filtros.estadoId
  }

  if (filtros.categoriaId) {
    params.categoriaId = filtros.categoriaId
  }

  const comuna = filtros.comuna?.trim()

  if (comuna) {
    params.comuna = comuna
  }

  const response = await apiClient.get<unknown>('/incidentes', {
    signal,
    params,
  })

  const incidentes = normalizarIncidentes(response.data)

  if (!incidentes) {
    throw new Error(
      'El servidor devolvió una lista de incidentes inválida.',
    )
  }

  return incidentes
}

/**
 * Obtiene los incidentes que alimentan los marcadores del mapa.
 *
 * Reutiliza `obtenerIncidentes` para compartir la normalización y respetar el
 * cliente autenticado. La deduplicación por `id` ocurre al construir los
 * marcadores, donde también se descartan las coordenadas inválidas.
 */
export function obtenerIncidentesParaMapa(
  signal?: AbortSignal,
): Promise<Incidente[]> {
  return obtenerIncidentes(signal)
}

export async function obtenerIncidenteDetalle(
  id: string,
  signal?: AbortSignal,
): Promise<IncidenteDetalle> {
  const response = await apiClient.get<IncidenteDetalle>(
    `/incidentes/${encodeURIComponent(id)}`,
    { signal },
  )

  const incidente = response.data

  if (
    !incidente ||
    typeof incidente !== 'object' ||
    incidente.id !== id ||
    typeof incidente.estadoId !== 'string' ||
    !incidente.estadoId
  ) {
    throw new Error(
      'El servidor devolvió un detalle de incidente inválido.',
    )
  }

  return incidente
}

export interface CambioEstadoIncidente {
  estado: EstadoIncidente
  observacion: string
}

export async function cambiarEstadoIncidente(
  id: string,
  cambio: CambioEstadoIncidente,
): Promise<Incidente> {
  const observacion = cambio.observacion.trim()

  if (!observacion) {
    throw new Error('Debes ingresar una observación.')
  }

  const response = await apiClient.patch<Incidente>(
    `/incidentes/${encodeURIComponent(id)}/estado`,
    {
      estado: cambio.estado,
      observacion,
    },
  )

  const incidente = response.data

  if (
    !incidente ||
    typeof incidente !== 'object' ||
    incidente.id !== id ||
    typeof incidente.estadoId !== 'string' ||
    !incidente.estadoId
  ) {
    throw new Error(
      'No se pudo verificar el estado actualizado. Consulta nuevamente el incidente.',
    )
  }

  return incidente
}

export const PRIORIDADES_INCIDENTE = [1, 2, 3, 4, 5] as const

export type PrioridadIncidente =
  (typeof PRIORIDADES_INCIDENTE)[number]

export function esPrioridadIncidente(
  valor: unknown,
): valor is PrioridadIncidente {
  return (
    typeof valor === 'number' &&
    (PRIORIDADES_INCIDENTE as readonly number[]).includes(valor)
  )
}

export async function asignarResponsableIncidente(
  incidenteId: string,
  usuarioAsignadoId: string,
): Promise<AsignacionActiva> {
  if (!usuarioAsignadoId.trim()) {
    throw new Error('Debes seleccionar un responsable.')
  }

  const response = await apiClient.patch<AsignacionActiva>(
    `/incidentes/${encodeURIComponent(incidenteId)}/asignacion`,
    {
      usuarioAsignadoId,
    },
  )

  const asignacion = response.data

  if (
    !asignacion ||
    typeof asignacion !== 'object' ||
    typeof asignacion.id !== 'string' ||
    !asignacion.id.trim() ||
    asignacion.incidenteId !== incidenteId ||
    asignacion.usuarioAsignadoId !== usuarioAsignadoId ||
    typeof asignacion.asignadoPorId !== 'string' ||
    !asignacion.asignadoPorId.trim() ||
    typeof asignacion.fechaAsignacion !== 'string' ||
    Number.isNaN(Date.parse(asignacion.fechaAsignacion)) ||
    asignacion.activa !== true
  ) {
    throw new Error(
      'No se pudo verificar la asignación. Consulta nuevamente el incidente antes de reintentar.',
    )
  }

  return asignacion
}

export async function cambiarPrioridadIncidente(
  incidenteId: string,
  nivelPrioridad: PrioridadIncidente,
): Promise<Incidente> {
  if (!esPrioridadIncidente(nivelPrioridad)) {
    throw new Error('La prioridad debe ser un entero entre 1 y 5.')
  }

  const response = await apiClient.patch<Incidente>(
    `/incidentes/${encodeURIComponent(incidenteId)}/prioridad`,
    {
      nivelPrioridad,
    },
  )

  const incidente = response.data

  if (
    !incidente ||
    typeof incidente !== 'object' ||
    incidente.id !== incidenteId ||
    !esPrioridadIncidente(incidente.nivelPrioridad) ||
    incidente.nivelPrioridad !== nivelPrioridad
  ) {
    throw new Error(
      'No se pudo verificar la prioridad. Consulta nuevamente el incidente antes de reintentar.',
    )
  }

  return incidente
}

export function normalizarHistorialIncidente(
  payload: unknown,
  incidenteId?: string,
): RegistroHistorialIncidente[] | null {
  if (!Array.isArray(payload)) {
    return null
  }

  for (const item of payload) {
    if (!item || typeof item !== 'object') {
      return null
    }

    const registro = item as Partial<RegistroHistorialIncidente>

    if (
      typeof registro.id !== 'string' ||
      !registro.id.trim() ||
      typeof registro.incidenteId !== 'string' ||
      !registro.incidenteId.trim() ||
      (incidenteId && registro.incidenteId !== incidenteId)
    ) {
      return null
    }
  }

  return payload as RegistroHistorialIncidente[]
}

export async function obtenerHistorialIncidente(
  id: string,
  signal?: AbortSignal,
): Promise<RegistroHistorialIncidente[]> {
  const response = await apiClient.get<unknown>(
    `/incidentes/${encodeURIComponent(id)}/historial`,
    { signal },
  )

  const historial = normalizarHistorialIncidente(response.data, id)

  if (!historial) {
    throw new Error(
      'El servidor devolvió un historial de incidente inválido.',
    )
  }

  return historial
}