
import assert from 'node:assert/strict'
import { after, before, beforeEach, describe, it } from 'node:test'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import type { AxiosAdapter, AxiosResponse } from 'axios'

import { AuthProvider, useAuth } from '../contexts/AuthContext'
import { useNotificaciones } from '../hooks/useNotificaciones'
import { apiClient, startNewSession } from '../services/api'
import type { User } from '../types'

const usuarioA = {
  id: 'usuario-a',
  email: 'a@ejemplo.cl',
  roles: ['CIUDADANO'],
} as User

const usuarioB = {
  id: 'usuario-b',
  email: 'b@ejemplo.cl',
  roles: ['CIUDADANO'],
} as User

const notificacionA = {
  id: 'notificacion-a',
  usuarioId: 'usuario-a',
  incidenteId: 'incidente-a',
  mensaje: 'Notificación del usuario A',
  leido: false,
  fecha: '2026-11-04T10:00:00.000Z',
}

const notificacionB = {
  ...notificacionA,
  id: 'notificacion-b',
  usuarioId: 'usuario-b',
  mensaje: 'Notificación del usuario B',
}

const almacenamiento = new Map<string, string>()

const localStorageMock = {
  getItem: (key: string) => almacenamiento.get(key) ?? null,

  setItem: (key: string, value: string) => {
    almacenamiento.set(key, String(value))
  },

  removeItem: (key: string) => {
    almacenamiento.delete(key)
  },

  clear: () => almacenamiento.clear(),

  key: (index: number) =>
    Array.from(almacenamiento.keys())[index] ?? null,

  get length() {
    return almacenamiento.size
  },
}

const originalStorage = Object.getOwnPropertyDescriptor(
  globalThis,
  'localStorage',
)

const originalAdapter = apiClient.defaults.adapter

function respuesta(
  config: Parameters<AxiosAdapter>[0],
  data: unknown,
): AxiosResponse {
  return {
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  }
}

let auth: ReturnType<typeof useAuth>
let notificaciones: ReturnType<typeof useNotificaciones>

function Observador() {
  auth = useAuth()
  notificaciones = useNotificaciones()
  return null
}

async function montar() {
  let renderer!: TestRenderer.ReactTestRenderer

  await act(async () => {
    renderer = TestRenderer.create(
      createElement(
        AuthProvider,
        null,
        createElement(Observador),
      ),
    )
  })

  return renderer
}

async function desmontar(renderer: TestRenderer.ReactTestRenderer) {
  await act(async () => {
    renderer.unmount()
  })
}

