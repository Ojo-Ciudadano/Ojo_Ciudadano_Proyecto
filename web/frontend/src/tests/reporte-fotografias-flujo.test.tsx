import assert from 'node:assert/strict'
import {
  afterEach,
  beforeEach,
  describe,
  it,
  mock,
} from 'node:test'
import { registerHooks } from 'node:module'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import { apiClient, startNewSession } from '../services/api'

// Node no interpreta CSS. Se omiten únicamente los estilos en este test.
const registro = registerHooks({
  load(url, context, nextLoad) {
    if (url.endsWith('.css')) {
      return {
        format: 'module',
        source: 'export default {}',
        shortCircuit: true,
      }
    }

    return nextLoad(url, context)
  },
})

const { NuevoIncidentePage } = await import(
  '../pages/reportes/NuevoIncidentePage'
)

registro.deregister()

let renderer: TestRenderer.ReactTestRenderer | null = null
let creaciones: number
let subidas: string[]
let liberadas: string[]
let numeroUrl: number
let segundaFalla: boolean

const originales = new Map<
  string,
  PropertyDescriptor | undefined
>()

function instalarGlobal(nombre: string, valor: unknown) {
  originales.set(
    nombre,
    Object.getOwnPropertyDescriptor(globalThis, nombre),
  )

  Object.defineProperty(globalThis, nombre, {
    configurable: true,
    value: valor,
  })
}

function raiz() {
  assert.ok(renderer)
  return renderer.root
}

function textoPantalla() {
  assert.ok(renderer)
  return JSON.stringify(renderer.toJSON())
}

async function montarFormulario() {
  await act(async () => {
    renderer = TestRenderer.create(
      createElement(
        AuthProvider,
        null,
        createElement(
          MemoryRouter,
          null,
          createElement(NuevoIncidentePage),
        ),
      ),
    )
  })

  act(() => {
    for (const [id, value] of [
      ['reporte-titulo', 'Reporte de prueba'],
      ['reporte-descripcion', 'Descripción de prueba'],
      ['reporte-categoria', 'categoria-1'],
      ['reporte-comuna', 'Lautaro'],
      ['reporte-direccion', 'Referencia de prueba'],
      ['reporte-sector', 'Centro'],
    ]) {
      raiz().findByProps({ id }).props.onChange({
        target: { value },
      })
    }
  })

  act(() => {
    const boton = raiz()
      .findAllByType('button')
      .find((elemento) =>
        elemento.children.includes('Obtener mi ubicación'),
      )

    assert.ok(boton)
    boton.props.onClick()
  })
}

function seleccionarFotos() {
  act(() => {
    raiz()
      .findByProps({ id: 'reporte-fotos-archivos' })
      .props.onChange({
        currentTarget: {
          files: [
            new File(['primera'], 'primera.jpg', {
              type: 'image/jpeg',
            }),
            new File(['segunda'], 'segunda.jpg', {
              type: 'image/jpeg',
            }),
          ],
          value: '',
        },
      })
  })
}

async function enviarFormulario() {
  await act(async () => {
    await raiz().findByType('form').props.onSubmit({
      preventDefault() {},
    })
  })
}

async function pulsarBoton(nombre: string) {
  const boton = raiz()
    .findAllByType('button')
    .find((elemento) => elemento.children.includes(nombre))

  assert.ok(boton, `No se encontró el botón: ${nombre}`)

  await act(async () => {
    await boton.props.onClick()
  })
}

