import assert from 'node:assert/strict'
import {
  afterEach,
  beforeEach,
  describe,
  it,
  mock,
} from 'node:test'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import type { AxiosAdapter, AxiosResponse } from 'axios'
import { AuthProvider } from '../contexts/AuthContext'
import { useFotografiaEvidencia } from '../hooks/useFotografiaEvidencia'
import { apiClient, startNewSession } from '../services/api'
import type { EvidenciaIncidente } from '../types/evidenciaIncidente'

const evidenciaA: EvidenciaIncidente = {
  id: 'foto-a',
  incidenteId: 'incidente-a',
  tipoMime: 'image/jpeg',
  tamanoArchivo: 100,
  fechaCreacion: '2026-11-05T12:00:00.000Z',
  url: '/evidencias/foto-a/archivo',
}

const evidenciaB: EvidenciaIncidente = {
  ...evidenciaA,
  id: 'foto-b',
  incidenteId: 'incidente-b',
  url: '/evidencias/foto-b/archivo',
}

const usuario = {
  id: 'usuario-a',
  email: 'ciudadano@ejemplo.cl',
  roles: ['CIUDADANO'],
}

const originalAdapter = apiClient.defaults.adapter
const originalStorage = Object.getOwnPropertyDescriptor(
  globalThis,
  'localStorage',
)

let renderer: TestRenderer.ReactTestRenderer | undefined
let fotografia: ReturnType<typeof useFotografiaEvidencia>
let creadas: string[]
let liberadas: string[]

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

function imagen() {
  return new Blob(['imagen'], { type: 'image/jpeg' })
}

function Observador({
  evidencia,
}: {
  evidencia: EvidenciaIncidente
}) {
  fotografia = useFotografiaEvidencia(evidencia)
  return null
}

function formulario(evidencia: EvidenciaIncidente) {
  return createElement(
    AuthProvider,
    null,
    createElement(Observador, { evidencia }),
  )
}

async function montar(evidencia = evidenciaA) {
  await act(async () => {
    renderer = TestRenderer.create(formulario(evidencia))
  })
}

async function desmontar() {
  await act(async () => {
    renderer?.unmount()
  })
  renderer = undefined
}

beforeEach(() => {
  const almacenamiento = new Map<string, string>()
  creadas = []
  liberadas = []

  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: {
      getItem: (clave: string) =>
        almacenamiento.get(clave) ?? null,
      setItem: (clave: string, valor: string) => {
        almacenamiento.set(clave, String(valor))
      },
      removeItem: (clave: string) => {
        almacenamiento.delete(clave)
      },
      clear: () => almacenamiento.clear(),
    },
  })

  mock.method(URL, 'createObjectURL', () => {
    const url = `blob:prueba-${creadas.length + 1}`
    creadas.push(url)
    return url
  })

  mock.method(URL, 'revokeObjectURL', (url: string) => {
    liberadas.push(url)
  })
})

afterEach(async () => {
  await desmontar()
  apiClient.defaults.adapter = originalAdapter
  mock.restoreAll()

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

describe('Carga de fotografías del incidente (#138)', () => {
  it('no solicita fotografías sin sesión', async () => {
    let consultas = 0

    apiClient.defaults.adapter = async (config) => {
      consultas += 1
      return respuesta(config, imagen())
    }

    await montar()

    assert.equal(consultas, 0)
    assert.equal(fotografia.sesionDisponible, false)
    assert.equal(fotografia.cargando, false)
    assert.equal(fotografia.url, null)
    assert.deepEqual(creadas, [])
  })

  it('consulta con autenticación y libera la imagen al desmontar', async () => {
    let token: unknown
    let signal: AbortSignal | undefined

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuario)
      }

      assert.equal(config.url, evidenciaA.url)
      assert.equal(config.responseType, 'blob')
      token = config.headers.Authorization
      signal = config.signal as AbortSignal
      return respuesta(config, imagen())
    }

    startNewSession('token-prueba')
    await montar()

    assert.equal(token, 'Bearer token-prueba')
    assert.equal(fotografia.url, 'blob:prueba-1')
    assert.equal(fotografia.error, null)
    assert.equal(fotografia.cargando, false)
    assert.equal(signal?.aborted, false)

    await desmontar()

    assert.equal(signal?.aborted, true)
    assert.deepEqual(liberadas, ['blob:prueba-1'])
  })

  it('permite reintentar una fotografía después de un fallo', async () => {
    let consultas = 0

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuario)
      }

      consultas += 1

      if (consultas === 1) {
        throw new Error('Fallo de conexión')
      }

      return respuesta(config, imagen())
    }

    startNewSession('token-prueba')
    await montar()

    assert.equal(fotografia.url, null)
    assert.equal(fotografia.error?.permiteReintentar, true)

    await act(async () => {
      fotografia.reintentar()
    })

    assert.equal(consultas, 2)
    assert.equal(fotografia.error, null)
    assert.equal(fotografia.url, 'blob:prueba-1')
  })

  it('oculta la fotografía anterior mientras carga otro incidente', async () => {
    let completar: (() => void) | undefined
    let signalB: AbortSignal | undefined

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuario)
      }

      if (config.url === evidenciaB.url) {
        signalB = config.signal as AbortSignal

        return new Promise<AxiosResponse>((resolve) => {
          completar = () => resolve(respuesta(config, imagen()))
        })
      }

      return respuesta(config, imagen())
    }

    startNewSession('token-prueba')
    await montar()

    assert.equal(fotografia.url, 'blob:prueba-1')

    await act(async () => {
      renderer?.update(formulario(evidenciaB))
    })

    assert.equal(fotografia.url, null)
    assert.equal(fotografia.cargando, true)
    assert.deepEqual(liberadas, ['blob:prueba-1'])
    assert.ok(completar)

    await act(async () => {
      completar?.()
    })

    assert.equal(fotografia.url, 'blob:prueba-2')
    assert.equal(fotografia.cargando, false)

    await desmontar()

    assert.equal(signalB?.aborted, true)
    assert.deepEqual(liberadas, [
      'blob:prueba-1',
      'blob:prueba-2',
    ])
  })

  it('permite recuperar una imagen que el navegador no pudo mostrar', async () => {
    apiClient.defaults.adapter = async (config) => {
      return respuesta(
        config,
        config.url === '/usuarios/me' ? usuario : imagen(),
      )
    }

    startNewSession('token-prueba')
    await montar()

    await act(async () => {
      fotografia.informarErrorImagen()
    })

    assert.equal(fotografia.url, null)
    assert.equal(fotografia.error?.permiteReintentar, true)

    await act(async () => {
      fotografia.reintentar()
    })

    assert.equal(fotografia.url, 'blob:prueba-2')
    assert.equal(fotografia.error, null)
    assert.deepEqual(liberadas, ['blob:prueba-1'])
  })

  it('descarta respuestas pendientes después de desmontar', async () => {
    let completar: (() => void) | undefined
    let signal: AbortSignal | undefined

    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/usuarios/me') {
        return respuesta(config, usuario)
      }

      signal = config.signal as AbortSignal

      return new Promise<AxiosResponse>((resolve) => {
        completar = () => resolve(respuesta(config, imagen()))
      })
    }

    startNewSession('token-prueba')
    await montar()

    assert.equal(fotografia.cargando, true)
    assert.ok(completar)

    await desmontar()

    assert.equal(signal?.aborted, true)

    await act(async () => {
      completar?.()
    })

    assert.deepEqual(creadas, [])
    assert.deepEqual(liberadas, [])
  })
})