import { apiClient } from './api'
import type {
  FiltrosIncidentes,
  Incidente,
} from '../types/incidente'
import type { IncidenteDetalle } from '../types/incidenteDetalle'
import type { EstadoIncidente } from '../config/transicionesIncidente'
import type { AsignacionActiva } from '../types/incidenteDetalle'

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

  const response = await apiClient.get<Incidente[]>('/incidentes', {
    signal,
    params,
  })

  if (!Array.isArray(response.data)) {
    throw new Error(
      'El servidor devolvió una lista de incidentes inválida.',
    )
  }

  return response.data
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