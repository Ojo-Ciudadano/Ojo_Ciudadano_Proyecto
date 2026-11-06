
import { describe, it, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import React from 'react'
import {
  act,
  create,
  type ReactTestRenderer,
} from 'react-test-renderer'
import { AxiosError, AxiosHeaders } from 'axios'
import {
  apiClient,
  resetApiAuthStateForTests,
} from '../services/api'
import { useUsuarios } from '../hooks/useUsuarios'

type EstadoUsuarios = ReturnType<typeof useUsuarios>

const originalAdapter = apiClient.defaults.adapter

const almacenamiento = new Map<string, string>()

Object.defineProperty(globalThis, 'localStorage', {
  configurable: true,
  value: {
    getItem: (key: string) =>
      almacenamiento.get(key) ?? null,

    setItem: (key: string, value: string) => {
      almacenamiento.set(key, String(value))
    },

    removeItem: (key: string) => {
      almacenamiento.delete(key)
    },

    clear: () => {
      almacenamiento.clear()
    },
  },
})

describe('Hook de cambio de rol (#148)', () => {
  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
    resetApiAuthStateForTests()
  })

  it('muestra error de permisos y conserva el rol ante un 403', async () => {
    let estado!: EstadoUsuarios
    let renderer!: ReactTestRenderer
    let intentosPatch = 0

    function ComponentePrueba() {
      estado = useUsuarios()
      return null
    }

    // Simular respuestas del backend
    apiClient.defaults.adapter = async (config) => {
      if (config.method === 'get') {
        return {
          data: {
            data: [
              {
                id: 'usuario-2',
                nombre: 'Ciudadano de prueba',
                email: 'ciudadano@ejemplo.com',
                roles: ['CIUDADANO'],
              },
            ],
            meta: {
              page: 1,
              limit: 10,
              total: 1,
              totalPages: 1,
            },
          },
          status: 200,
          statusText: 'OK',
          headers: new AxiosHeaders(),
          config,
        }
      }

      if (config.method === 'patch') {
        intentosPatch++

        const response = {
          data: {
            statusCode: 403,
            message: 'No tienes permisos suficientes.',
            error: 'Forbidden',
          },
          status: 403,
          statusText: 'Forbidden',
          headers: new AxiosHeaders(),
          config,
        }

        // Error de Axios que procesará el interceptor
        throw new AxiosError(
          'Request failed with status code 403',
          'ERR_BAD_REQUEST',
          config,
          undefined,
          response,
        )
      }

      throw new Error('Petición inesperada')
    }

    // Montar el componente y esperar la carga inicial
    await act(async () => {
      renderer = create(
        React.createElement(ComponentePrueba),
      )
    })

    // Esperar a que se complete el GET de usuarios
    await act(async () => {
      await Promise.resolve()
      await Promise.resolve()
    })

    assert.equal(
      estado.cargando,
      false,
      `No terminó la carga: ${estado.error}`,
    )

    assert.equal(
      estado.usuarios.length,
      1,
      `No se cargó el usuario: ${estado.error}`,
    )

    assert.deepEqual(
      estado.usuarios[0].roles,
      ['CIUDADANO'],
    )

    // Intentar cambiar el rol
    let resultado: boolean | undefined

    await act(async () => {
      resultado = await estado.cambiarRol(
        'usuario-2',
        'ADMIN',
      )
    })

    // El cambio debe fallar
    assert.equal(resultado, false)
    assert.equal(intentosPatch, 1)

    // Debe mostrarse el mensaje de permisos
    assert.match(
      estado.errorRol ?? '',
      /permisos/i,
    )

    // El usuario conserva su rol original
    assert.deepEqual(
      estado.usuarios[0].roles,
      ['CIUDADANO'],
    )

    // Se libera el bloqueo
    assert.equal(
      estado.actualizandoRolId,
      null,
    )

    await act(async () => {
      renderer.unmount()
    })
  })
})
