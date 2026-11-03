import { apiClient } from './api'
import type {
  FiltrosIncidentes,
  Incidente,
} from '../types/incidente'

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