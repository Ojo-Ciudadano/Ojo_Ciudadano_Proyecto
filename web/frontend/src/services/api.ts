
import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { ApiError, User } from '../types'

const API_URL = import.meta.env?.VITE_API_URL || 'http://localhost:3000'

export const LOGIN_REDIRECT_PATH = '/login'

export const DEFAULT_SESSION_EXPIRED_MESSAGE =
  'Tu sesión ha expirado o ya no es válida. Por favor, inicia sesión nuevamente.'

export const DEFAULT_PERMISSION_DENIED_MESSAGE =
  'No tienes permisos suficientes para realizar esta operación.'

export interface SessionInvalidatedEvent {
  statusCode: 401
  message: string
  path: string
  redirectTo: string
  sessionEpoch: number
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

// Issue #144: observadores de cambios de sesión
const sessionChangeListeners = new Set<() => void>()

let currentSessionEpoch = 0
let lastInvalidatedToken: string | null = null
let lastInvalidatedEpoch: number | null = null

function isPlainRecord(value: unknown): value is Record<string, unknown> {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  if (typeof Blob !== 'undefined' && value instanceof Blob) {
    return false
  }

  return !Array.isArray(value)
}

function invokeListenersSafely<T>(
  listeners: Iterable<(payload: T) => void>,
  payload: T,
): void {
  for (const listener of Array.from(listeners)) {
    try {
      listener(payload)
    } catch {
      // Aislar fallos de un suscriptor para garantizar limpieza, aviso y redirección.
    }
  }
}

function invokeVoidListenersSafely(listeners: Iterable<() => void>): void {
  for (const listener of Array.from(listeners)) {
    try {
      listener()
    } catch {
      // Aislar fallos de un suscriptor al cambiar de sesión.
    }
  }
}

// Issue #144: identificador de la sesión actual
export function getSessionEpoch(): number {
  return currentSessionEpoch
}

// Issue #144: suscripción a cambios de sesión
export function subscribeSessionChanges(
  listener: () => void,
): () => void {
  sessionChangeListeners.add(listener)

  return () => {
    sessionChangeListeners.delete(listener)
  }
}

// Issue #144: avisar a los hooks cuando cambia la sesión
function notifySessionChange(): void {
  invokeVoidListenersSafely(sessionChangeListeners)
}

export function onSessionInvalidated(
  listener: SessionInvalidatedListener,
): () => void {
  sessionInvalidatedListeners.add(listener)

  return () => {
    sessionInvalidatedListeners.delete(listener)
  }
}

export function onPermissionDenied(
  listener: PermissionDeniedListener,
): () => void {
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

  // Issue #144: notificar nueva sesión
  notifySessionChange()
}

export function clearSessionStorage(): void {
  currentSessionEpoch += 1

  localStorage.removeItem('access_token')
  localStorage.removeItem('user')

  // Issue #144: notificar cierre o invalidación
  notifySessionChange()
}

export function resetApiAuthStateForTests(): void {
  currentSessionEpoch = 0
  lastInvalidatedToken = null
  lastInvalidatedEpoch = null

  sessionInvalidatedListeners.clear()
  permissionDeniedListeners.clear()
  sessionChangeListeners.clear()
}

function isLoginEndpoint(url?: string): boolean {
  if (!url) return false
  return /\/?auth\/login(\?|$|\/)/i.test(url)
}

function readAuthorizationHeader(
  headers: TaggedRequestConfig['headers'],
): unknown {
  if (!headers) return undefined

  const headersWithGet = headers as { get?: (name: string) => unknown }
  if (typeof headersWithGet.get === 'function') {
    const value = headersWithGet.get('Authorization')
    if (value !== undefined && value !== null) {
      return value
    }
  }

  const headerRecord = headers as Record<string, unknown>
  return headerRecord.Authorization ?? headerRecord.authorization
}

function setAuthorizationHeader(
  config: TaggedRequestConfig,
  token: string,
): void {
  const bearerValue = `Bearer ${token}`
  const headersWithSet = config.headers as
    | { set?: (name: string, value: string) => void }
    | undefined

  if (typeof headersWithSet?.set === 'function') {
    headersWithSet.set('Authorization', bearerValue)
    return
  }

  config.headers = {
    ...(config.headers as Record<string, unknown> | undefined),
    Authorization: bearerValue,
  } as TaggedRequestConfig['headers']
}

function extractBearerToken(authHeader?: unknown): string | null {
  if (typeof authHeader !== 'string') return null

  const match = authHeader.match(/^Bearer\s+(.+)$/i)

  return match ? match[1].trim() : null
}

function isRequestFromCurrentSession(
  requestToken: string | null | undefined,
  requestEpoch: number | undefined,
): boolean {
  const activeToken = localStorage.getItem('access_token')

  return (
    Boolean(requestToken) &&
    Boolean(activeToken) &&
    requestToken === activeToken &&
    (requestEpoch === undefined || requestEpoch === currentSessionEpoch)
  )
}

function invalidateActiveSessionOnce(
  requestToken: string | null | undefined,
  requestEpoch: number | undefined,
  path: string,
): boolean {
  const effectiveEpoch = requestEpoch ?? currentSessionEpoch

  const alreadyInvalidated =
    (requestToken !== null &&
      requestToken !== undefined &&
      lastInvalidatedToken === requestToken &&
      lastInvalidatedEpoch === effectiveEpoch) ||
    lastInvalidatedEpoch === effectiveEpoch

  if (
    alreadyInvalidated ||
    !requestToken ||
    !isRequestFromCurrentSession(requestToken, requestEpoch)
  ) {
    return false
  }

  lastInvalidatedToken = requestToken
  lastInvalidatedEpoch = effectiveEpoch

  clearSessionStorage()

  const event: SessionInvalidatedEvent = {
    statusCode: 401,
    message: DEFAULT_SESSION_EXPIRED_MESSAGE,
    path,
    redirectTo: LOGIN_REDIRECT_PATH,
    sessionEpoch: effectiveEpoch,
  }

  invokeListenersSafely(sessionInvalidatedListeners, event)
  return true
}

function normalizeMessage(
  rawMessage: unknown,
  fallback: string,
): string {
  if (Array.isArray(rawMessage)) {
    const joined = rawMessage.filter(Boolean).join(', ').trim()
    return joined || fallback
  }

  if (
    typeof rawMessage === 'string' &&
    rawMessage.trim().length > 0
  ) {
    return rawMessage.trim()
  }

  return fallback
}

function resolvePermissionDeniedMessage(rawMessage: unknown): string {
  const genericMessages = new Set([
    'forbidden',
    'forbidden resource',
    'error 403',
  ])

  if (typeof rawMessage === 'string') {
    const trimmed = rawMessage.trim()

    if (
      trimmed &&
      !genericMessages.has(trimmed.toLowerCase())
    ) {
      return trimmed
    }
  }

  if (Array.isArray(rawMessage) && rawMessage.length > 0) {
    const joined = rawMessage.filter(Boolean).join(', ').trim()

    if (
      joined &&
      !genericMessages.has(joined.toLowerCase())
    ) {
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

apiClient.interceptors.request.use(
  (config: TaggedRequestConfig) => {
    const loginRequest = isLoginEndpoint(config.url)
    config._isAuthLoginRequest = loginRequest

    const explicitToken = extractBearerToken(
      readAuthorizationHeader(config.headers),
    )

    const storedToken = localStorage.getItem('access_token')

    const tokenToUse =
      explicitToken ?? (!loginRequest ? storedToken : null)

    if (tokenToUse && !explicitToken) {
      setAuthorizationHeader(config, tokenToUse)
    }

    config._requestToken = tokenToUse ?? null

    if (config._sessionEpoch === undefined) {
      config._sessionEpoch = currentSessionEpoch
    }

    return config
  },
)

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const config = (error.config ?? {}) as TaggedRequestConfig
    const rawResponseData: unknown = error.response?.data
    const responseData = isPlainRecord(rawResponseData)
      ? rawResponseData
      : undefined

    const statusCode: number =
      typeof error.response?.status === 'number'
        ? error.response.status
        : typeof responseData?.statusCode === 'number'
          ? responseData.statusCode
          : 500

    const path: string =
      typeof responseData?.path === 'string'
        ? responseData.path
        : config.url ?? ''

    const timestamp: string =
      typeof responseData?.timestamp === 'string'
        ? responseData.timestamp
        : new Date().toISOString()

    if (statusCode === 401) {
      const isLogin =
        config._isAuthLoginRequest ||
        isLoginEndpoint(config.url)

      if (isLogin) {
        const loginError: ApiError = {
          statusCode: 401,
          timestamp,
          path,
          error:
            typeof responseData?.error === 'string'
              ? responseData.error
              : 'Unauthorized',
          message: normalizeMessage(
            responseData?.message,
            'Credenciales inválidas',
          ),
        }

        return Promise.reject(loginError)
      }

      invalidateActiveSessionOnce(
        config._requestToken,
        config._sessionEpoch,
        path,
      )

      const unauthorizedError: ApiError = {
        statusCode: 401,
        timestamp,
        path,
        error:
          typeof responseData?.error === 'string'
            ? responseData.error
            : 'Unauthorized',
        message: normalizeMessage(
          responseData?.message,
          DEFAULT_SESSION_EXPIRED_MESSAGE,
        ),
      }

      return Promise.reject(unauthorizedError)
    }

    if (statusCode === 403) {
      const permissionMessage =
        resolvePermissionDeniedMessage(
          responseData?.message,
        )

      const forbiddenError: ApiError = {
        statusCode: 403,
        timestamp,
        path,
        error:
          typeof responseData?.error === 'string'
            ? responseData.error
            : 'Forbidden',
        message: permissionMessage,
      }

      const belongsToCurrentEpoch =
        config._sessionEpoch === undefined ||
        config._sessionEpoch === currentSessionEpoch

      if (belongsToCurrentEpoch) {
        const event: PermissionDeniedEvent = {
          statusCode: 403,
          message: permissionMessage,
          path,
          error: forbiddenError,
        }

        invokeListenersSafely(permissionDeniedListeners, event)
      }

      return Promise.reject(forbiddenError)
    }

    const apiError: ApiError = responseData
      ? {
          statusCode,
          timestamp,
          path,
          error:
            typeof responseData.error === 'string'
              ? responseData.error
              : 'Internal Server Error',
          message: normalizeMessage(
            responseData.message,
            'Ocurrió un error inesperado',
          ),
        }
      : {
          statusCode,
          timestamp,
          path,
          error: 'Internal Server Error',
          message: 'Ocurrió un error inesperado',
        }

    return Promise.reject(apiError)
  },
)
