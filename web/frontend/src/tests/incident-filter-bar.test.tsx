import assert from 'node:assert/strict'
import { registerHooks } from 'node:module'
import { afterEach, describe, it } from 'node:test'
import { createElement, useState } from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import {
  contarFiltrosActivos,
  crearFiltrosBarraVacios,
  obtenerCiudadesDisponibles,
  obtenerComunasDisponibles,
  resolverCambioCiudad,
  textoContadorFiltros,
  type FiltrosBarraIncidentes,
  type OpcionCiudadComunas,
  type OpcionFiltroIncidente,
} from '../components/incidentes/incidentFilterBar.utils'

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

cssHooks.deregister()

const CIUDADES_DEMO: OpcionCiudadComunas[] = [
  {
    ciudad: 'Temuco',
    comunas: ['Temuco', 'Padre Las Casas', 'Vilcún'],
  },
  {
    ciudad: 'Villarrica',
    comunas: ['Villarrica', 'Pucón', 'Curarrehue'],
  },
  {
    ciudad: 'Angol',
    comunas: ['Angol', 'Renaico'],
  },
]

const ESTADOS_DEMO: OpcionFiltroIncidente[] = [
  { id: 'est-nuevo', nombre: 'Nuevo' },
  { id: 'est-revision', nombre: 'En revisión' },
  { id: 'est-resuelto', nombre: 'Resuelto' },
]

const CATEGORIAS_DEMO: OpcionFiltroIncidente[] = [
  { id: 'cat-alumbrado', nombre: 'Alumbrado público' },
  { id: 'cat-baches', nombre: 'Baches y calzada' },
  { id: 'cat-basura', nombre: 'Microbasural' },
]

let renderer: TestRenderer.ReactTestRenderer | undefined

interface OpcionesMontaje {
  filtrosIniciales?: FiltrosBarraIncidentes
  ciudades?: readonly OpcionCiudadComunas[]
  estados?: readonly OpcionFiltroIncidente[]
  categorias?: readonly OpcionFiltroIncidente[]
  defaultExpanded?: boolean
  disabled?: boolean
  onChangeSpy?: (filtros: FiltrosBarraIncidentes) => void
  onLimpiarSpy?: () => void
}

function ContenedorControlado({
  filtrosIniciales = crearFiltrosBarraVacios(),
  ciudades = CIUDADES_DEMO,
  estados = ESTADOS_DEMO,
  categorias = CATEGORIAS_DEMO,
  defaultExpanded = false,
  disabled = false,
  onChangeSpy,
  onLimpiarSpy,
}: OpcionesMontaje) {
  const [filtros, setFiltros] =
    useState<FiltrosBarraIncidentes>(filtrosIniciales)

  return createElement(IncidentFilterBar, {
    filtros,
    ciudades,
    estados,
    categorias,
    defaultExpanded,
    disabled,
    idPrefix: 'test-filtros',
    onChange: (actualizados) => {
      setFiltros(actualizados)
      onChangeSpy?.(actualizados)
    },
    onLimpiar: onLimpiarSpy,
  })
}

async function montar(opciones: OpcionesMontaje = {}) {
  await act(async () => {
    renderer = TestRenderer.create(
      createElement(ContenedorControlado, opciones),
    )
  })

  return renderer!
}

function leerOpcionesSelect(
  vista: TestRenderer.ReactTestRenderer,
  testId: string,
): string[] {
  const select = vista.root.findByProps({ 'data-testid': testId })
  return select
    .findAllByType('option')
    .map((option) => String(option.props.value))
}

afterEach(async () => {
  await act(async () => {
    renderer?.unmount()
  })

  renderer = undefined
})

