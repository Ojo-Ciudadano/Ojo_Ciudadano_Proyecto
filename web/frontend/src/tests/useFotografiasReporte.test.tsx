import assert from 'node:assert/strict'
import { afterEach, beforeEach, describe, it, mock } from 'node:test'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { useFotografiasReporte } from '../hooks/useFotografiasReporte'
import { apiClient } from '../services/api'

let fotografias: ReturnType<typeof useFotografiasReporte>
let renderer: TestRenderer.ReactTestRenderer | null = null
let urlsLiberadas: string[]
let siguienteUrl: number

function Observador() {
  fotografias = useFotografiasReporte()
  return null
}

function archivo(nombre: string): File {
  return new File(['imagen de prueba'], nombre, {
    type: 'image/jpeg',
  })
}

async function montar() {
  await act(async () => {
    renderer = TestRenderer.create(createElement(Observador))
  })
}

function confirmar(incidenteId: string, foto: File) {
  return {
    data: {
      id: `evidencia-${foto.name}`,
      incidenteId,
      tipoMime: foto.type,
      tamanoArchivo: foto.size,
      fechaCreacion: '2026-11-05T05:00:00.000Z',
      url: '/evidencias/prueba/archivo',
    },
  }
}

beforeEach(() => {
  urlsLiberadas = []
  siguienteUrl = 0

  mock.method(URL, 'createObjectURL', () => {
    siguienteUrl += 1
    return `blob:prueba-${siguienteUrl}`
  })

  mock.method(URL, 'revokeObjectURL', (url: string) => {
    urlsLiberadas.push(url)
  })
})

afterEach(async () => {
  if (renderer) {
    await act(async () => {
      renderer?.unmount()
    })
    renderer = null
  }

  mock.restoreAll()
})

describe('Selección y subida de fotografías (#137)', () => {
  it('quitar una fotografía libera su URL y conserva las demás', async () => {
    await montar()

    act(() => {
      fotografias.agregarFotografias([
        archivo('primera.jpg'),
        archivo('segunda.jpg'),
      ])
    })

    const primera = fotografias.fotografias[0]
    const segunda = fotografias.fotografias[1]

    act(() => {
      fotografias.quitarFotografia(primera.id)
    })

    assert.equal(fotografias.fotografias.length, 1)
    assert.equal(fotografias.fotografias[0].id, segunda.id)
    assert.deepEqual(urlsLiberadas, [primera.urlPrevisualizacion])
  })

  it('rechaza archivos inválidos antes de crear su previsualización', async () => {
    await montar()

    act(() => {
      fotografias.agregarFotografias([
        new File(['documento'], 'documento.pdf', {
          type: 'application/pdf',
        }),
        archivo('valida.jpg'),
      ])
    })

    assert.equal(fotografias.fotografias.length, 1)
    assert.equal(fotografias.erroresSeleccion.length, 1)
    assert.equal(siguienteUrl, 1)
  })

  it('reintenta solo la fotografía fallida usando el mismo incidente', async () => {
    await montar()

    const primera = archivo('primera.jpg')
    const segunda = archivo('segunda.jpg')
    const incidenteId = 'incidente-real-de-prueba'
    const solicitudes: string[] = []

    mock.method(apiClient, 'post', async (
      ruta: string,
      formulario: FormData,
    ) => {
      const foto = formulario.get('archivo')

      assert.ok(foto instanceof File)
      assert.equal(
        ruta,
        `/incidentes/${incidenteId}/evidencias`,
      )

      solicitudes.push(foto.name)

      if (foto.name === 'segunda.jpg' && solicitudes.length === 2) {
        throw new Error('Fallo temporal de almacenamiento')
      }

      return confirmar(incidenteId, foto)
    })

    act(() => {
      fotografias.agregarFotografias([primera, segunda])
    })

    await act(async () => {
      const resultado = await fotografias.subirPendientes(incidenteId)

      assert.deepEqual(resultado, {
        total: 2,
        confirmadas: 1,
        fallidas: 1,
      })
    })

    assert.deepEqual(
      fotografias.fotografias.map((foto) => foto.estado),
      ['confirmada', 'fallida'],
    )

    await act(async () => {
      const resultado = await fotografias.subirPendientes(incidenteId)

      assert.deepEqual(resultado, {
        total: 2,
        confirmadas: 2,
        fallidas: 0,
      })
    })

    assert.deepEqual(solicitudes, [
      'primera.jpg',
      'segunda.jpg',
      'segunda.jpg',
    ])

    act(() => {
      fotografias.limpiarFotografias()
    })

    assert.equal(fotografias.fotografias.length, 0)
    assert.deepEqual(urlsLiberadas, [
      'blob:prueba-1',
      'blob:prueba-2',
    ])
  })

  it('impide dos subidas simultáneas', async () => {
    await montar()

    const foto = archivo('unica.jpg')
    let finalizar!: () => void
    let llamadas = 0

    const espera = new Promise<void>((resolve) => {
      finalizar = resolve
    })

    mock.method(apiClient, 'post', async () => {
      llamadas += 1
      await espera
      return confirmar('incidente-prueba', foto)
    })

    act(() => {
      fotografias.agregarFotografias([foto])
    })

    await act(async () => {
      const primeraSubida = fotografias.subirPendientes('incidente-prueba')
      const segundaSubida = await fotografias.subirPendientes(
        'incidente-prueba',
      )

      assert.equal(segundaSubida, null)

      finalizar()
      await primeraSubida
    })

    assert.equal(llamadas, 1)
    assert.equal(fotografias.fotografias[0].estado, 'confirmada')
  })

  it('libera las previsualizaciones al desmontar el formulario', async () => {
    await montar()

    act(() => {
      fotografias.agregarFotografias([
        archivo('primera.jpg'),
        archivo('segunda.jpg'),
      ])
    })

    await act(async () => {
      renderer?.unmount()
    })
    renderer = null

    assert.deepEqual(urlsLiberadas, [
      'blob:prueba-1',
      'blob:prueba-2',
    ])
  })
})