beforeEach(() => {
  creaciones = 0
  subidas = []
  liberadas = []
  numeroUrl = 0
  segundaFalla = true

  const almacenamiento = new Map<string, string>()

  instalarGlobal('localStorage', {
    getItem: (key: string) => almacenamiento.get(key) ?? null,
    setItem: (key: string, value: string) => {
      almacenamiento.set(key, String(value))
    },
    removeItem: (key: string) => {
      almacenamiento.delete(key)
    },
    clear: () => almacenamiento.clear(),
  })

  instalarGlobal('window', { isSecureContext: true })

  instalarGlobal('navigator', {
    geolocation: {
      getCurrentPosition(
        exito: (posicion: {
          coords: {
            latitude: number
            longitude: number
          }
        }) => void,
      ) {
        exito({
          coords: {
            latitude: -38.53,
            longitude: -72.43,
          },
        })
      },
    },
  })

  mock.method(URL, 'createObjectURL', () => {
    numeroUrl += 1
    return `blob:flujo-${numeroUrl}`
  })

  mock.method(URL, 'revokeObjectURL', (url: string) => {
    liberadas.push(url)
  })

  mock.method(apiClient, 'get', async (ruta: string) => {
    if (ruta === '/usuarios/me') {
      return {
        data: {
          id: 'ciudadano-1',
          nombre: 'Ciudadano',
          email: 'prueba@example.com',
          roles: ['CIUDADANO'],
        },
      }
    }

    if (ruta === '/categorias') {
      return {
        data: [{
          id: 'categoria-1',
          nombre: 'Aseo y basura',
        }],
      }
    }

    throw new Error(`Consulta inesperada: ${ruta}`)
  })

  mock.method(
    apiClient,
    'post',
    async (ruta: string, cuerpo: unknown) => {
      if (ruta === '/incidentes') {
        creaciones += 1
        return { data: { id: 'incidente-creado-1' } }
      }

      assert.equal(
        ruta,
        '/incidentes/incidente-creado-1/evidencias',
      )

      // El incidente debe existir antes de cualquier subida.
      assert.equal(creaciones, 1)
      assert.ok(cuerpo instanceof FormData)

      const foto = cuerpo.get('archivo')
      assert.ok(foto instanceof File)

      subidas.push(foto.name)

      if (foto.name === 'segunda.jpg' && segundaFalla) {
        throw { statusCode: 502 }
      }

      return {
        data: {
          id: `evidencia-${foto.name}`,
          incidenteId: 'incidente-creado-1',
        },
      }
    },
  )

  startNewSession('token-de-prueba')
})

afterEach(async () => {
  if (renderer) {
    await act(async () => {
      renderer?.unmount()
    })

    renderer = null
  }

  mock.restoreAll()

  for (const [nombre, descriptor] of originales) {
    if (descriptor) {
      Object.defineProperty(globalThis, nombre, descriptor)
    } else {
      Reflect.deleteProperty(globalThis, nombre)
    }
  }

  originales.clear()
})

describe('Flujo del formulario con fotografías (#137)', () => {
  it('crea una sola vez y reintenta únicamente la foto fallida', async () => {
    await montarFormulario()
    seleccionarFotos()
    await enviarFormulario()

    assert.equal(creaciones, 1)
    assert.deepEqual(subidas, [
      'primera.jpg',
      'segunda.jpg',
    ])

    assert.match(textoPantalla(), /Subida no confirmada/)

    segundaFalla = false

    await pulsarBoton('Reintentar fotografías fallidas')

    assert.equal(creaciones, 1)
    assert.deepEqual(subidas, [
      'primera.jpg',
      'segunda.jpg',
      'segunda.jpg',
    ])

    assert.match(
      textoPantalla(),
      /Se confirmó la subida de 2 fotografías/,
    )

    assert.deepEqual(liberadas, [
      'blob:flujo-1',
      'blob:flujo-2',
    ])
  })

  it('permite finalizar sin reenviar las fotos fallidas', async () => {
    await montarFormulario()
    seleccionarFotos()
    await enviarFormulario()

    await pulsarBoton(
      'Continuar sin las fotografías fallidas',
    )

    assert.equal(creaciones, 1)
    assert.deepEqual(subidas, [
      'primera.jpg',
      'segunda.jpg',
    ])

    assert.match(
      textoPantalla(),
      /Se confirmó la subida de 1 fotografía/,
    )

    assert.match(
      textoPantalla(),
      /1 fotografía no confirmada/,
    )

    assert.doesNotMatch(
      textoPantalla(),
      /Reintentar fotografías fallidas/,
    )

    assert.deepEqual(liberadas, [
      'blob:flujo-1',
      'blob:flujo-2',
    ])
  })

  it('crea el reporte sin fotografías y no solicita subidas', async () => {
    await montarFormulario()
    await enviarFormulario()

    assert.equal(creaciones, 1)
    assert.deepEqual(subidas, [])

    assert.match(
      textoPantalla(),
      /El reporte se creó sin fotografías/,
    )
  })
})