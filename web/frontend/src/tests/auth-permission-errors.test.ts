import { describe, it, beforeEach } from 'node:test'
import assert from 'node:assert/strict'
import React, { useEffect, useRef } from 'react'
import { act, create, type ReactTestRenderer } from 'react-test-renderer'
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import {
  DEFAULT_PERMISSION_DENIED_MESSAGE,
  LOGIN_REDIRECT_PATH,
  apiClient,
  clearSessionStorage,
  onPermissionDenied,
  onSessionInvalidated,
  resetApiAuthStateForTests,
  startNewSession,
  subscribeSessionChanges,
  type PermissionDeniedEvent,
  type SessionInvalidatedEvent,
} from '../services/api'
import { AuthProvider, useAuth } from '../contexts/AuthContext'
import { ProtectedRoute } from '../components/auth/ProtectedRoute'
import type { ApiError, User } from '../types'

class MemoryStorage implements Storage {
  private store = new Map<string, string>()

  get length(): number {
    return this.store.size
  }

  clear(): void {
    this.store.clear()
  }

  getItem(key: string): string | null {
    return this.store.get(key) ?? null
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null
  }

  removeItem(key: string): void {
    this.store.delete(key)
  }

  setItem(key: string, value: string): void {
    this.store.set(key, String(value))
  }
}

Object.defineProperty(globalThis, 'localStorage', {
  value: new MemoryStorage(),
  configurable: true,
  writable: true,
})

interface MockAxiosError extends Error {
  isAxiosError: boolean
  config: InternalAxiosRequestConfig
  response: AxiosResponse
}

function createHttpError(
  status: number,
  config: InternalAxiosRequestConfig,
  data: Record<string, unknown>,
): MockAxiosError {
  const response: AxiosResponse = {
    status,
    statusText: status === 401 ? 'Unauthorized' : 'Forbidden',
    headers: {},
    config,
    data: {
      statusCode: status,
      timestamp: new Date().toISOString(),
      path: config.url ?? '',
      error: status === 401 ? 'Unauthorized' : 'Forbidden',
      ...data,
    },
  }

  return Object.assign(new Error(`Request failed with status code ${status}`), {
    isAxiosError: true,
    config,
    response,
  })
}

const sampleUserA: User = {
  id: 'user-1',
  email: 'ciudadano@ojociudadano.cl',
  nombre: 'Bernardo',
  apellido: 'Sandoval',
  roles: ['CIUDADANO'],
}

const sampleUserB: User = {
  id: 'user-2',
  email: 'admin@ojociudadano.cl',
  nombre: 'Admin',
  apellido: 'UCT',
  roles: ['ADMIN'],
}

