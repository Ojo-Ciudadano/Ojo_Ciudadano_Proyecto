import assert from 'node:assert/strict'
import { afterEach, describe, it, mock } from 'node:test'
import { apiClient } from '../services/api'
import {
  mensajeErrorFotografia,
  subirFotografiaReporte,
} from '../services/fotografiasReporte.service'

function crearArchivo(): File {
  return new File(['imagen de prueba'], 'fotografia.jpg', {
    type: 'image/jpeg',
  })
}

afterEach(() => {
  mock.restoreAll()
})

describe('Servicio de subida de fotografías (#137)', () => {
  it('utiliza el incidente real y el campo multipart archivo', async () => {
    const archivo = crearArchivo()

    const solicitud = mock.method(
      apiClient,
      'post',
      async () => ({
        data: {
          id: 'evidencia-1',
          incidenteId: 'incidente-1',
        },
      }),
    )

    const resultado = await subirFotografiaReporte(
      ' incidente-1 ',
      archivo,
    )

    assert.deepEqual(resultado, {
      id: 'evidencia-1',
      incidenteId: 'incidente-1',
    })

    assert.equal(solicitud.mock.callCount(), 1)

    const argumentos = solicitud.mock.calls[0].arguments

    assert.equal(
      argumentos[0],
      '/incidentes/incidente-1/evidencias',
    )

    const formulario = argumentos[1]

    assert.ok(formulario instanceof FormData)
    assert.deepEqual(
      Array.from(formulario.keys()),
      ['archivo'],
    )

    const enviada = formulario.get('archivo')

    assert.ok(enviada instanceof File)
    assert.equal(enviada.name, archivo.name)
    assert.equal(enviada.type, archivo.type)
    assert.equal(enviada.size, archivo.size)
    assert.equal(await enviada.text(), await archivo.text())

    assert.deepEqual(argumentos[2], {
      headers: {
        'Content-Type': null,
      },
      timeout: 120_000,
    })
  })

  it('acepta una confirmación aunque cambien los metadatos secundarios', async () => {
    mock.method(apiClient, 'post', async () => ({
      data: {
        id: 'evidencia-1',
        incidenteId: 'incidente-1',
        tipoMime: 'image/JPEG',
        tamanoBytes: 123,
      },
    }))

    assert.deepEqual(
      await subirFotografiaReporte(
        'incidente-1',
        crearArchivo(),
      ),
      {
        id: 'evidencia-1',
        incidenteId: 'incidente-1',
      },
    )
  })

  it('rechaza un incidente vacío antes de enviar el archivo', async () => {
    const solicitud = mock.method(
      apiClient,
      'post',
      async () => ({ data: {} }),
    )

    await assert.rejects(
      subirFotografiaReporte('  ', crearArchivo()),
      /identificador del incidente/,
    )

    assert.equal(solicitud.mock.callCount(), 0)
  })

  it('rechaza un archivo inválido antes de realizar la petición', async () => {
    const solicitud = mock.method(
      apiClient,
      'post',
      async () => ({ data: {} }),
    )

    const archivo = new File(['documento'], 'documento.pdf', {
      type: 'application/pdf',
    })

    await assert.rejects(
      subirFotografiaReporte('incidente-1', archivo),
      /Formato no permitido/,
    )

    assert.equal(solicitud.mock.callCount(), 0)
  })

  it('no confirma respuestas sin identificador o de otro incidente', async () => {
    const respuestas: unknown[] = [
      null,
      {},
      { id: '', incidenteId: 'incidente-1' },
      { id: 'foto-1', incidenteId: 'otro-incidente' },
    ]

    for (const data of respuestas) {
      const solicitud = mock.method(
        apiClient,
        'post',
        async () => ({ data }),
      )

      try {
        await assert.rejects(
          subirFotografiaReporte(
            'incidente-1',
            crearArchivo(),
          ),
          /Puede haberse guardado/,
        )
      } finally {
        solicitud.mock.restore()
      }
    }
  })

  it('no repite automáticamente un POST cuando falla', async () => {
    const fallo = { statusCode: 502 }

    const solicitud = mock.method(
      apiClient,
      'post',
      async () => {
        throw fallo
      },
    )

    await assert.rejects(
      subirFotografiaReporte(
        'incidente-1',
        crearArchivo(),
      ),
      (error: unknown) => error === fallo,
    )

    assert.equal(solicitud.mock.callCount(), 1)
  })

  it('presenta mensajes comprensibles para los errores del backend', () => {
    const casos = [
      [400, /rechazó la fotografía/],
      [401, /sesión/],
      [403, /permisos/],
      [404, /incidente no está disponible/],
      [413, /10 MB/],
      [502, /almacenamiento/],
      [503, /almacenamiento/],
    ] as const

    for (const [statusCode, mensaje] of casos) {
      assert.match(
        mensajeErrorFotografia({ statusCode }),
        mensaje,
      )
    }
  })

  it('no expone mensajes técnicos ante un resultado incierto', () => {
    const mensaje = mensajeErrorFotografia(
      new Error('Request failed with status code 500'),
    )

    assert.doesNotMatch(mensaje, /Request failed/)
    assert.match(mensaje, /podría haberse guardado/)
    assert.match(mensaje, /antes de reenviarla/)
  })
})