describe('incidentFilterBar helpers (#316)', () => {
  it('cuenta los filtros activos ignorando espacios vacíos', () => {
    assert.equal(contarFiltrosActivos(crearFiltrosBarraVacios()), 0)
    assert.equal(
      contarFiltrosActivos({
        ciudad: 'Temuco',
        comuna: '   ',
        estadoId: 'est-nuevo',
        categoriaId: '',
      }),
      2,
    )
    assert.equal(
      contarFiltrosActivos({
        ciudad: 'Temuco',
        comuna: 'Padre Las Casas',
        estadoId: 'est-nuevo',
        categoriaId: 'cat-baches',
      }),
      4,
    )
  })

  it('formatea la descripción accesible del contador', () => {
    assert.equal(textoContadorFiltros(0), 'Sin filtros activos')
    assert.equal(textoContadorFiltros(1), '1 filtro activo')
    assert.equal(textoContadorFiltros(3), '3 filtros activos')
  })

  it('deduplica y ordena ciudades y comunas disponibles', () => {
    assert.deepEqual(obtenerCiudadesDisponibles(CIUDADES_DEMO), [
      'Angol',
      'Temuco',
      'Villarrica',
    ])

    assert.deepEqual(obtenerComunasDisponibles(CIUDADES_DEMO, 'Temuco'), [
      'Padre Las Casas',
      'Temuco',
      'Vilcún',
    ])

    assert.deepEqual(obtenerComunasDisponibles(CIUDADES_DEMO, ''), [
      'Angol',
      'Curarrehue',
      'Padre Las Casas',
      'Pucón',
      'Renaico',
      'Temuco',
      'Vilcún',
      'Villarrica',
    ])
  })

  it('limpia la comuna al cambiar a una ciudad incompatible y la conserva si sigue siendo válida', () => {
    const conComuna = {
      ciudad: 'Temuco',
      comuna: 'Padre Las Casas',
      estadoId: 'est-nuevo',
      categoriaId: 'cat-baches',
    }

    assert.deepEqual(
      resolverCambioCiudad(conComuna, 'Villarrica', CIUDADES_DEMO),
      {
        ciudad: 'Villarrica',
        comuna: '',
        estadoId: 'est-nuevo',
        categoriaId: 'cat-baches',
      },
    )

    assert.deepEqual(resolverCambioCiudad(conComuna, '', CIUDADES_DEMO), {
      ciudad: '',
      comuna: 'Padre Las Casas',
      estadoId: 'est-nuevo',
      categoriaId: 'cat-baches',
    })

    const ciudadesCompartidas: OpcionCiudadComunas[] = [
      { ciudad: 'Temuco', comunas: ['Temuco', 'Freire'] },
      { ciudad: 'Pitrufquén', comunas: ['Pitrufquén', 'Freire'] },
    ]

    assert.deepEqual(
      resolverCambioCiudad(
        {
          ciudad: 'Temuco',
          comuna: 'Freire',
          estadoId: '',
          categoriaId: '',
        },
        'Pitrufquén',
        ciudadesCompartidas,
      ),
      {
        ciudad: 'Pitrufquén',
        comuna: 'Freire',
        estadoId: '',
        categoriaId: '',
      },
    )
  })
})

