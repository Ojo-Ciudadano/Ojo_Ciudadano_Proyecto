import { apiClient, clearSessionStorage } from './api'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  User,
} from '../types'

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', credentials)
    return data
  },

  /**
   * Registra una nueva cuenta de ciudadano en POST /auth/register.
   * El backend asigna el rol CIUDADANO y devuelve los datos públicos del usuario.
   */
  async register(payload: RegisterRequest): Promise<RegisterResponse> {
    const { data } = await apiClient.post<RegisterResponse>(
      '/auth/register',
      payload,
    )
    return data
  },

  async logout(): Promise<void> {
    clearSessionStorage()
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