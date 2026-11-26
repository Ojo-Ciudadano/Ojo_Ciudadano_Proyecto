import assert from 'node:assert/strict'
import { registerHooks } from 'node:module'
import { afterEach, describe, it } from 'node:test'
import { AxiosHeaders } from 'axios'
import { createElement } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { MemoryRouter } from 'react-router-dom'
import {
  apiClient,
  resetApiAuthStateForTests,
} from '../services/api'
import type { Incidente } from '../types/incidente'

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

const { IncidentFilterBar } = await import(
  '../components/incidentes/IncidentFilterBar'
)
const { IncidentesPage } = await import(
  '../pages/incidentes/IncidentesPage'
)

cssHooks.deregister()

const almacenamiento = new Map<string, string>()

if (typeof globalThis.localStorage === 'undefined') {
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
      clear: () => {
        almacenamiento.clear()
      },
      key: (indice: number) =>
        Array.from(almacenamiento.keys())[indice] ?? null,
      get length() {
        return almacenamiento.size
      },
    },
  })
}

const INCIDENTES: Incidente[] = Array.from(
  { length: 12 },
  (_, indice) => ({
    id: `incidente-${indice + 1}`,
    titulo: `Incidente ${indice + 1}`,
    descripcion: 'Descripcion de prueba',
    fechaCreacion: '2026-11-20T12:00:00.000Z',
    categoriaId: 'cat-alumbrado',
    estadoId: 'est-nuevo',
    categoria: {
      id: 'cat-alumbrado',
      nombre: 'Alumbrado público',
    },
    estado: {
      id: 'est-nuevo',
      nombre: 'Nuevo',
      orden: 1,
    },
    ubicacion: {
      comuna: 'Temuco',
      region: 'La Araucanía',
    },
    nivelPrioridad: 3,
  }),
)

interface Llamada {
  params?: Record<string, string>
}

const originalAdapter = apiClient.defaults.adapter
const llamadas: Llamada[] = []
let renderer: TestRenderer.ReactTestRenderer | undefined

function configurarApi(): void {
  llamadas.length = 0

  apiClient.defaults.adapter = async (config) => {
    if (
      config.method !== 'get' ||
      !String(config.url).includes('/incidentes')
    ) {
      throw new Error(`Petición inesperada: ${config.method} ${config.url}`)
    }

    llamadas.push({
      params: config.params as Record<string, string> | undefined,
    })

    return {
      data: INCIDENTES,
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders(),
      config,
    }
  }
}

async function esperarCarga(): Promise<void> {
  await act(async () => {
    await Promise.resolve()
    await Promise.resolve()
  })
}

async function montar(): Promise<void> {
  await act(async () => {
    renderer = TestRenderer.create(
      createElement(
        MemoryRouter,
        {},
        createElement(IncidentesPage),
      ),
    )
  })

  await esperarCarga()
}

function select(testId: string) {
  return renderer!.root.findByProps({
    'data-testid': testId,
  })
}

function textoPagina(): string | undefined {
  return renderer!.root
    .findAllByType('span')
    .map((nodo) => nodo.children.join(''))
    .find((texto) => texto.startsWith('Página '))
}

async function cambiarSelect(
  testId: string,
  valor: string,
): Promise<void> {
  await act(async () => {
    select(testId).props.onChange({
      target: { value: valor },
    })
    await Promise.resolve()
    await Promise.resolve()
  })
}

async function hacerClick(testId: string): Promise<void> {
  await act(async () => {
    select(testId).props.onClick()
    await Promise.resolve()
    await Promise.resolve()
  })
}

function ultimosParams(): Record<string, string> {
  return llamadas[llamadas.length - 1]?.params ?? {}
}

describe('Filtros de la tabla administrativa (#317)', () => {
  afterEach(() => {
    renderer?.unmount()
    renderer = undefined
    apiClient.defaults.adapter = originalAdapter
    resetApiAuthStateForTests()
    llamadas.length = 0
  })

  it('combina filtros, conserva la selección al paginar y limpia la consulta', async () => {
    configurarApi()
    await montar()

    assert.equal(
      renderer!.root.findAllByType(IncidentFilterBar).length,
      1,
      'La tabla debe usar IncidentFilterBar',
    )

    await cambiarSelect('incident-filter-bar-ciudad', 'Temuco')
    await cambiarSelect('incident-filter-bar-comuna', 'Temuco')
    await cambiarSelect('incident-filter-bar-estado', 'est-nuevo')
    await cambiarSelect(
      'incident-filter-bar-categoria',
      'cat-alumbrado',
    )

    assert.deepEqual(ultimosParams(), {
      ciudad: 'Temuco',
      comuna: 'Temuco',
      estadoId: 'est-nuevo',
      categoriaId: 'cat-alumbrado',
    })

    const llamadasAntesDePaginar = llamadas.length

    const botonSiguiente = renderer!.root
      .findAllByType('button')
      .find(
        (nodo) =>
          nodo.children.join('').trim() === 'Siguiente',
      )

    assert.ok(botonSiguiente, 'Debe existir el botón Siguiente')

    await act(async () => {
      botonSiguiente.props.onClick()
    })

    assert.equal(textoPagina(), 'Página 2 de 2')
    assert.equal(llamadas.length, llamadasAntesDePaginar)
    assert.equal(
      select('incident-filter-bar-ciudad').props.value,
      'Temuco',
    )
    assert.equal(
      select('incident-filter-bar-comuna').props.value,
      'Temuco',
    )
    assert.equal(
      select('incident-filter-bar-estado').props.value,
      'est-nuevo',
    )
    assert.equal(
      select('incident-filter-bar-categoria').props.value,
      'cat-alumbrado',
    )

    await cambiarSelect('incident-filter-bar-categoria', '')

    assert.equal(textoPagina(), 'Página 1 de 2')
    assert.deepEqual(ultimosParams(), {
      ciudad: 'Temuco',
      comuna: 'Temuco',
      estadoId: 'est-nuevo',
    })

    await hacerClick('incident-filter-bar-clear')

    assert.deepEqual(ultimosParams(), {})
    assert.equal(textoPagina(), 'Página 1 de 2')
    assert.equal(
      select('incident-filter-bar-ciudad').props.value,
      '',
    )
    assert.equal(
      select('incident-filter-bar-comuna').props.value,
      '',
    )
    assert.equal(
      select('incident-filter-bar-estado').props.value,
      '',
    )
    assert.equal(
      select('incident-filter-bar-categoria').props.value,
      '',
    )
  })
})