describe('Hook de notificaciones - Issue #144', () => {
  before(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: localStorageMock,
    })
  })

  beforeEach(() => {
    almacenamiento.clear()
    apiClient.defaults.adapter = originalAdapter
  })

  after(() => {
    apiClient.defaults.adapter = originalAdapter

    if (originalStorage) {
      Object.defineProperty(
        globalThis,
        'localStorage',
        originalStorage,
      )
    } else {
      Reflect.deleteProperty(globalThis, 'localStorage')
    }
  })

  it('no consulta notificaciones sin sesión', async () => {
    let consultas = 0

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/notificaciones') {
        consultas += 1
      }

      return respuesta(config, [])
    }

    const renderer = await montar()

    assert.deepEqual(notificaciones.notificaciones, [])
    assert.equal(notificaciones.cargando, false)
    assert.equal(consultas, 0)

    await desmontar(renderer)
  })

  it('carga las notificaciones del usuario autenticado', async () => {
    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuarioA)
      }

      if (config.url === '/notificaciones') {
        return respuesta(config, [notificacionA])
      }

      return respuesta(config, [])
    }

    startNewSession('token-a')

    const renderer = await montar()

    assert.equal(auth.user?.id, 'usuario-a')

    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionA],
    )

    assert.equal(notificaciones.error, null)
    assert.equal(notificaciones.cargando, false)

    await desmontar(renderer)
  })

  it('limpia los datos cuando se cierra la sesión', async () => {
    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuarioA)
      }

      if (config.url === '/notificaciones') {
        return respuesta(config, [notificacionA])
      }

      return respuesta(config, [])
    }

    startNewSession('token-a')

    const renderer = await montar()

    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionA],
    )

    await act(async () => {
      await auth.logout()
    })

    assert.deepEqual(notificaciones.notificaciones, [])
    assert.equal(notificaciones.cargando, false)
    assert.equal(notificaciones.error, null)

    await desmontar(renderer)
  })

  it('no muestra notificaciones anteriores al cambiar de cuenta', async () => {
    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        const token = config.headers.Authorization

        return respuesta(
          config,
          token === 'Bearer token-b' ? usuarioB : usuarioA,
        )
      }

      if (config.url === '/notificaciones') {
        const token = config.headers.Authorization

        return respuesta(
          config,
          token === 'Bearer token-b'
            ? [notificacionB]
            : [notificacionA],
        )
      }

      return respuesta(config, [])
    }

    startNewSession('token-a')

    const renderer = await montar()

    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionA],
    )

    await act(async () => {
      await auth.logout()
    })

    assert.deepEqual(notificaciones.notificaciones, [])

    await act(async () => {
      startNewSession('token-b')
      await auth.refreshProfile()
    })

    assert.equal(auth.user?.id, 'usuario-b')

    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionB],
    )

    await desmontar(renderer)
  })

  it('permite reintentar después de un error', async () => {
    let consultas = 0

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuarioA)
      }

      if (config.url === '/notificaciones') {
        consultas += 1

        if (consultas === 1) {
          throw new Error('Error de conexión')
        }

        return respuesta(config, [notificacionA])
      }

      return respuesta(config, [])
    }

    startNewSession('token-a')

    const renderer = await montar()

    assert.deepEqual(notificaciones.notificaciones, [])
    assert.ok(notificaciones.error)

    await act(async () => {
      notificaciones.reintentar()
    })

    assert.equal(consultas, 2)
    assert.equal(notificaciones.error, null)

    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionA],
    )

    await desmontar(renderer)
  })

  it('ignora respuestas tardías de una sesión anterior', async () => {
    let resolverConsultaA:
      | ((response: AxiosResponse) => void)
      | undefined

    let consultaAIniciada:
      | (() => void)
      | undefined

    const consultaALista = new Promise<void>((resolve) => {
      consultaAIniciada = resolve
    })

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(
          config,
          config.headers.Authorization === 'Bearer token-b'
            ? usuarioB
            : usuarioA,
        )
      }

      if (config.url === '/notificaciones') {
        if (config.headers.Authorization === 'Bearer token-a') {
          return new Promise<AxiosResponse>((resolve) => {
            resolverConsultaA = resolve
            consultaAIniciada?.()
          })
        }

        return respuesta(config, [notificacionB])
      }

      return respuesta(config, [])
    }

    startNewSession('token-a')

    const renderer = await montar()

    await consultaALista

    assert.equal(auth.user?.id, 'usuario-a')
    assert.equal(notificaciones.cargando, true)

    await act(async () => {
      await auth.logout()
    })

    assert.deepEqual(notificaciones.notificaciones, [])

    await act(async () => {
      startNewSession('token-b')
      await auth.refreshProfile()
    })

    assert.equal(auth.user?.id, 'usuario-b')

    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionB],
    )

    // La respuesta antigua llega después de cargar la cuenta B.
    await act(async () => {
      resolverConsultaA?.(
        respuesta(
          {
            url: '/notificaciones',
            headers: {},
          } as Parameters<AxiosAdapter>[0],
          [notificacionA],
        ),
      )
    })

    // Debe conservar únicamente las notificaciones de B.
    assert.deepEqual(
      notificaciones.notificaciones,
      [notificacionB],
    )

    assert.equal(notificaciones.error, null)

    await desmontar(renderer)
  })
})
