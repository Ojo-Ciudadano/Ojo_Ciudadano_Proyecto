import axios from 'axios'
import type { ApiError } from '../types'

const rawApiUrl = (import.meta.env.VITE_API_URL || 'http://localhost:3000').trim()
const normalizedApiUrl = rawApiUrl.replace(/\/+$/, '')

export const apiClient = axios.create({
  baseURL: `${normalizedApiUrl}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError: ApiError = error.response?.data ?? {
      statusCode: 500,
      timestamp: new Date().toISOString(),
      path: error.config?.url ?? '',
      error: 'Internal Server Error',
      message: 'Ocurrió un error inesperado',
    }
    return Promise.reject(apiError)
  },
)
