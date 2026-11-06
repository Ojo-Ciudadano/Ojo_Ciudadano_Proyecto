import assert from 'node:assert/strict'
import { afterEach, describe, it, mock } from 'node:test'
import { apiClient } from '../services/api'
import {
  interpretarErrorEvidencia,
  normalizarEvidenciasIncidente,
  obtenerArchivoEvidencia,
  obtenerRutaArchivoEvidencia,
} from '../services/evidenciasIncidente.service'
import type { EvidenciaIncidente } from '../types/evidenciaIncidente'

function crearEvidencia(
  cambios: Partial<EvidenciaIncidente> = {},
): EvidenciaIncidente {
  return {
    id: 'foto-a',
    incidenteId: 'incidente-a',
    tipoMime: 'image/jpeg',
    tamanoArchivo: 100,
    fechaCreacion: '2026-11-05T12:00:00.000Z',
    url: '/evidencias/foto-a/archivo',
    ...cambios,
  }
}

afterEach(() => {
  mock.restoreAll()
})

describe('Servicio de fotografías del incidente (#138)', () => {
  it('acepta un incidente sin evidencias', () => {
    assert.deepEqual(
      normalizarEvidenciasIncidente([], 'incidente-a'),
      [],
    )
  })

  it('conserva varias fotografías y su orden', () => {
    const evidencias = [
      crearEvidencia(),
      crearEvidencia({
        id: 'foto-b',
        tipoMime: 'image/png',
        url: '/evidencias/foto-b/archivo',
      }),
    ]

    assert.deepEqual(
      normalizarEvidenciasIncidente(evidencias, 'incidente-a'),
      evidencias,
    )
  })

  it('rechaza evidencias de otro incidente y duplicadas', () => {
    assert.throws(() => {
      normalizarEvidenciasIncidente(
        [crearEvidencia({ incidenteId: 'otro-incidente' })],
        'incidente-a',
      )
    })

    assert.throws(() => {
      normalizarEvidenciasIncidente(
        [crearEvidencia(), crearEvidencia()],
        'incidente-a',
      )
    })
  })

  it('rechaza colecciones y metadatos inválidos', () => {
    for (const payload of [null, undefined, {}, 'incorrecto']) {
      assert.throws(() => {
        normalizarEvidenciasIncidente(payload, 'incidente-a')
      })
    }

    const casos: Partial<EvidenciaIncidente>[] = [
      { id: '' },
      { tipoMime: 'application/pdf' },
      { tamanoArchivo: 0 },
      { tamanoArchivo: -1 },
      { tamanoArchivo: 1.5 },
      { fechaCreacion: 'fecha incorrecta' },
    ]

    for (const cambios of casos) {
      assert.throws(() => {
        normalizarEvidenciasIncidente(
          [crearEvidencia(cambios)],
          'incidente-a',
        )
      })
    }
  })

  it('acepta las dos rutas de recuperación del backend', () => {
    assert.equal(
      obtenerRutaArchivoEvidencia(crearEvidencia()),
      '/evidencias/foto-a/archivo',
    )

    const ruta =
      '/incidentes/incidente-a/evidencias/foto-a/archivo'

    assert.equal(
      obtenerRutaArchivoEvidencia(
        crearEvidencia({ url: ruta }),
      ),
      ruta,
    )
  })

  it('rechaza referencias externas antes de realizar peticiones', async () => {
    const consulta = mock.method(apiClient, 'get', async () => {
      throw new Error('No debería realizarse esta petición')
    })

    for (const url of [
      'https://externo.example/foto.jpg',
      '//externo.example/foto.jpg',
      '/evidencias/otra-foto/archivo',
    ]) {
      await assert.rejects(
        obtenerArchivoEvidencia(crearEvidencia({ url })),
      )
    }

    assert.equal(consulta.mock.callCount(), 0)
  })

  it('recupera un Blob con el cliente autenticado y permite cancelar', async () => {
    const archivo = new Blob(['imagen'], {
      type: 'image/jpeg',
    })
    const controller = new AbortController()

    const consulta = mock.method(apiClient, 'get', async () => ({
      data: archivo,
    }))

    const resultado = await obtenerArchivoEvidencia(
      crearEvidencia(),
      controller.signal,
    )

    assert.equal(resultado, archivo)
    assert.equal(consulta.mock.callCount(), 1)
    assert.deepEqual(consulta.mock.calls[0].arguments, [
      '/evidencias/foto-a/archivo',
      {
        responseType: 'blob',
        signal: controller.signal,
      },
    ])
  })

  it('rechaza archivos vacíos y respuestas que no son imágenes', async () => {
    const respuestas: unknown[] = [
      null,
      {},
      new Blob([], { type: 'image/jpeg' }),
      new Blob(['texto'], { type: 'text/plain' }),
      new Blob(['pdf'], { type: 'application/pdf' }),
    ]

    mock.method(apiClient, 'get', async () => ({
      data: respuestas.shift(),
    }))

    for (let indice = 0; indice < 5; indice += 1) {
      await assert.rejects(
        obtenerArchivoEvidencia(crearEvidencia()),
      )
    }
  })

  it('propaga los fallos para que cada fotografía gestione su error', async () => {
    const error = { statusCode: 503 }

    mock.method(apiClient, 'get', async () => {
      throw error
    })

    await assert.rejects(
      obtenerArchivoEvidencia(crearEvidencia()),
      (recibido: unknown) => recibido === error,
    )
  })

  it('ofrece reintento ante fallos recuperables sin mostrar textos técnicos', () => {
    for (const statusCode of [401, 403, 404]) {
      const resultado = interpretarErrorEvidencia({ statusCode })

      assert.equal(resultado.permiteReintentar, false)
      assert.ok(resultado.mensaje)
    }

    for (const error of [
      { statusCode: 502 },
      { statusCode: 503 },
      new Error('Mensaje técnico interno'),
    ]) {
      const resultado = interpretarErrorEvidencia(error)

      assert.equal(resultado.permiteReintentar, true)
      assert.match(resultado.mensaje, /reintentar/i)
      assert.doesNotMatch(resultado.mensaje, /técnico interno/)
    }
  })
})