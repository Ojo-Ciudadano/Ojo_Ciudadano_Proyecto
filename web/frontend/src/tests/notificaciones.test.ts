
import assert from 'node:assert/strict'
import { describe, it, before, after } from 'node:test'
import type { AxiosAdapter } from 'axios'

import {
  mapearNotificacion,
  obtenerNotificaciones,
} from '../services/notificaciones.service'

import { apiClient } from '../services/api'

const ejemplo = {
  id: 'notificacion-1',
  usuarioId: 'usuario-1',
  incidenteId: 'incidente-1',
  mensaje: 'El incidente cambió de estado',
  leido: false,
  fecha: '2026-11-04T10:00:00.000Z',
}

// Simular localStorage para ejecutar las pruebas en Node.js.
const almacenamiento = new Map<string, string>()

const localStorageMock = {
  getItem: (key: string) => almacenamiento.get(key) ?? null,
  setItem: (key: string, value: string) => {
    almacenamiento.set(key, String(value))
  },
  removeItem: (key: string) => {
    almacenamiento.delete(key)
  },
  clear: () => {
    almacenamiento.clear()
  },
  key: (index: number) =>
    Array.from(almacenamiento.keys())[index] ?? null,
  get length() {
    return almacenamiento.size
  },
}

const originalLocalStorage = Object.getOwnPropertyDescriptor(
  globalThis,
  'localStorage',
)

const originalAdapter = apiClient.defaults.adapter

function simularRespuesta(data: unknown): AxiosAdapter {
  return async (config) => ({
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  })
}

describe('Notificaciones Web - Issue #144', () => {
  before(() => {
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: localStorageMock,
    })

    localStorageMock.setItem('access_token', 'token-prueba')
  })

  after(() => {
    apiClient.defaults.adapter = originalAdapter

    if (originalLocalStorage) {
      Object.defineProperty(
        globalThis,
        'localStorage',
        originalLocalStorage,
      )
    } else {
      Reflect.deleteProperty(globalThis, 'localStorage')
    }
  })

  it('conserva todos los campos de la notificación', () => {
    const resultado = mapearNotificacion(ejemplo)

    assert.deepEqual(resultado, ejemplo)
  })

  it('rechaza notificaciones inválidas', () => {
    assert.throws(
      () =>
        mapearNotificacion({
          ...ejemplo,
          fecha: 'fecha-invalida',
        }),
      /notificación inválida/,
    )
  })

  it('consulta GET /notificaciones sin usuarioId', async () => {
    apiClient.defaults.adapter = async (config) => {
      assert.equal(config.method, 'get')
      assert.equal(config.url, '/notificaciones')
      assert.equal(config.params, undefined)
      assert.equal(
        config.headers.Authorization,
        'Bearer token-prueba',
      )

      return {
        data: [ejemplo],
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }

    const resultado = await obtenerNotificaciones()

    assert.deepEqual(resultado, [ejemplo])
  })

  it('devuelve una colección vacía', async () => {
    apiClient.defaults.adapter = simularRespuesta([])

    const resultado = await obtenerNotificaciones()

    assert.deepEqual(resultado, [])
  })

  it('conserva el orden recibido desde el backend', async () => {
    const segunda = {
      ...ejemplo,
      id: 'notificacion-2',
      fecha: '2026-11-03T10:00:00.000Z',
    }

    apiClient.defaults.adapter = simularRespuesta([
      ejemplo,
      segunda,
    ])

    const resultado = await obtenerNotificaciones()

    assert.deepEqual(
      resultado.map((item) => item.id),
      ['notificacion-1', 'notificacion-2'],
    )
  })

  it('rechaza respuestas que no sean arreglos', async () => {
    apiClient.defaults.adapter = simularRespuesta({
      data: [ejemplo],
    })

    await assert.rejects(
      obtenerNotificaciones(),
      /lista de notificaciones inválida/,
    )
  })

  it('propaga errores del backend de forma controlada', async () => {
    apiClient.defaults.adapter = async () => {
      throw new Error('Error de conexión')
    }

    await assert.rejects(
      obtenerNotificaciones(),
      (error: unknown) => {
        assert.ok(error !== null && typeof error === 'object')
        assert.ok('statusCode' in error)
        assert.equal(error.statusCode, 500)
        return true
      },
    )
  })
})
