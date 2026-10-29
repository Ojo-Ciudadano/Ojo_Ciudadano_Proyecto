import { apiClient } from './api'
import type { LoginRequest, LoginResponse } from '../types'

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', credentials)
    return data
  },

  async logout(): Promise<void> {
    localStorage.removeItem('access_token')
    localStorage.removeItem('user')
  },

  async getProfile() {
    const { data } = await apiClient.get('/auth/profile')
    return data
  },
}