describe('Manejo de errores 401 y 403 en el cliente HTTP web (#150)', () => {
  beforeEach(() => {
    localStorage.clear()
    resetApiAuthStateForTests()
  })

  it('invalida la sesión local, limpia storage y emite aviso ante un 401 de una ruta protegida', async () => {
    startNewSession('token-valido-1', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    apiClient.defaults.adapter = async (config) => {
      throw createHttpError(401, config, {
        message: 'Token invalido o expirado',
      })
    }

    await assert.rejects(
      () => apiClient.get('/incidentes'),
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 401)
        return true
      },
    )

    assert.equal(localStorage.getItem('access_token'), null)
    assert.equal(localStorage.getItem('user'), null)
    assert.equal(invalidatedEvents.length, 1)
    assert.match(invalidatedEvents[0].message, /inicia sesión nuevamente/i)
  })

  it('evita cierres de sesión, redirecciones o mensajes repetidos cuando varias solicitudes reciben 401 simultáneamente', async () => {
    startNewSession('token-simultaneo', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    apiClient.defaults.adapter = async (config) => {
      await new Promise((resolve) => setTimeout(resolve, 10))
      throw createHttpError(401, config, {
        message: 'Token invalido o expirado',
      })
    }

    const results = await Promise.allSettled([
      apiClient.get('/incidentes'),
      apiClient.get('/categorias'),
      apiClient.get('/notificaciones'),
    ])

    assert.equal(results.every((r) => r.status === 'rejected'), true)
    assert.equal(invalidatedEvents.length, 1)
    assert.equal(localStorage.getItem('access_token'), null)
    assert.equal(localStorage.getItem('user'), null)
  })

  it('diferencia un 401 de /auth/login manteniendo el mensaje de credenciales sin disparar cierre de sesión', async () => {
    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    apiClient.defaults.adapter = async (config) => {
      throw createHttpError(401, config, {
        message: 'Credenciales inválidas',
      })
    }

    await assert.rejects(
      () =>
        apiClient.post('/auth/login', {
          email: 'usuario@ojociudadano.cl',
          password: 'password-incorrecta',
        }),
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 401)
        assert.equal(apiError.message, 'Credenciales inválidas')
        return true
      },
    )

    assert.equal(invalidatedEvents.length, 0)
  })

  it('conserva la sesión activa ante un 403, emite mensaje de permisos y permite conservar el último dato confirmado sin reintentar escrituras', async () => {
    startNewSession('token-ciudadano', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    const permissionEvents: PermissionDeniedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))
    onPermissionDenied((event) => permissionEvents.push(event))

    let writeAttempts = 0
    apiClient.defaults.adapter = async (config) => {
      writeAttempts += 1
      throw createHttpError(403, config, {
        message: 'No tienes permisos suficientes para acceder a este recurso.',
      })
    }

    let confirmedIncidentState = 'NUEVO'

    await assert.rejects(
      async () => {
        await apiClient.patch('/incidentes/INC-4021/estado', {
          estado: 'CERRADO',
        })
        confirmedIncidentState = 'CERRADO'
      },
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 403)
        assert.match(String(apiError.message), /permisos/i)
        return true
      },
    )

    assert.equal(writeAttempts, 1)
    assert.equal(confirmedIncidentState, 'NUEVO')
    assert.equal(localStorage.getItem('access_token'), 'token-ciudadano')
    assert.deepEqual(JSON.parse(localStorage.getItem('user')!), sampleUserA)
    assert.equal(invalidatedEvents.length, 0)
    assert.equal(permissionEvents.length, 1)
  })

  it('evita que un 401 tardío de una sesión anterior cierre una sesión nueva', async () => {
    startNewSession('token-sesion-anterior', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    let rejectSlowRequest!: (reason?: unknown) => void
    let capturedSlowConfig!: InternalAxiosRequestConfig
    let notifyAdapterCalled!: () => void

    const adapterCalled = new Promise<void>((resolve) => {
      notifyAdapterCalled = resolve
    })

    apiClient.defaults.adapter = (config) => {
      capturedSlowConfig = config
      notifyAdapterCalled()
      return new Promise((_, reject) => {
        rejectSlowRequest = reject
      })
    }

    // 1. Se dispara la petición bajo la sesión anterior y esperamos a que salga con el token antiguo
    const slowRequestPromise = apiClient.get('/incidentes')
    await adapterCalled

    // 2. El usuario cambia a una nueva sesión antes de que responda la solicitud anterior
    startNewSession('token-sesion-nueva', sampleUserB)

    // 3. La solicitud de la sesión anterior falla tardíamente con 401
    rejectSlowRequest(
      createHttpError(401, capturedSlowConfig, {
        message: 'Token expirado de la sesión anterior',
      }),
    )

    await assert.rejects(
      () => slowRequestPromise,
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 401)
        return true
      },
    )

    // 4. La nueva sesión permanece intacta y no se emitió cierre de sesión
    assert.equal(localStorage.getItem('access_token'), 'token-sesion-nueva')
    assert.deepEqual(JSON.parse(localStorage.getItem('user')!), sampleUserB)
    assert.equal(invalidatedEvents.length, 0)
  })
})

