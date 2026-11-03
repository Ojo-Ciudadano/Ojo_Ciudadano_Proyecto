
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