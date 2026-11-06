import { describe, it, beforeEach, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { apiClient } from '../services/api'
import { obtenerUsuarios } from '../services/usuarios.service'
import type { UsuariosListadoResponse } from '../types/usuario'
import type { ApiError } from '../types'

// ==========================================
// SIMULACIÓN DE LOCALSTORAGE
// ==========================================

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

// ==========================================
// CONFIGURACIÓN DE PRUEBAS
// ==========================================

const originalAdapter = apiClient.defaults.adapter

const respuestaEjemplo: UsuariosListadoResponse = {
  data: [
    {
      id: 'usuario-1',
      nombre: 'Administrador',
      email: 'admin@ojociudadano.cl',
      telefono: null,
      fechaRegistro: '2026-10-01T12:00:00.000Z',
      roles: ['ADMIN'],
    },
    {
      id: 'usuario-2',
      nombre: 'Ciudadano de prueba',
      email: 'ciudadano@ejemplo.com',
      telefono: '912345678',
      fechaRegistro: '2026-10-02T12:00:00.000Z',
      roles: ['CIUDADANO'],
    },
  ],
  meta: {
    page: 1,
    limit: 10,
    total: 2,
    totalPages: 1,
  },
}

// ==========================================
// PRUEBAS ISSUE #147
// ==========================================

describe('Listado de usuarios Web (#147)', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
  })

  // ========================================
  // PRUEBA 1: CONSULTA GET
  // ========================================

  it('consulta GET /usuarios con paginación', async () => {
    let metodo = ''
    let ruta = ''
    let pagina = 0
    let limite = 0

    apiClient.defaults.adapter = async (config) => {
      metodo = config.method ?? ''
      ruta = config.url ?? ''
      pagina = Number(config.params?.page)
      limite = Number(config.params?.limit)

      return {
        data: respuestaEjemplo,
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }

    const resultado = await obtenerUsuarios(1, 10)

    assert.equal(metodo, 'get')
    assert.equal(ruta, '/usuarios')
    assert.equal(pagina, 1)
    assert.equal(limite, 10)
    assert.equal(resultado.data.length, 2)
  })

  // ========================================
  // PRUEBA 2: IDENTIFICADORES Y ROLES
  // ========================================

  it('conserva los identificadores y roles de los usuarios', async () => {
    apiClient.defaults.adapter = async (config) => ({
      data: respuestaEjemplo,
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    })

    const resultado = await obtenerUsuarios()

    assert.equal(resultado.data[0].id, 'usuario-1')
    assert.deepEqual(resultado.data[0].roles, ['ADMIN'])

    assert.equal(resultado.data[1].id, 'usuario-2')
    assert.deepEqual(resultado.data[1].roles, ['CIUDADANO'])
  })

  // ========================================
  // PRUEBA 3: PAGINACIÓN
  // ========================================

  it('respeta los metadatos de paginación del backend', async () => {
    apiClient.defaults.adapter = async (config) => ({
      data: {
        data: respuestaEjemplo.data,
        meta: {
          page: 2,
          limit: 10,
          total: 25,
          totalPages: 3,
        },
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    })

    const resultado = await obtenerUsuarios(2, 10)

    assert.equal(resultado.meta.page, 2)
    assert.equal(resultado.meta.limit, 10)
    assert.equal(resultado.meta.total, 25)
    assert.equal(resultado.meta.totalPages, 3)
  })

  // ========================================
  // PRUEBA 4: RESPUESTA INVÁLIDA
  // ========================================

  it('rechaza una respuesta inválida del servidor', async () => {
    apiClient.defaults.adapter = async (config) => ({
      data: {
        data: null,
        meta: null,
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    })

    await assert.rejects(
      () => obtenerUsuarios(),
      (error: unknown) => {
        assert.ok(error instanceof Error)
        assert.match(
          error.message,
          /respuesta de usuarios inv.lida/i,
        )
        return true
      },
    )
  })

  // ========================================
  // PRUEBA 5: ERROR DE CONEXIÓN
  // ========================================

  it('propaga los errores de conexión mediante el cliente HTTP', async () => {
    apiClient.defaults.adapter = async () => {
      throw new Error('Error de conexión')
    }

    await assert.rejects(
      () => obtenerUsuarios(),
      (error: unknown) => {
        const apiError = error as ApiError

        assert.equal(apiError.statusCode, 500)
        assert.equal(apiError.error, 'Internal Server Error')
        assert.equal(
          typeof apiError.message,
          'string',
        )

        return true
      },
    )
  })
})