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
import {
  interpretarErrorMapa,
  mapearIncidenteAMarcador,
  mapearIncidentesAMarcadores,
} from '../components/map/incidentMarkers'
import {
  ESTADO_SELECCION_INICIAL,
  ORIGEN_NAVEGACION_MAPA,
  TEXTO_ACCION_DETALLE,
  TEXTO_CATEGORIA_DESCONOCIDA,
  TEXTO_ESTADO_DESCONOCIDO,
  TEXTO_FECHA_DESCONOCIDA,
  construirResumenIncidente,
  construirRutaDetalleIncidente,
  crearContenidoPopup,
  crearContenidoTooltip,
  formatearFechaCreacion,
  reducirSeleccionPopup,
  resolverIncidenteNavegacion,
  type DocumentoPopup,
} from '../components/map/incidentPopup'
import {
  normalizarIncidentes,
  obtenerIncidenteDetalle,
  obtenerIncidentesParaMapa,
} from '../services/incidentes.service'
import { interpretarErrorDetalleIncidente } from '../hooks/useIncidenteDetalle'
import {
  puedeGestionarIncidente,
  resolverEnlaceRetornoDetalle,
} from '../pages/incidentes/detalleIncidenteAcceso'
import { apiClient } from '../services/api'
import {
  INCIDENT_DETAIL_ALLOWED_ROLES,
  NAVIGATION_RULES,
  ROLES,
  getVisibleNavigation,
  isRoleAuthorized,
} from '../config/navigation'
import type { Incidente } from '../types/incidente'

const almacenamiento = new Map<string, string>()

