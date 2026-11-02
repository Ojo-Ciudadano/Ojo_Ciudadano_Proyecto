import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  ARAUCANIA_BOUNDS,
  ARAUCANIA_CENTER,
  DEFAULT_MARKER_COLOR,
  DEFAULT_ZOOM,
  MAX_ZOOM,
  MIN_ZOOM,
  OSM_ATTRIBUTION,
  OSM_TILE_URL,
  TILE_ERROR_MESSAGE,
  colorPorPrioridad,
  esCoordenadaValida,
  normalizarMarcadores,
  type IncidentMapMarker,
} from '../components/map/mapConfig'
import { NAVIGATION_RULES, ROLES, getVisibleNavigation } from '../config/navigation'

const marcadorBase: IncidentMapMarker = {
  id: 'INC-1',
  latitud: -38.7359,
  longitud: -72.5904,
  titulo: 'Incendio forestal',
  prioridad: 1,
}

describe('Configuración del mapa de incidentes (#139)', () => {
  it('define un centro y zoom iniciales coherentes con La Araucanía', () => {
    assert.equal(esCoordenadaValida(ARAUCANIA_CENTER[0], ARAUCANIA_CENTER[1]), true)

    const [suroeste, noreste] = ARAUCANIA_BOUNDS as [number, number][]
    assert.ok(ARAUCANIA_CENTER[0] >= suroeste[0] && ARAUCANIA_CENTER[0] <= noreste[0])
    assert.ok(ARAUCANIA_CENTER[1] >= suroeste[1] && ARAUCANIA_CENTER[1] <= noreste[1])

    assert.ok(DEFAULT_ZOOM >= MIN_ZOOM)
    assert.ok(DEFAULT_ZOOM <= MAX_ZOOM)
  })

  it('usa la capa base de OpenStreetMap con atribución y placeholders de tiles', () => {
    assert.match(OSM_TILE_URL, /\{z\}/)
    assert.match(OSM_TILE_URL, /\{x\}/)
    assert.match(OSM_TILE_URL, /\{y\}/)
    assert.match(OSM_ATTRIBUTION, /OpenStreetMap/)
    assert.match(OSM_ATTRIBUTION, /openstreetmap\.org/)
  })

  it('expone un mensaje de error de carga cartográfica', () => {
    assert.ok(TILE_ERROR_MESSAGE.length > 0)
    assert.match(TILE_ERROR_MESSAGE, /cartografía/i)
  })
})

describe('Validación de coordenadas y marcadores', () => {
  it('acepta coordenadas válidas y rechaza valores fuera de rango o no numéricos', () => {
    assert.equal(esCoordenadaValida(-38.7359, -72.5904), true)
    assert.equal(esCoordenadaValida(0, 0), true)

    assert.equal(esCoordenadaValida(91, 0), false)
    assert.equal(esCoordenadaValida(-91, 0), false)
    assert.equal(esCoordenadaValida(0, 181), false)
    assert.equal(esCoordenadaValida(0, -181), false)
    assert.equal(esCoordenadaValida(Number.NaN, 0), false)
    assert.equal(esCoordenadaValida(0, Number.POSITIVE_INFINITY), false)
    assert.equal(esCoordenadaValida('-38.7', '-72.5'), false)
    assert.equal(esCoordenadaValida(null, undefined), false)
  })

  it('descarta marcadores inválidos y duplicados conservando el orden', () => {
    const marcadores: IncidentMapMarker[] = [
      marcadorBase,
      { ...marcadorBase, id: 'INC-2', latitud: 120 },
      { ...marcadorBase, id: '   ' },
      { ...marcadorBase, id: 'INC-3', longitud: Number.NaN },
      { ...marcadorBase, id: 'INC-4', titulo: 'Corte de servicio', prioridad: 2 },
      { ...marcadorBase, titulo: 'Duplicado de INC-1' },
    ]

    const resultado = normalizarMarcadores(marcadores)

    assert.deepEqual(
      resultado.map((marcador) => marcador.id),
      ['INC-1', 'INC-4'],
    )
    assert.equal(resultado[1].titulo, 'Corte de servicio')
  })

  it('tolera entradas nulas o no definidas', () => {
    assert.deepEqual(normalizarMarcadores(undefined), [])
    assert.deepEqual(normalizarMarcadores(null), [])
    assert.deepEqual(normalizarMarcadores([]), [])
  })
})

describe('Colores por prioridad de los marcadores', () => {
  it('asigna un color por nivel y un respaldo para valores desconocidos', () => {
    assert.equal(colorPorPrioridad(1), '#dc2626')
    assert.equal(colorPorPrioridad(2), '#f97316')
    assert.equal(colorPorPrioridad(3), '#1d4ed8')
    assert.equal(colorPorPrioridad(4), DEFAULT_MARKER_COLOR)
    assert.equal(colorPorPrioridad(null), DEFAULT_MARKER_COLOR)
    assert.equal(colorPorPrioridad(undefined), DEFAULT_MARKER_COLOR)
    assert.equal(colorPorPrioridad(Number.NaN), DEFAULT_MARKER_COLOR)
  })
})

describe('Integración del mapa en la navegación autenticada', () => {
  it('registra /mapa para CIUDADANO y ADMIN con etiqueta de incidentes', () => {
    const regla = NAVIGATION_RULES.find((item) => item.path === '/mapa')

    assert.ok(regla)
    assert.equal(regla.label, 'Mapa de incidentes')
    assert.deepEqual([...regla.allowedRoles].sort(), [ROLES.ADMIN, ROLES.CIUDADANO].sort())
  })

  it('muestra el mapa a ambos roles en la navegación principal', () => {
    for (const rol of [ROLES.CIUDADANO, ROLES.ADMIN]) {
      const rutas = getVisibleNavigation([rol], 'main').map((item) => item.path)
      assert.ok(rutas.includes('/mapa'), `El rol ${rol} debería ver /mapa`)
    }
  })
})
