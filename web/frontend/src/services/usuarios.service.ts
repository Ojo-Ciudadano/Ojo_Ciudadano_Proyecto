import { apiClient } from './api'
import type { UsuariosListadoResponse } from '../types/usuario'

export async function obtenerUsuarios(
  page: number = 1,
  limit: number = 10,
  signal?: AbortSignal,
): Promise<UsuariosListadoResponse> {
  const response = await apiClient.get<UsuariosListadoResponse>(
    '/usuarios',
    {
      params: {
        page,
        limit,
      },
      signal,
    },
  )

  const resultado = response.data

  if (
    !resultado ||
    !Array.isArray(resultado.data) ||
    !resultado.meta ||
    !Number.isInteger(resultado.meta.page) ||
    !Number.isInteger(resultado.meta.limit) ||
    !Number.isInteger(resultado.meta.total) ||
    !Number.isInteger(resultado.meta.totalPages)
  ) {
    throw new Error(
      'El servidor devolvió una respuesta de usuarios inválida.',
    )
  }

  return resultado
}