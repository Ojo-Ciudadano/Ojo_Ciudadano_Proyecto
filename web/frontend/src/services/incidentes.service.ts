import { apiClient } from './api'
import type {
  FiltrosIncidentes,
  Incidente,
} from '../types/incidente'
import type { IncidenteDetalle } from '../types/incidenteDetalle'

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