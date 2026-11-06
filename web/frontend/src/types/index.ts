export type RoleName = 'ADMIN' | 'CIUDADANO'

export interface User {
  id: string
  email: string
  nombre: string
  apellido?: string
  telefono?: string | null
  fechaRegistro?: string | Date
  roles: string[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  access_token: string
  user: User
}

export interface RegisterRequest {
  nombre: string
  email: string
  password: string
  telefono?: string
}

export interface RegisterResponse {
  id: string
  nombre: string
  email: string
  telefono?: string | null
  fechaRegistro?: string
}

export interface ApiError {
  statusCode: number
  timestamp: string
  path: string
  error: string
  message: string | string[]
}
