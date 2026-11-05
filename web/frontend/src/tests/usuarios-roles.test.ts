
import { describe, it, beforeEach, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { apiClient } from '../services/api'
import { actualizarRolUsuario } from '../services/usuarios.service'
import type { ApiError } from '../types'

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

const originalAdapter = apiClient.defaults.adapter

function respuesta(
  config: InternalAxiosRequestConfig,
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

describe('Selector de roles de usuarios Web (#148)', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
  })

  it('envía PATCH con el identificador y el rol ADMIN', async () => {
    let metodo = ''
    let ruta = ''
    let cuerpo: unknown = null
    let solicitudes = 0

    apiClient.defaults.adapter = async (config) => {
      solicitudes++
      metodo = config.method ?? ''
      ruta = config.url ?? ''
      cuerpo = JSON.parse(String(config.data))

      return respuesta(config, {
        id: 'usuario-2',
        roles: ['ADMIN'],
      })
    }

    const resultado = await actualizarRolUsuario(
      'usuario-2',
      'ADMIN',
    )

    assert.equal(solicitudes, 1)
    assert.equal(metodo, 'patch')
    assert.equal(ruta, '/usuarios/usuario-2/rol')
    assert.deepEqual(cuerpo, { rol: 'ADMIN' })
    assert.deepEqual(resultado, {
      id: 'usuario-2',
      roles: ['ADMIN'],
    })
  })

  it('permite cambiar el rol a CIUDADANO', async () => {
    apiClient.defaults.adapter = async (config) => {
      assert.deepEqual(
        JSON.parse(String(config.data)),
        { rol: 'CIUDADANO' },
      )

      return respuesta(config, {
        id: 'usuario-3',
        roles: ['CIUDADANO'],
      })
    }

    const resultado = await actualizarRolUsuario(
      'usuario-3',
      'CIUDADANO',
    )

    assert.deepEqual(resultado.roles, ['CIUDADANO'])
  })

  it('rechaza una respuesta con identificador incorrecto', async () => {
    apiClient.defaults.adapter = async (config) =>
      respuesta(config, {
        id: 'otro-usuario',
        roles: ['ADMIN'],
      })

    await assert.rejects(
      () => actualizarRolUsuario('usuario-2', 'ADMIN'),
      /servidor no confirmó correctamente/i,
    )
  })

  it('rechaza una respuesta que no confirma el rol solicitado', async () => {
    apiClient.defaults.adapter = async (config) =>
      respuesta(config, {
        id: 'usuario-2',
        roles: ['CIUDADANO'],
      })

    await assert.rejects(
      () => actualizarRolUsuario('usuario-2', 'ADMIN'),
      /servidor no confirmó correctamente/i,
    )
  })

  it('rechaza una respuesta con múltiples roles', async () => {
    apiClient.defaults.adapter = async (config) =>
      respuesta(config, {
        id: 'usuario-2',
        roles: ['ADMIN', 'CIUDADANO'],
      })

    await assert.rejects(
      () => actualizarRolUsuario('usuario-2', 'ADMIN'),
      /servidor no confirmó correctamente/i,
    )
  })

  it('propaga un error 403 de permisos', async () => {
    apiClient.defaults.adapter = async (config) => {
      const error = Object.assign(
        new Error('Request failed with status code 403'),
        {
          isAxiosError: true,
          config,
          response: {
            data: {
              statusCode: 403,
              error: 'Forbidden',
              message: 'No tienes permisos suficientes.',
            },
            status: 403,
            statusText: 'Forbidden',
            headers: {},
            config,
          },
        },
      )

      throw error
    }

    await assert.rejects(
      () => actualizarRolUsuario('usuario-2', 'ADMIN'),
      (error: unknown) => {
        const apiError = error as ApiError

        assert.equal(apiError.statusCode, 403)
        assert.match(String(apiError.message), /permisos/i)
        return true
      },
    )
  })

  it('no confirma un cambio cuando falla la conexión', async () => {
    apiClient.defaults.adapter = async () => {
      throw new Error('Error de conexión')
    }

    await assert.rejects(
      () => actualizarRolUsuario('usuario-2', 'ADMIN'),
    )
  })
})
