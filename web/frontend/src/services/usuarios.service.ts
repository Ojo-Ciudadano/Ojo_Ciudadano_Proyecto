
import { apiClient } from './api'
import type {
  UsuariosListadoResponse,
  RolUsuario,
  ActualizarRolUsuarioRequest,
  ActualizarRolUsuarioResponse,
} from '../types/usuario'

// Issue #147: listado paginado de usuarios
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

// Issue #135: candidatos para asignar responsables
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

// Issue #148: actualizar el rol de un usuario
export async function actualizarRolUsuario(
  usuarioId: string,
  nuevoRol: RolUsuario,
): Promise<ActualizarRolUsuarioResponse> {
  const datos: ActualizarRolUsuarioRequest = {
    rol: nuevoRol,
  }

  const response = await apiClient.patch<ActualizarRolUsuarioResponse>(
    `/usuarios/${encodeURIComponent(usuarioId)}/rol`,
    datos,
  )

  const resultado = response.data

  // Validar que el backend confirmó el cambio solicitado
  if (
    !resultado ||
    resultado.id !== usuarioId ||
    !Array.isArray(resultado.roles) ||
    resultado.roles.length !== 1 ||
    resultado.roles[0] !== nuevoRol
  ) {
    throw new Error(
      'El servidor no confirmó correctamente el cambio de rol.',
    )
  }

  return resultado
}
