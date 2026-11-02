import { apiClient } from './api'
import type { Incidente } from '../types/incidente'

export async function obtenerIncidentes(
  signal?: AbortSignal,
): Promise<Incidente[]> {
  const response = await apiClient.get<Incidente[]>('/incidentes', {
    signal,
  })

  if (!Array.isArray(response.data)) {
    throw new Error('El servidor devolvió una lista de incidentes inválida.')
  }

  return response.data
}