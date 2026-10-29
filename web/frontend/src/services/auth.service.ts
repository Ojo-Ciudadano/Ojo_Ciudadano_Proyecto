import { apiClient } from './api'
import type { LoginRequest, LoginResponse, User } from '../types'

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', credentials)
    return data
  },

  async logout(): Promise<void> {
    localStorage.removeItem('access_token')
    localStorage.removeItem('user')
  },

  /**
   * Consulta el perfil y rol del usuario autenticado en /api/v1/usuarios/me (#118).
   * Devuelve id, nombre, email, telefono, fechaRegistro y roles vigentes.
   */
  async getProfile(): Promise<User> {
    const { data } = await apiClient.get<User>('/usuarios/me')
    return data
  },
}