// `apiClient` lee el token desde localStorage; node:test no incluye Storage.
if (typeof globalThis.localStorage === 'undefined') {
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: {
      getItem: (clave: string) => almacenamiento.get(clave) ?? null,
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

const incidenteBase: Incidente = {
  id: 'INC-1',
  titulo: 'Incendio forestal',
  descripcion: 'Humo visible en el cerro',
  fechaCreacion: '2026-11-03T15:12:00.000Z',
  nivelPrioridad: 1,
  categoria: { id: 'CAT-1', nombre: 'Incendios' },
  estado: { id: 'EST-1', nombre: 'Reportado' },
  ubicacion: {
    latitud: -38.7359,
    longitud: -72.5904,
    comuna: 'Temuco',
    region: 'La Araucanía',
  },
}

const marcadorBase: IncidentMapMarker = {
  id: 'INC-1',
  latitud: -38.7359,
  longitud: -72.5904,
  titulo: 'Incendio forestal',
  descripcion: 'Humo visible en el cerro',
  categoria: 'Incendios',
  estado: 'Reportado',
  fechaCreacion: '2026-11-03T15:12:00.000Z',
  prioridad: 1,
}

interface ElementoFalso {
  tagName: string
  className: string
  textContent: string
  atributos: Record<string, string>
  hijos: ElementoFalso[]
  listeners: Record<string, Array<() => void>>
  appendChild(hijo: ElementoFalso): void
  setAttribute(nombre: string, valor: string): void
  addEventListener(evento: string, handler: () => void): void
  click(): void
}

function crearElementoFalso(tagName: string): ElementoFalso {
  const elemento: ElementoFalso = {
    tagName,
    className: '',
    textContent: '',
    atributos: {},
    hijos: [],
    listeners: {},
    appendChild(hijo: ElementoFalso) {
      elemento.hijos.push(hijo)
    },
    setAttribute(nombre: string, valor: string) {
      elemento.atributos[nombre] = valor
    },
    addEventListener(evento: string, handler: () => void) {
      if (!elemento.listeners[evento]) {
        elemento.listeners[evento] = []
      }
      elemento.listeners[evento].push(handler)
    },
    click() {
      for (const handler of elemento.listeners.click ?? []) {
        handler()
      }
    },
  }
  return elemento
}

function crearDocumentoFalso(): DocumentoPopup {
  return {
    createElement(tagName: string): HTMLElement {
      return crearElementoFalso(tagName) as unknown as HTMLElement
    },
  }
}

function comoElementoFalso(elemento: HTMLElement): ElementoFalso {
  return elemento as unknown as ElementoFalso
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

describe('Mapeo de incidentes a marcadores (#140)', () => {
  it('asocia el id del incidente y conserva sus datos de presentación', () => {
    const marcador = mapearIncidenteAMarcador(incidenteBase)

    assert.ok(marcador)
    assert.equal(marcador.id, 'INC-1')
    assert.equal(marcador.latitud, -38.7359)
    assert.equal(marcador.longitud, -72.5904)
    assert.equal(marcador.titulo, 'Incendio forestal')
    assert.equal(marcador.descripcion, 'Humo visible en el cerro')
    assert.equal(marcador.categoria, 'Incendios')
    assert.equal(marcador.estado, 'Reportado')
    assert.equal(marcador.fechaCreacion, '2026-11-03T15:12:00.000Z')
    assert.equal(marcador.prioridad, 1)
  })

  it('excluye coordenadas nulas, ausentes, no numéricas o fuera de rango', () => {
    const invalidos: (Incidente | null)[] = [
      null,
      { ...incidenteBase, id: 'SIN-UBICACION', ubicacion: null },
      { ...incidenteBase, id: 'SIN-LATITUD', ubicacion: { longitud: -72.5904 } },
      { ...incidenteBase, id: 'NULAS', ubicacion: { latitud: null, longitud: null } },
      { ...incidenteBase, id: 'FUERA-RANGO', ubicacion: { latitud: 120, longitud: -72.5904 } },
      { ...incidenteBase, id: 'TEXTO', ubicacion: { latitud: 'abc' as unknown as number, longitud: -72.5904 } },
      { ...incidenteBase, id: 'NAN', ubicacion: { latitud: Number.NaN, longitud: -72.5904 } },
      { ...incidenteBase, id: 'INFINITO', ubicacion: { latitud: -38.7359, longitud: Number.POSITIVE_INFINITY } },
    ]

    for (const incidente of invalidos) {
      assert.equal(mapearIncidenteAMarcador(incidente), null)
    }
  })

  it('filtra inválidos sin romper los válidos y deduplica por id', () => {
    const incidentes: Incidente[] = [
      incidenteBase,
      { ...incidenteBase, id: 'INC-2', ubicacion: { latitud: 91, longitud: 0 } },
      {
        ...incidenteBase,
        id: 'INC-3',
        titulo: 'Corte de suministro',
        nivelPrioridad: 2,
        ubicacion: {
          latitud: '-38.70' as unknown as number,
          longitud: '-72.60' as unknown as number,
        },
      },
      { ...incidenteBase, id: 'INC-1', titulo: 'Duplicado de INC-1' },
    ]

    const marcadores = mapearIncidentesAMarcadores(incidentes)

    assert.deepEqual(
      marcadores.map((marcador) => marcador.id),
      ['INC-1', 'INC-3'],
    )
    assert.equal(marcadores[1].titulo, 'Corte de suministro')
    assert.equal(marcadores[1].prioridad, 2)
  })

  it('tolera listados nulos y actualiza la capa reemplazando el listado previo', () => {
    assert.deepEqual(mapearIncidentesAMarcadores(undefined), [])
    assert.deepEqual(mapearIncidentesAMarcadores(null), [])

    const inicial = mapearIncidentesAMarcadores([incidenteBase])
    const actualizado = mapearIncidentesAMarcadores([
      { ...incidenteBase, id: 'INC-9', titulo: 'Nuevo reporte' },
    ])

    assert.deepEqual(inicial.map((marcador) => marcador.id), ['INC-1'])
    assert.deepEqual(actualizado.map((marcador) => marcador.id), ['INC-9'])

    // Cada sincronización entrega ids únicos aunque la API repita registros.
    const conDuplicados = mapearIncidentesAMarcadores([
      incidenteBase,
      incidenteBase,
    ])
    assert.equal(conDuplicados.length, 1)
  })

  it('traduce errores de sesión, permisos y fallos generales de la consulta', () => {
    const sesion = interpretarErrorMapa({ statusCode: 401 })
    assert.equal(sesion.permiteReintentar, false)
    assert.match(sesion.mensaje, /sesión/i)

    const permisos = interpretarErrorMapa({ response: { status: 403 } })
    assert.equal(permisos.permiteReintentar, false)
    assert.match(permisos.mensaje, /permisos/i)

    const red = interpretarErrorMapa(new Error('network error'))
    assert.equal(red.permiteReintentar, true)
    assert.match(red.mensaje, /Inténtalo nuevamente/i)
  })
})

describe('Popup resumen del marcador (#141)', () => {
  it('resume título, categoría, estado y fecha de creación', () => {
    const resumen = construirResumenIncidente(marcadorBase)
    assert.equal(resumen.incidentId, 'INC-1')
    assert.equal(resumen.titulo, 'Incendio forestal')
    assert.equal(resumen.categoria, 'Incendios')
    assert.equal(resumen.estado, 'Reportado')
    assert.notEqual(resumen.fechaCreacion, TEXTO_FECHA_DESCONOCIDA)
    assert.match(resumen.fechaCreacion, /2026/)
  })

  it('conserva los campos de resumen en el recorrido incidente -> mapear -> normalizar -> resumen', () => {
    const mapeado = mapearIncidenteAMarcador(incidenteBase)
    assert.ok(mapeado)
    const [normalizado] = normalizarMarcadores([mapeado])
    assert.ok(normalizado)
    const resumen = construirResumenIncidente(normalizado)

    assert.equal(resumen.incidentId, 'INC-1')
    assert.equal(resumen.titulo, 'Incendio forestal')
    assert.equal(resumen.categoria, 'Incendios')
    assert.equal(resumen.estado, 'Reportado')
    assert.notEqual(resumen.fechaCreacion, TEXTO_FECHA_DESCONOCIDA)
  })

  it('usa respaldos legibles cuando faltan datos o la fecha es inválida', () => {
    const resumen = construirResumenIncidente({
      ...marcadorBase,
      titulo: '   ',
      categoria: null,
      estado: '',
      fechaCreacion: 'no-es-fecha',
    })
    assert.equal(resumen.titulo, 'Incidente INC-1')
    assert.equal(resumen.categoria, TEXTO_CATEGORIA_DESCONOCIDA)
    assert.equal(resumen.estado, TEXTO_ESTADO_DESCONOCIDO)
    assert.equal(resumen.fechaCreacion, TEXTO_FECHA_DESCONOCIDA)
    assert.equal(formatearFechaCreacion(null), TEXTO_FECHA_DESCONOCIDA)
    assert.equal(formatearFechaCreacion(undefined), TEXTO_FECHA_DESCONOCIDA)
  })

  it('construye el popup con nodos de texto, conserva el id y expone el botón Ver detalle', () => {
    const documento = crearDocumentoFalso()
    const contenido = comoElementoFalso(
      crearContenidoPopup(marcadorBase, documento),
    )
    assert.equal(contenido.className, 'incident-map__popup')
    assert.equal(contenido.atributos['data-incident-id'], 'INC-1')
    const [titulo, datos, boton] = contenido.hijos
    assert.equal(titulo.tagName, 'strong')
    assert.equal(titulo.textContent, 'Incendio forestal')
    assert.equal(datos.tagName, 'dl')
    assert.deepEqual(
      datos.hijos.map((hijo) => `${hijo.tagName}:${hijo.textContent}`),
      [
        'dt:Categoría',
        'dd:Incendios',
        'dt:Estado',
        'dd:Reportado',
        'dt:Fecha de creación',
        `dd:${construirResumenIncidente(marcadorBase).fechaCreacion}`,
      ],
    )
    assert.equal(boton.tagName, 'button')
    assert.equal(boton.textContent, TEXTO_ACCION_DETALLE)
    assert.equal(boton.atributos['data-incident-id'], 'INC-1')
  })

  it('trata un título con HTML como texto literal en popup y tooltip, sin interpretarlo', () => {
    const documento = crearDocumentoFalso()
    const marcadorXss = {
      ...marcadorBase,
      titulo: '<img src=x onerror=alert(1)>',
    }
    const contenido = comoElementoFalso(
      crearContenidoPopup(marcadorXss, documento),
    )
    const tooltip = comoElementoFalso(
      crearContenidoTooltip(marcadorXss, documento),
    )
    assert.equal(
      contenido.hijos[0].textContent,
      '<img src=x onerror=alert(1)>',
    )
    assert.equal(tooltip.textContent, '<img src=x onerror=alert(1)>')
  })

  it('reemplaza la selección al cambiar de marcador y solo limpia al cerrar el seleccionado', () => {
    let estado = reducirSeleccionPopup(ESTADO_SELECCION_INICIAL, {
      tipo: 'seleccionar',
      incidenteId: 'INC-1',
    })
    assert.equal(estado.incidenteSeleccionadoId, 'INC-1')
    estado = reducirSeleccionPopup(estado, {
      tipo: 'seleccionar',
      incidenteId: 'INC-2',
    })
    assert.equal(estado.incidenteSeleccionadoId, 'INC-2')
    estado = reducirSeleccionPopup(estado, {
      tipo: 'cerrar',
      incidenteId: 'INC-1',
    })
    assert.equal(estado.incidenteSeleccionadoId, 'INC-2')
    estado = reducirSeleccionPopup(estado, {
      tipo: 'cerrar',
      incidenteId: 'INC-2',
    })
    assert.equal(estado.incidenteSeleccionadoId, null)
    estado = reducirSeleccionPopup(
      { incidenteSeleccionadoId: 'INC-3' },
      { tipo: 'limpiar' },
    )
    assert.equal(estado.incidenteSeleccionadoId, null)
  })
})

describe('Navegación desde un marcador al detalle del incidente Web (#142)', () => {
  it('invoca la acción de detalle con el identificador real del marcador seleccionado', () => {
    const documento = crearDocumentoFalso()
    const abiertos: string[] = []

    const contenido = comoElementoFalso(
      crearContenidoPopup(marcadorBase, documento, {
        onVerDetalle: (id) => abiertos.push(id),
      }),
    )

    const boton = contenido.hijos.find((hijo) => hijo.tagName === 'button')
    assert.ok(boton)
    boton.click()

    assert.deepEqual(abiertos, ['INC-1'])
    assert.equal(construirRutaDetalleIncidente(abiertos[0]), '/incidentes/INC-1')
  })

  it('evita abrir un incidente anterior cuando el usuario selecciona varios marcadores antes de navegar', () => {
    let estado = reducirSeleccionPopup(ESTADO_SELECCION_INICIAL, {
      tipo: 'seleccionar',
      incidenteId: 'INC-1',
    })
    estado = reducirSeleccionPopup(estado, {
      tipo: 'seleccionar',
      incidenteId: 'INC-2',
    })
    // Aunque llegue un cierre tardío de INC-1 o un clic residual de INC-1, el destino activo es INC-2
    estado = reducirSeleccionPopup(estado, {
      tipo: 'cerrar',
      incidenteId: 'INC-1',
    })

    const idResuelto = resolverIncidenteNavegacion(estado, 'INC-1')
    assert.equal(idResuelto, 'INC-2')
    assert.equal(construirRutaDetalleIncidente(idResuelto), '/incidentes/INC-2')
  })

  it('rechaza identificadores vacíos o inválidos al construir la ruta del detalle', () => {
    assert.equal(construirRutaDetalleIncidente(''), null)
    assert.equal(construirRutaDetalleIncidente('   '), null)
    assert.equal(construirRutaDetalleIncidente(null), null)
    assert.equal(construirRutaDetalleIncidente(undefined), null)
    assert.equal(
      construirRutaDetalleIncidente('INC/42'),
      '/incidentes/INC%2F42',
    )
  })

  it('autoriza el acceso a la ruta del detalle para CIUDADANO y ADMIN pero restringe las acciones administrativas a ADMIN', () => {
    assert.equal(
      isRoleAuthorized([ROLES.CIUDADANO], INCIDENT_DETAIL_ALLOWED_ROLES),
      true,
    )
    assert.equal(
      isRoleAuthorized([ROLES.ADMIN], INCIDENT_DETAIL_ALLOWED_ROLES),
      true,
    )
    assert.equal(isRoleAuthorized([], INCIDENT_DETAIL_ALLOWED_ROLES), false)

    assert.equal(puedeGestionarIncidente([ROLES.CIUDADANO]), false)
    assert.equal(puedeGestionarIncidente([ROLES.ADMIN]), true)
    assert.equal(puedeGestionarIncidente(null), false)
  })

  it('ofrece retorno al mapa cuando el detalle se abre desde el mapa y conserva el retorno por rol en otros orígenes', () => {
    const desdeMapaCiudadano = resolverEnlaceRetornoDetalle(
      { origen: ORIGEN_NAVEGACION_MAPA },
      [ROLES.CIUDADANO],
    )
    assert.deepEqual(desdeMapaCiudadano, {
      destino: '/mapa',
      etiqueta: '← Volver al mapa',
    })

    const desdeMapaAdmin = resolverEnlaceRetornoDetalle(
      { origen: ORIGEN_NAVEGACION_MAPA },
      [ROLES.ADMIN],
    )
    assert.deepEqual(desdeMapaAdmin, {
      destino: '/mapa',
      etiqueta: '← Volver al mapa',
    })

    const desdeTablaAdmin = resolverEnlaceRetornoDetalle(null, [ROLES.ADMIN])
    assert.deepEqual(desdeTablaAdmin, {
      destino: '/incidentes',
      etiqueta: '← Volver a la tabla de incidentes',
    })
  })

  it('representa de forma controlada acceso denegado (403), inexistente (404) e identificador inválido (400)', () => {
    const denegado = interpretarErrorDetalleIncidente({ statusCode: 403 })
    assert.equal(denegado.noEncontrado, false)
    assert.equal(denegado.error?.accesoDenegado, true)
    assert.equal(denegado.error?.permiteReintentar, false)
    assert.match(denegado.error?.mensaje ?? '', /permisos/i)

    const noEncontrado = interpretarErrorDetalleIncidente({ statusCode: 404 })
    assert.equal(noEncontrado.noEncontrado, true)
    assert.equal(noEncontrado.error, null)

    const idInvalido = interpretarErrorDetalleIncidente({ statusCode: 400 })
    assert.equal(idInvalido.noEncontrado, false)
    assert.equal(idInvalido.error?.permiteReintentar, false)
    assert.match(idInvalido.error?.mensaje ?? '', /identificador/i)
  })

  it('reutiliza obtenerIncidenteDetalle (#133) consultando el incidente elegido por su id real', async () => {
    const adapterOriginal = apiClient.defaults.adapter
    let urlConsultada = ''

    apiClient.defaults.adapter = async (config) => {
      urlConsultada = config.url ?? ''
      return {
        data: {
          ...incidenteBase,
          id: 'INC-1',
          estadoId: 'EST-1',
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }

    try {
      const detalle = await obtenerIncidenteDetalle('INC-1')
      assert.equal(urlConsultada, '/incidentes/INC-1')
      assert.equal(detalle.id, 'INC-1')
      assert.equal(detalle.titulo, 'Incendio forestal')
    } finally {
      apiClient.defaults.adapter = adapterOriginal
    }
  })
})

describe('Normalización de respuestas de incidentes (#140)', () => {
  it('acepta el arreglo plano actual de la API', () => {
    assert.deepEqual(normalizarIncidentes([incidenteBase]), [incidenteBase])
  })

  it('rechaza respuestas que no son un arreglo de incidentes', () => {
    assert.equal(normalizarIncidentes(null), null)
    assert.equal(normalizarIncidentes(undefined), null)
    assert.equal(normalizarIncidentes({ mensaje: 'sin datos' }), null)
    assert.equal(normalizarIncidentes({ data: [incidenteBase] }), null)
    assert.equal(normalizarIncidentes('texto'), null)
  })
})

describe('Consulta de incidentes para el mapa (#140)', () => {
  it('consulta el listado plano una sola vez y sin parámetros de paginación', async () => {
    const adapterOriginal = apiClient.defaults.adapter
    const solicitudes: unknown[] = []

    apiClient.defaults.adapter = async (config) => {
      solicitudes.push(config.params)

      return {
        data: [incidenteBase, { ...incidenteBase, id: 'INC-2' }],
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }

    try {
      const incidentes = await obtenerIncidentesParaMapa()

      assert.deepEqual(
        incidentes.map((incidente) => incidente.id),
        ['INC-1', 'INC-2'],
      )
      assert.equal(solicitudes.length, 1)
      assert.deepEqual(solicitudes[0], {})

      const marcadores = mapearIncidentesAMarcadores(incidentes)
      assert.equal(marcadores.length, 2)
      assert.deepEqual(
        marcadores.map((marcador) => marcador.id),
        ['INC-1', 'INC-2'],
      )
    } finally {
      apiClient.defaults.adapter = adapterOriginal
    }
  })

  it('rechaza respuestas que no son un arreglo de incidentes', async () => {
    const adapterOriginal = apiClient.defaults.adapter

    apiClient.defaults.adapter = async (config) => ({
      data: { items: [incidenteBase] },
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    })

    try {
      await assert.rejects(
        () => obtenerIncidentesParaMapa(),
        /lista de incidentes inválida/i,
      )
    } finally {
      apiClient.defaults.adapter = adapterOriginal
    }
  })

  it('propaga los errores de la API para que la pantalla ofrezca reintento', async () => {
    const adapterOriginal = apiClient.defaults.adapter

    apiClient.defaults.adapter = async (config) => {
      const error = Object.assign(new Error('Fallo interno'), {
        config,
        response: {
          status: 500,
          statusText: 'Internal Server Error',
          headers: {},
          config,
          data: { statusCode: 500, message: 'Fallo interno' },
        },
      })
      throw error
    }

    try {
      await assert.rejects(
        () => obtenerIncidentesParaMapa(),
        (error: unknown) => {
          const info = interpretarErrorMapa(error)
          assert.equal(info.permiteReintentar, true)
          assert.match(info.mensaje, /No se pudieron cargar/i)
          return true
        },
      )
    } finally {
      apiClient.defaults.adapter = adapterOriginal
    }
  })
})