describe('IncidentFilterBar (#316)', () => {
  it('expande y contrae la barra conservando los filtros y el contador visible', async () => {
    const vista = await montar({
      filtrosIniciales: {
        ciudad: 'Temuco',
        comuna: 'Padre Las Casas',
        estadoId: 'est-nuevo',
        categoriaId: '',
      },
      defaultExpanded: false,
    })

    const toggle = vista.root.findByProps({
      'data-testid': 'incident-filter-bar-toggle',
    })
    const contadorCerrado = vista.root.findByProps({
      'data-testid': 'incident-filter-bar-count',
    })

    assert.equal(toggle.props['aria-expanded'], false)
    assert.equal(
      vista.root.findAllByProps({
        'data-testid': 'incident-filter-bar-panel',
      }).length,
      0,
    )
    assert.equal(contadorCerrado.children.join(''), '3')
    assert.equal(
      contadorCerrado.props['aria-label'],
      '3 filtros activos',
    )

    await act(async () => {
      toggle.props.onClick()
    })

    assert.equal(toggle.props['aria-expanded'], true)
    assert.equal(
      vista.root.findAllByProps({
        'data-testid': 'incident-filter-bar-panel',
      }).length,
      1,
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-ciudad' })
        .props.value,
      'Temuco',
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-comuna' })
        .props.value,
      'Padre Las Casas',
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-estado' })
        .props.value,
      'est-nuevo',
    )

    await act(async () => {
      toggle.props.onClick()
    })

    assert.equal(toggle.props['aria-expanded'], false)
    assert.equal(
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-count' })
        .children.join(''),
      '3',
    )

    await act(async () => {
      toggle.props.onClick()
    })

    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-ciudad' })
        .props.value,
      'Temuco',
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-comuna' })
        .props.value,
      'Padre Las Casas',
    )
  })

  it('actualiza comunas en cascada al cambiar de ciudad y limpia la comuna inválida', async () => {
    const historial: FiltrosBarraIncidentes[] = []

    const vista = await montar({
      defaultExpanded: true,
      onChangeSpy: (filtros) => historial.push(filtros),
    })

    const selectCiudad = vista.root.findByProps({
      'data-testid': 'incident-filter-bar-ciudad',
    })
    const selectComuna = vista.root.findByProps({
      'data-testid': 'incident-filter-bar-comuna',
    })

    await act(async () => {
      selectCiudad.props.onChange({ target: { value: 'Temuco' } })
    })

    assert.deepEqual(
      leerOpcionesSelect(vista, 'incident-filter-bar-comuna'),
      ['', 'Padre Las Casas', 'Temuco', 'Vilcún'],
    )

    await act(async () => {
      selectComuna.props.onChange({ target: { value: 'Padre Las Casas' } })
    })

    assert.equal(selectComuna.props.value, 'Padre Las Casas')

    await act(async () => {
      selectCiudad.props.onChange({ target: { value: 'Villarrica' } })
    })

    assert.deepEqual(
      leerOpcionesSelect(vista, 'incident-filter-bar-comuna'),
      ['', 'Curarrehue', 'Pucón', 'Villarrica'],
    )
    assert.equal(selectComuna.props.value, '')
    assert.deepEqual(historial[historial.length - 1], {
      ciudad: 'Villarrica',
      comuna: '',
      estadoId: '',
      categoriaId: '',
    })
    assert.equal(
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-count' })
        .children.join(''),
      '1',
    )
  })

  it('permite combinar ciudad, comuna, estado y categoría y comunica cada cambio', async () => {
    const cambios: FiltrosBarraIncidentes[] = []

    const vista = await montar({
      defaultExpanded: true,
      onChangeSpy: (filtros) => cambios.push(filtros),
    })

    await act(async () => {
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-ciudad' })
        .props.onChange({ target: { value: 'Angol' } })
    })

    await act(async () => {
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-comuna' })
        .props.onChange({ target: { value: 'Renaico' } })
    })

    await act(async () => {
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-estado' })
        .props.onChange({ target: { value: 'est-revision' } })
    })

    await act(async () => {
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-categoria' })
        .props.onChange({ target: { value: 'cat-alumbrado' } })
    })

    assert.deepEqual(cambios[cambios.length - 1], {
      ciudad: 'Angol',
      comuna: 'Renaico',
      estadoId: 'est-revision',
      categoriaId: 'cat-alumbrado',
    })

    const contador = vista.root.findByProps({
      'data-testid': 'incident-filter-bar-count',
    })
    assert.equal(contador.children.join(''), '4')
    assert.equal(contador.props['aria-label'], '4 filtros activos')
  })

  it('limpia todas las selecciones, notifica el cambio y reinicia el contador a cero', async () => {
    const cambios: FiltrosBarraIncidentes[] = []
    let vecesLimpiado = 0

    const vista = await montar({
      defaultExpanded: true,
      filtrosIniciales: {
        ciudad: 'Villarrica',
        comuna: 'Pucón',
        estadoId: 'est-resuelto',
        categoriaId: 'cat-basura',
      },
      onChangeSpy: (filtros) => cambios.push(filtros),
      onLimpiarSpy: () => {
        vecesLimpiado += 1
      },
    })

    const botonLimpiar = vista.root.findByProps({
      'data-testid': 'incident-filter-bar-clear',
    })
    assert.equal(botonLimpiar.props.disabled, false)

    await act(async () => {
      botonLimpiar.props.onClick()
    })

    assert.equal(vecesLimpiado, 1)
    assert.deepEqual(cambios[cambios.length - 1], crearFiltrosBarraVacios())
    assert.equal(
      vista.root
        .findByProps({ 'data-testid': 'incident-filter-bar-count' })
        .children.join(''),
      '0',
    )
    assert.equal(botonLimpiar.props.disabled, true)
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-ciudad' })
        .props.value,
      '',
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-comuna' })
        .props.value,
      '',
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-estado' })
        .props.value,
      '',
    )
    assert.equal(
      vista.root.findByProps({
        'data-testid': 'incident-filter-bar-categoria',
      }).props.value,
      '',
    )
  })

  it('deshabilita el botón limpiar sin filtros activos y respeta la prop disabled', async () => {
    const vista = await montar({
      defaultExpanded: true,
      disabled: true,
      filtrosIniciales: {
        ciudad: 'Temuco',
        comuna: 'Temuco',
        estadoId: '',
        categoriaId: '',
      },
    })

    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-clear' })
        .props.disabled,
      true,
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-ciudad' })
        .props.disabled,
      true,
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-comuna' })
        .props.disabled,
      true,
    )
    assert.equal(
      vista.root.findByProps({ 'data-testid': 'incident-filter-bar-estado' })
        .props.disabled,
      true,
    )
    assert.equal(
      vista.root.findByProps({
        'data-testid': 'incident-filter-bar-categoria',
      }).props.disabled,
      true,
    )
  })
})