describe('Consolidación de sesión vencida, permisos y redirección única (#332)', () => {
  beforeEach(() => {
    localStorage.clear()
    resetApiAuthStateForTests()
  })

  it('incluye redirectTo /login, ejecuta limpieza una sola vez y aísla errores entre listeners', async () => {
    startNewSession('token-valido-332', sampleUserA)

    let sessionChangeCount = 0
    subscribeSessionChanges(() => {
      sessionChangeCount += 1
    })

    const receivedBySecondListener: SessionInvalidatedEvent[] = []
    onSessionInvalidated(() => {
      throw new Error('fallo simulado en primer listener')
    })
    onSessionInvalidated((event) => {
      receivedBySecondListener.push(event)
    })

    apiClient.defaults.adapter = async (config) => {
      throw createHttpError(401, config, {
        message: 'Token expirado',
      })
    }

    await assert.rejects(() => apiClient.get('/incidentes'))

    assert.equal(sessionChangeCount, 1)
    assert.equal(receivedBySecondListener.length, 1)
    assert.equal(receivedBySecondListener[0].redirectTo, LOGIN_REDIRECT_PATH)
    assert.equal(localStorage.getItem('access_token'), null)
    assert.equal(localStorage.getItem('user'), null)
  })

  it('permite invalidar una segunda sesión aunque vuelva a emitir el mismo token después de reiniciar sesión', async () => {
    startNewSession('token-reutilizado', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    apiClient.defaults.adapter = async (config) => {
      throw createHttpError(401, config, {
        message: 'Token expirado',
      })
    }

    await assert.rejects(() => apiClient.get('/incidentes'))
    assert.equal(invalidatedEvents.length, 1)

    startNewSession('token-reutilizado', sampleUserB)
    await assert.rejects(() => apiClient.get('/usuarios'))
    assert.equal(invalidatedEvents.length, 2)
    assert.notEqual(
      invalidatedEvents[0].sessionEpoch,
      invalidatedEvents[1].sessionEpoch,
    )
  })

  it('normaliza mensajes de error de /auth/login y no altera ni invalida sesiones ante 401 de credenciales', async () => {
    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    apiClient.defaults.adapter = async (config) => {
      throw createHttpError(401, config, {
        message: ['Credenciales incorrectas', 'Verifica tu contraseña'],
      })
    }

    await assert.rejects(
      () =>
        apiClient.post('/auth/login?from=web', {
          email: 'ciudadano@ojociudadano.cl',
          password: 'mala',
        }),
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 401)
        assert.equal(
          apiError.message,
          'Credenciales incorrectas, Verifica tu contraseña',
        )
        return true
      },
    )

    assert.equal(invalidatedEvents.length, 0)
  })

  it('maneja 401 en respuestas tipo Blob y cabeceras authorization en minúsculas sin filtrar el Blob', async () => {
    startNewSession('token-blob', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    apiClient.defaults.adapter = async (config) => {
      const response: AxiosResponse = {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config,
        data: new Blob(['unauthorized'], { type: 'text/plain' }),
      }

      throw Object.assign(new Error('Request failed with status code 401'), {
        isAxiosError: true,
        config,
        response,
      })
    }

    await assert.rejects(
      () =>
        apiClient.get('/incidentes/inc-1/evidencias/ev-1', {
          responseType: 'blob',
          headers: { authorization: 'Bearer token-blob' },
        }),
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 401)
        assert.match(String(apiError.message), /inicia sesión nuevamente/i)
        return true
      },
    )

    assert.equal(invalidatedEvents.length, 1)
    assert.equal(localStorage.getItem('access_token'), null)
  })

  it('ignora un 403 tardío de una sesión anterior y mantiene operativa la sesión vigente tras un 403 actual', async () => {
    startNewSession('token-anterior-403', sampleUserA)

    const permissionEvents: PermissionDeniedEvent[] = []
    onPermissionDenied((event) => permissionEvents.push(event))

    let rejectOld403!: (reason?: unknown) => void
    let oldConfig!: InternalAxiosRequestConfig
    let notifyOldSent!: () => void
    const oldSent = new Promise<void>((resolve) => {
      notifyOldSent = resolve
    })

    apiClient.defaults.adapter = (config) => {
      if (config.url === '/admin/recurso-antiguo') {
        oldConfig = config
        notifyOldSent()
        return new Promise((_, reject) => {
          rejectOld403 = reject
        })
      }

      if (config.url === '/admin/recurso-restringido') {
        return Promise.reject(
          createHttpError(403, config, { message: 'Forbidden resource' }),
        )
      }

      return Promise.resolve({
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
        data: { ok: true },
      })
    }

    const stale403Promise = apiClient.get('/admin/recurso-antiguo')
    await oldSent

    startNewSession('token-vigente-403', sampleUserB)

    rejectOld403(
      createHttpError(403, oldConfig, {
        message: 'Sin permisos en sesión anterior',
      }),
    )
    await assert.rejects(() => stale403Promise)
    assert.equal(permissionEvents.length, 0)

    await assert.rejects(
      () => apiClient.get('/admin/recurso-restringido'),
      (err: unknown) => {
        const apiError = err as ApiError
        assert.equal(apiError.statusCode, 403)
        assert.equal(apiError.message, DEFAULT_PERMISSION_DENIED_MESSAGE)
        return true
      },
    )

    assert.equal(permissionEvents.length, 1)
    assert.equal(localStorage.getItem('access_token'), 'token-vigente-403')

    const authorizedResponse = await apiClient.get('/incidentes')
    assert.equal(authorizedResponse.status, 200)
  })

  it('limpia estado de AuthProvider y redirige una sola vez a /login ante múltiples 401 simultáneos', async () => {
    startNewSession('token-router-401', sampleUserA)

    const visitedPaths: string[] = []
    const loginStates: unknown[] = []
    let latestAuthState: {
      isAuthenticated: boolean
      sessionExpiredMessage: string | null
      userEmail: string | null
    } = {
      isAuthenticated: false,
      sessionExpiredMessage: null,
      userEmail: null,
    }

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return {
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
          data: sampleUserA,
        }
      }

      await new Promise((resolve) => setTimeout(resolve, 10))
      throw createHttpError(401, config, {
        message: 'Token expirado',
      })
    }

    function RouteTracker() {
      const location = useLocation()
      const navigate = useNavigate()
      const auth = useAuth()
      const locationRef = useRef(location)
      const lastRedirectEpochRef = useRef<number | null>(null)

      useEffect(() => {
        locationRef.current = location
        visitedPaths.push(location.pathname)
        if (location.pathname === LOGIN_REDIRECT_PATH) {
          loginStates.push(location.state)
        }
      }, [location])

      useEffect(() => {
        latestAuthState = {
          isAuthenticated: auth.isAuthenticated,
          sessionExpiredMessage: auth.sessionExpiredMessage,
          userEmail: auth.user?.email ?? null,
        }
      }, [auth.isAuthenticated, auth.sessionExpiredMessage, auth.user])

      useEffect(() => {
        return onSessionInvalidated((event) => {
          if (lastRedirectEpochRef.current === event.sessionEpoch) {
            return
          }
          lastRedirectEpochRef.current = event.sessionEpoch
          if (locationRef.current.pathname !== event.redirectTo) {
            navigate(event.redirectTo, {
              replace: true,
              state: { from: locationRef.current },
            })
          }
        })
      }, [navigate])

      return null
    }

    let renderer!: ReactTestRenderer
    await act(async () => {
      renderer = create(
        React.createElement(
          MemoryRouter,
          { initialEntries: ['/incidentes'] },
          React.createElement(
            AuthProvider,
            null,
            React.createElement(RouteTracker),
            React.createElement(
              Routes,
              null,
              React.createElement(Route, {
                path: '/login',
                element: React.createElement('div', null, 'LOGIN'),
              }),
              React.createElement(Route, {
                path: '/incidentes',
                element: React.createElement(
                  ProtectedRoute,
                  null,
                  React.createElement('div', null, 'INCIDENTES'),
                ),
              }),
            ),
          ),
        ),
      )
      await new Promise((resolve) => setTimeout(resolve, 20))
    })

    assert.equal(latestAuthState.isAuthenticated, true)
    assert.equal(latestAuthState.userEmail, sampleUserA.email)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    await act(async () => {
      await Promise.allSettled([
        apiClient.get('/incidentes'),
        apiClient.get('/notificaciones'),
        apiClient.get('/categorias'),
      ])
      await new Promise((resolve) => setTimeout(resolve, 20))
    })

    assert.equal(invalidatedEvents.length, 1)
    assert.equal(latestAuthState.isAuthenticated, false)
    assert.equal(latestAuthState.userEmail, null)
    assert.match(
      String(latestAuthState.sessionExpiredMessage),
      /inicia sesión nuevamente/i,
    )
    assert.equal(
      visitedPaths.filter((p) => p === LOGIN_REDIRECT_PATH).length,
      1,
    )
    assert.equal(
      (loginStates[0] as { from?: { pathname?: string } })?.from?.pathname,
      '/incidentes',
    )

    act(() => {
      renderer.unmount()
    })
  })

  it('no invalida ni cierra una sesión nueva si una solicitud manual de cierre o cambio ocurre antes del 401 tardío', async () => {
    startNewSession('token-cierre-manual', sampleUserA)

    const invalidatedEvents: SessionInvalidatedEvent[] = []
    onSessionInvalidated((event) => invalidatedEvents.push(event))

    let rejectInFlight!: (reason?: unknown) => void
    let capturedConfig!: InternalAxiosRequestConfig
    let notifySent!: () => void
    const sent = new Promise<void>((resolve) => {
      notifySent = resolve
    })

    apiClient.defaults.adapter = (config) => {
      capturedConfig = config
      notifySent()
      return new Promise((_, reject) => {
        rejectInFlight = reject
      })
    }

    const pendingRequest = apiClient.get('/notificaciones')
    await sent

    // Cierre manual antes de que llegue el 401 tardío
    clearSessionStorage()

    rejectInFlight(
      createHttpError(401, capturedConfig, { message: 'Token expirado' }),
    )
    await assert.rejects(() => pendingRequest)

    assert.equal(invalidatedEvents.length, 0)
  })
})