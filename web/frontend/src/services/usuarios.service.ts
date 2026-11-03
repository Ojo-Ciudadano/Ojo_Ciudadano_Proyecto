import { apiClient } from './api'

export interface CandidatoResponsable {
  id: string
  nombre: string
  email: string
  roles: string[]
}

export interface PaginaCandidatos {
  data: CandidatoResponsable[]
  meta: {
    page: number
    limit: number
    total: number
    totalPages: number
  }
}

export async function obtenerCandidatosResponsables(
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaCandidatos> {
  const response = await apiClient.get<PaginaCandidatos>(
    '/usuarios',
    {
      signal,
      params: {
        rol: 'ADMIN',
        page: pagina,
        limit: 10,
      },
    },
  )

  const resultado = response.data

  if (
    !resultado ||
    !Array.isArray(resultado.data) ||
    !resultado.meta ||
    !Number.isInteger(resultado.meta.page) ||
    resultado.meta.page !== pagina ||
    !Number.isInteger(resultado.meta.limit) ||
    resultado.meta.limit <= 0 ||
    !Number.isInteger(resultado.meta.total) ||
    resultado.meta.total < 0 ||
    !Number.isInteger(resultado.meta.totalPages) ||
    resultado.meta.totalPages < 0
  ) {
    throw new Error(
      'El servidor devolvió una lista de responsables inválida.',
    )
  }

  const candidatosValidos = resultado.data.every(
    (usuario) =>
      usuario !== null &&
      typeof usuario === 'object' &&
      typeof usuario.id === 'string' &&
      usuario.id.trim().length > 0 &&
      typeof usuario.nombre === 'string' &&
      typeof usuario.email === 'string' &&
      Array.isArray(usuario.roles) &&
      usuario.roles.includes('ADMIN'),
  )

  if (!candidatosValidos) {
    throw new Error(
      'El servidor devolvió candidatos sin los datos o el rol requeridos.',
    )
  }

  return resultado
}