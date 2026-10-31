import { describe, it, beforeEach } from 'node:test'
import assert from 'node:assert/strict'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { apiClient,  onPermissionDenied,  onSessionInvalidated,  resetApiAuthStateForTests,  startNewSession,  type PermissionDeniedEvent,  type SessionInvalidatedEvent,} from '../services/api'
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