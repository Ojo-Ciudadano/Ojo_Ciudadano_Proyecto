export interface User {
  id: string
  email: string
  nombre: string
  apellido: string
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

export interface ApiError {
  statusCode: number
  timestamp: string
  path: string
  error: string
  message: string | string[]
}