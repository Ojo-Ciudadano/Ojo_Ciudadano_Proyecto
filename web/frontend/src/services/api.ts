import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { ApiError, User } from '../types'

const API_URL = import.meta.env?.VITE_API_URL || 'http://localhost:3000'

export const DEFAULT_SESSION_EXPIRED_MESSAGE =
  'Tu sesión ha expirado o ya no es válida. Por favor, inicia sesión nuevamente.'
export const DEFAULT_PERMISSION_DENIED_MESSAGE =
  'No tienes permisos suficientes para realizar esta operación.'

export interface SessionInvalidatedEvent {
  statusCode: 401
  message: string
  path: string
}

export interface PermissionDeniedEvent {
  statusCode: 403
  message: string
  path: string
  error: ApiError
}

type SessionInvalidatedListener = (event: SessionInvalidatedEvent) => void
type PermissionDeniedListener = (event: PermissionDeniedEvent) => void

interface TaggedRequestConfig extends InternalAxiosRequestConfig {
  _requestToken?: string | null
  _sessionEpoch?: number
  _isAuthLoginRequest?: boolean
}

const sessionInvalidatedListeners = new Set<SessionInvalidatedListener>()
const permissionDeniedListeners = new Set<PermissionDeniedListener>()

let currentSessionEpoch = 0
let lastInvalidatedToken: string | null = null
let lastInvalidatedEpoch: number | null = null

export function onSessionInvalidated(listener: SessionInvalidatedListener): () => void {
  sessionInvalidatedListeners.add(listener)
  return () => {
    sessionInvalidatedListeners.delete(listener)
  }
}

export function onPermissionDenied(listener: PermissionDeniedListener): () => void {
  permissionDeniedListeners.add(listener)
  return () => {
    permissionDeniedListeners.delete(listener)
  }
}

export function startNewSession(token: string, user?: User): void {
  currentSessionEpoch += 1
  lastInvalidatedToken = null
  lastInvalidatedEpoch = null
  localStorage.setItem('access_token', token)
  if (user !== undefined) {
    localStorage.setItem('user', JSON.stringify(user))
  }
}

export function clearSessionStorage(): void {
  currentSessionEpoch += 1
  localStorage.removeItem('access_token')
  localStorage.removeItem('user')
}

export function resetApiAuthStateForTests(): void {
  currentSessionEpoch = 0
  lastInvalidatedToken = null
  lastInvalidatedEpoch = null
  sessionInvalidatedListeners.clear()
  permissionDeniedListeners.clear()
}

function isLoginEndpoint(url?: string): boolean {
  if (!url) return false
  return /\/?auth\/login(\?|$|\/)/i.test(url)
}

function extractBearerToken(authHeader?: unknown): string | null {
  if (typeof authHeader !== 'string') return null
  const match = authHeader.match(/^Bearer\s+(.+)$/i)
  return match ? match[1].trim() : null
}

function normalizeMessage(rawMessage: unknown, fallback: string): string {
  if (Array.isArray(rawMessage)) {
    const joined = rawMessage.filter(Boolean).join(', ').trim()
    return joined || fallback
  }
  if (typeof rawMessage === 'string' && rawMessage.trim().length > 0) {
    return rawMessage.trim()
  }
  return fallback
}

function resolvePermissionDeniedMessage(rawMessage: unknown): string {
  const genericMessages = new Set(['forbidden', 'forbidden resource', 'error 403'])
  if (typeof rawMessage === 'string') {
    const trimmed = rawMessage.trim()
    if (trimmed && !genericMessages.has(trimmed.toLowerCase())) {
      return trimmed
    }
  }
  if (Array.isArray(rawMessage) && rawMessage.length > 0) {
    const joined = rawMessage.filter(Boolean).join(', ').trim()
    if (joined && !genericMessages.has(joined.toLowerCase())) {
      return joined
    }
  }
  return DEFAULT_PERMISSION_DENIED_MESSAGE
}

export const apiClient = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config: TaggedRequestConfig) => {
  const loginRequest = isLoginEndpoint(config.url)
  config._isAuthLoginRequest = loginRequest

  const explicitToken = extractBearerToken(config.headers?.Authorization)
  const storedToken = localStorage.getItem('access_token')
  const tokenToUse = explicitToken ?? (!loginRequest ? storedToken : null)

  if (tokenToUse && !explicitToken) {
    config.headers.Authorization = `Bearer ${tokenToUse}`
  }

  config._requestToken = tokenToUse ?? null
  if (config._sessionEpoch === undefined) {
    config._sessionEpoch = currentSessionEpoch
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const config = (error.config ?? {}) as TaggedRequestConfig
    const responseData = error.response?.data
    const statusCode: number =
      error.response?.status ?? responseData?.statusCode ?? 500
    const path: string = responseData?.path ?? config.url ?? ''

    if (statusCode === 401) {
      const isLogin = config._isAuthLoginRequest || isLoginEndpoint(config.url)

      if (isLogin) {
        const loginError: ApiError = {
          statusCode: 401,
          timestamp: responseData?.timestamp ?? new Date().toISOString(),
          path,
          error: responseData?.error ?? 'Unauthorized',
          message: responseData?.message ?? 'Credenciales inválidas',
        }
        return Promise.reject(loginError)
      }

      const requestToken = config._requestToken
      const requestEpoch = config._sessionEpoch
      const activeToken = localStorage.getItem('access_token')

      const alreadyInvalidated =
        (requestToken !== null &&
          requestToken !== undefined &&
          lastInvalidatedToken === requestToken &&
          lastInvalidatedEpoch === requestEpoch) ||
        (requestEpoch !== undefined && lastInvalidatedEpoch === requestEpoch)

      const belongsToCurrentSession =
        Boolean(requestToken) &&
        Boolean(activeToken) &&
        requestToken === activeToken &&
        (requestEpoch === undefined || requestEpoch === currentSessionEpoch)

      if (!alreadyInvalidated && belongsToCurrentSession && requestToken) {
        lastInvalidatedToken = requestToken
        lastInvalidatedEpoch = requestEpoch ?? currentSessionEpoch
        clearSessionStorage()

        const event: SessionInvalidatedEvent = {
          statusCode: 401,
          message: DEFAULT_SESSION_EXPIRED_MESSAGE,
          path,
        }
        sessionInvalidatedListeners.forEach((listener) => listener(event))
      }

      const unauthorizedError: ApiError = {
        statusCode: 401,
        timestamp: responseData?.timestamp ?? new Date().toISOString(),
        path,
        error: responseData?.error ?? 'Unauthorized',
        message: normalizeMessage(
          responseData?.message,
          DEFAULT_SESSION_EXPIRED_MESSAGE,
        ),
      }
      return Promise.reject(unauthorizedError)
    }

    if (statusCode === 403) {
      const permissionMessage = resolvePermissionDeniedMessage(
        responseData?.message,
      )
      const forbiddenError: ApiError = {
        statusCode: 403,
        timestamp: responseData?.timestamp ?? new Date().toISOString(),
        path,
        error: responseData?.error ?? 'Forbidden',
        message: permissionMessage,
      }

      const event: PermissionDeniedEvent = {
        statusCode: 403,
        message: permissionMessage,
        path,
        error: forbiddenError,
      }
      permissionDeniedListeners.forEach((listener) => listener(event))

      return Promise.reject(forbiddenError)
    }

    const apiError: ApiError = responseData ?? {
      statusCode,
      timestamp: new Date().toISOString(),
      path,
      error: 'Internal Server Error',
      message: 'Ocurrió un error inesperado',
    }
    return Promise.reject(apiError)
  },
)