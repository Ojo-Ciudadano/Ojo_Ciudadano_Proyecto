
export interface UsuarioListado {
  id: string
  nombre: string
  email: string
  telefono: string | null
  fechaRegistro: string
  roles: string[]
}

export interface UsuariosPaginationMeta {
  page: number
  limit: number
  total: number
  totalPages: number
}

export interface UsuariosListadoResponse {
  data: UsuarioListado[]
  meta: UsuariosPaginationMeta
}

// Issue #148: roles permitidos por el backend
export type RolUsuario = 'ADMIN' | 'CIUDADANO'

// Datos enviados al endpoint PATCH
export interface ActualizarRolUsuarioRequest {
  rol: RolUsuario
}

// Respuesta del backend después del cambio
export interface ActualizarRolUsuarioResponse {
  id: string
  roles: RolUsuario[]
}
