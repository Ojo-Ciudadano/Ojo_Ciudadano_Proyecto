import assert from 'node:assert/strict'
import { registerHooks } from 'node:module'
import {
  after,
  afterEach,
  before,
  describe,
  it,
} from 'node:test'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { AuthProvider } from '../contexts/AuthContext'
import { prepararFotografiasGaleria } from '../services/evidenciasIncidente.service'
import type { EvidenciaIncidente } from '../types/evidenciaIncidente'

const cssHooks = registerHooks({
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

const { GaleriaEvidencias } = await import(
  '../components/incidentes/GaleriaEvidencias'
)

cssHooks.deregister()

function evidencia(id: string): EvidenciaIncidente {
  return {
    id,
    incidenteId: 'incidente-a',
    tipoMime: 'image/jpeg',
    tamanoArchivo: 100,
    fechaCreacion: '2026-11-05T12:00:00.000Z',
    url: `/evidencias/${id}/archivo`,
  }
}

const originalStorage = Object.getOwnPropertyDescriptor(
  globalThis,
  'localStorage',
)

let renderer: TestRenderer.ReactTestRenderer | undefined

async function montar(evidencias: unknown) {
  await act(async () => {
    renderer = TestRenderer.create(
      createElement(
        AuthProvider,
        null,
        createElement(GaleriaEvidencias, {
          incidenteId: 'incidente-a',
          evidencias,
        }),
      ),
    )
  })

  return renderer!
}

before(() => {
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: {
      getItem: () => null,
      setItem: () => {},
      removeItem: () => {},
      clear: () => {},
    },
  })
})

afterEach(async () => {
  await act(async () => {
    renderer?.unmount()
  })

  renderer = undefined
})

after(() => {
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

describe('Galería de evidencias (#138)', () => {
  for (const [nombre, valor] of [
    ['null', null],
    ['undefined', undefined],
    ['arreglo vacío', []],
  ] as const) {
    it(`muestra el estado vacío con ${nombre}`, async () => {
      assert.deepEqual(
        prepararFotografiasGaleria(valor, 'incidente-a'),
        [],
      )

      const vista = await montar(valor)
      const contenido = JSON.stringify(vista.toJSON())

      assert.match(
        contenido,
        /Este incidente no tiene fotografías asociadas/,
      )
      assert.equal(
        vista.root.findAllByProps({ role: 'alert' }).length,
        0,
      )
    })
  }

  it('conserva dos fotografías válidas alrededor de una inválida', async () => {
    const primera = evidencia('foto-a')
    const tercera = evidencia('foto-c')
    const coleccion = [
      primera,
      {
        ...evidencia('foto-b'),
        tipoMime: 'application/pdf',
      },
      tercera,
    ]

    const preparadas = prepararFotografiasGaleria(
      coleccion,
      'incidente-a',
    )

    assert.equal(preparadas.length, 3)
    assert.deepEqual(preparadas[0].evidencia, primera)
    assert.equal(preparadas[1].evidencia, null)
    assert.equal(
      preparadas[1].error,
      'Fotografía 2 no disponible.',
    )
    assert.deepEqual(preparadas[2].evidencia, tercera)

    const vista = await montar(coleccion)

    assert.equal(vista.root.findAllByType('li').length, 3)

    const avisos = vista.root.findAllByProps({
      role: 'alert',
    })

    assert.equal(avisos.length, 1)
    assert.deepEqual(
      avisos[0].children,
      ['Fotografía 2 no disponible.'],
    )

    const pies = vista.root
      .findAllByType('figcaption')
      .map((pie) => pie.children.join(''))

    assert.deepEqual(pies, [
      'Fotografía 1',
      'Fotografía 3',
    ])
  })

  it('aísla duplicados, referencias externas y evidencias de otro incidente', () => {
    const primera = evidencia('foto-a')
    const ultima = evidencia('foto-e')

    const resultado = prepararFotografiasGaleria(
      [
        primera,
        primera,
        {
          ...evidencia('foto-c'),
          url: 'https://externo.example/foto.jpg',
        },
        {
          ...evidencia('foto-d'),
          incidenteId: 'otro-incidente',
        },
        ultima,
      ],
      'incidente-a',
    )

    assert.deepEqual(
      resultado.map((foto) => foto.evidencia?.id ?? null),
      ['foto-a', null, null, null, 'foto-e'],
    )
  })

  it('distingue una colección malformada de un incidente sin fotografías', async () => {
    assert.throws(() => {
      prepararFotografiasGaleria({}, 'incidente-a')
    })

    const vista = await montar({})
    const contenido = JSON.stringify(vista.toJSON())

    assert.match(
      contenido,
      /No se pudo interpretar la información/,
    )
    assert.doesNotMatch(
      contenido,
      /Este incidente no tiene fotografías asociadas/,
    )
  })
})