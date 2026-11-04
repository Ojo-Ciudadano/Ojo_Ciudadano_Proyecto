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
  FILTROS_MAPA_INICIALES,
  actualizarColeccionCatalogoMapa,
  crearCoordinadorConsultasMapa,
  debeCerrarPopupMapa,
  interpretarErrorMapa,
  mapearIncidenteAMarcador,
  mapearIncidentesAMarcadores,
  obtenerOpcionesFiltroMapa,
  restablecerFiltrosMapa,
  sincronizarIncidenteSeleccionado,
  tieneFiltrosMapaActivos,
} from '../components/map/incidentMarkers'
import {
  normalizarFiltrosMapa,
  normalizarIncidentes,
  obtenerIncidentesParaMapa,
} from '../services/incidentes.service'
import { apiClient } from '../services/api'
import { NAVIGATION_RULES, ROLES, getVisibleNavigation } from '../config/navigation'
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

const ESTADO_NUEVO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14'
const ESTADO_ATENCION_ID = 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15'
const CATEGORIA_INCENDIO_ID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
const CATEGORIA_ALUMBRADO_ID = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16'

const incidenteBase: Incidente = {
  id: 'INC-1',
  titulo: 'Incendio forestal',
  descripcion: 'Humo visible en el cerro',
  nivelPrioridad: 1,
  estadoId: ESTADO_NUEVO_ID,
  categoriaId: CATEGORIA_INCENDIO_ID,
  estado: {
    id: ESTADO_NUEVO_ID,
    nombre: 'Nuevo',
  },
  categoria: {
    id: CATEGORIA_INCENDIO_ID,
    nombre: 'Incendio forestal',
  },
  ubicacion: {
    latitud: -38.7359,
    longitud: -72.5904,
    comuna: 'Temuco',
    region: 'La Araucanía',
  },
}

const incidenteSecundario: Incidente = {
  id: 'INC-2',
  titulo: 'Luminaria apagada en Av. Alemania',
  descripcion: 'Poste sin suministro eléctrico',
  nivelPrioridad: 3,
  estadoId: ESTADO_ATENCION_ID,
  categoriaId: CATEGORIA_ALUMBRADO_ID,
  estado: {
    id: ESTADO_ATENCION_ID,
    nombre: 'En atención',
  },
  categoria: {
    id: CATEGORIA_ALUMBRADO_ID,
    nombre: 'Alumbrado público',
  },
  ubicacion: {
    latitud: -38.74,
    longitud: -72.6,
    comuna: 'Temuco',
    region: 'La Araucanía',
  },
}

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

describe('Mapeo de incidentes a marcadores (#140)', () => {
  it('asocia el id del incidente y conserva sus datos de presentación', () => {
    const marcador = mapearIncidenteAMarcador(incidenteBase)

    assert.ok(marcador)
    assert.equal(marcador.id, 'INC-1')
    assert.equal(marcador.latitud, -38.7359)
    assert.equal(marcador.longitud, -72.5904)
    assert.equal(marcador.titulo, 'Incendio forestal')
    assert.equal(marcador.descripcion, 'Humo visible en el cerro')
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

describe('Filtros por estado y categoría en el mapa Web (#143)', () => {
  it('extrae valores e identificadores válidos de los catálogos existentes sin duplicados', () => {
    const listado: Incidente[] = [
      incidenteBase,
      incidenteSecundario,
      { ...incidenteBase, id: 'INC-DUPLICADO' },
      {
        ...incidenteBase,
        id: 'INC-SIN-CATALOGO',
        estadoId: '',
        categoriaId: '',
        estado: { id: '   ', nombre: '   ' },
        categoria: null,
      },
    ]

    const estados = obtenerOpcionesFiltroMapa(listado, 'estado')
    const categorias = obtenerOpcionesFiltroMapa(listado, 'categoria')

    assert.deepEqual(estados, [
      { id: ESTADO_ATENCION_ID, nombre: 'En atención' },
      { id: ESTADO_NUEVO_ID, nombre: 'Nuevo' },
    ])

    assert.deepEqual(categorias, [
      { id: CATEGORIA_ALUMBRADO_ID, nombre: 'Alumbrado público' },
      { id: CATEGORIA_INCENDIO_ID, nombre: 'Incendio forestal' },
    ])
  })

  it('conserva las opciones del catálogo inicial cuando una consulta filtrada retorna menos o ningún resultado', () => {
    const catalogoInicial = actualizarColeccionCatalogoMapa(
      [],
      [incidenteBase, incidenteSecundario],
      true,
    )
    const trasFiltroVacio = actualizarColeccionCatalogoMapa(
      catalogoInicial,
      [],
      false,
    )

    assert.equal(
      obtenerOpcionesFiltroMapa(trasFiltroVacio, 'estado').length,
      2,
    )
    assert.equal(
      obtenerOpcionesFiltroMapa(trasFiltroVacio, 'categoria').length,
      2,
    )
  })

  it('normaliza, detecta y restablece los filtros de estado y categoría', () => {
    assert.equal(tieneFiltrosMapaActivos(FILTROS_MAPA_INICIALES), false)
    assert.equal(
      tieneFiltrosMapaActivos({ estadoId: '  ', categoriaId: '' }),
      false,
    )

    const normalizados = normalizarFiltrosMapa({
      estadoId: `  ${ESTADO_NUEVO_ID}  `,
      categoriaId: ` ${CATEGORIA_INCENDIO_ID} `,
    })

    assert.deepEqual(normalizados, {
      estadoId: ESTADO_NUEVO_ID,
      categoriaId: CATEGORIA_INCENDIO_ID,
    })
    assert.equal(tieneFiltrosMapaActivos(normalizados), true)
    assert.deepEqual(restablecerFiltrosMapa(), FILTROS_MAPA_INICIALES)
  })

  it('consulta GET /incidentes con filtro individual por estado, por categoría, combinados y al restablecer', async () => {
    const adapterOriginal = apiClient.defaults.adapter
    const parametrosRecibidos: Record<string, string>[] = []

    const catalogoCompleto: Incidente[] = [
      incidenteBase,
      incidenteSecundario,
      {
        ...incidenteBase,
        id: 'INC-COORD-INVALIDA',
        ubicacion: { latitud: 150, longitud: -72.59 },
      },
    ]

    apiClient.defaults.adapter = async (config) => {
      const params = (config.params ?? {}) as Record<string, string>
      parametrosRecibidos.push({ ...params })

      const filtrados = catalogoCompleto.filter((inc) => {
        const cumpleEstado = !params.estadoId || inc.estadoId === params.estadoId
        const cumpleCategoria =
          !params.categoriaId || inc.categoriaId === params.categoriaId
        return cumpleEstado && cumpleCategoria
      })

      return {
        data: filtrados,
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }
    }

    try {
      // 1. Filtro individual por estado (excluye coordenadas inválidas)
      const porEstado = await obtenerIncidentesParaMapa(undefined, {
        estadoId: ESTADO_NUEVO_ID,
      })
      assert.deepEqual(parametrosRecibidos[0], { estadoId: ESTADO_NUEVO_ID })
      assert.deepEqual(
        mapearIncidentesAMarcadores(porEstado).map((m) => m.id),
        ['INC-1'],
      )

      // 2. Filtro individual por categoría
      const porCategoria = await obtenerIncidentesParaMapa(undefined, {
        categoriaId: CATEGORIA_ALUMBRADO_ID,
      })
      assert.deepEqual(parametrosRecibidos[1], {
        categoriaId: CATEGORIA_ALUMBRADO_ID,
      })
      assert.deepEqual(
        mapearIncidentesAMarcadores(porCategoria).map((m) => m.id),
        ['INC-2'],
      )

      // 3. Filtros combinados simultáneos (estado + categoría)
      const combinados = await obtenerIncidentesParaMapa(undefined, {
        estadoId: ESTADO_ATENCION_ID,
        categoriaId: CATEGORIA_ALUMBRADO_ID,
      })
      assert.deepEqual(parametrosRecibidos[2], {
        estadoId: ESTADO_ATENCION_ID,
        categoriaId: CATEGORIA_ALUMBRADO_ID,
      })
      assert.deepEqual(
        mapearIncidentesAMarcadores(combinados).map((m) => m.id),
        ['INC-2'],
      )

      // 4. Restablecer filtros recupera todos los incidentes georreferenciados válidos
      const restablecidos = await obtenerIncidentesParaMapa(
        undefined,
        restablecerFiltrosMapa(),
      )
      assert.deepEqual(parametrosRecibidos[3], {})
      assert.deepEqual(
        mapearIncidentesAMarcadores(restablecidos).map((m) => m.id),
        ['INC-1', 'INC-2'],
      )
    } finally {
      apiClient.defaults.adapter = adapterOriginal
    }
  })

  it('evita que una respuesta desactualizada sobrescriba los resultados de una selección más reciente', async () => {
    const adapterOriginal = apiClient.defaults.adapter
    const coordinador = crearCoordinadorConsultasMapa()

    let resolverPrimeraConsulta!: (datos: Incidente[]) => void
    let notificarPrimeraEnVuelo!: () => void

    const primeraSolicitudEnVuelo = new Promise<void>((resolve) => {
      notificarPrimeraEnVuelo = resolve
    })

    apiClient.defaults.adapter = (config) => {
      const params = (config.params ?? {}) as Record<string, string>

      if (params.estadoId === ESTADO_NUEVO_ID) {
        notificarPrimeraEnVuelo()
        return new Promise((resolve) => {
          resolverPrimeraConsulta = (datos) => {
            resolve({
              data: datos,
              status: 200,
              statusText: 'OK',
              headers: {},
              config,
            })
          }
        })
      }

      return Promise.resolve({
        data: [incidenteSecundario],
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      })
    }

    try {
      let marcadoresEnPantalla: IncidentMapMarker[] = []

      // 1. Inicia una primera consulta lenta (Estado = Nuevo) y espera a que esté en vuelo
      const consultaAnterior = coordinador.iniciarConsulta()
      const promesaAnterior = obtenerIncidentesParaMapa(consultaAnterior.signal, {
        estadoId: ESTADO_NUEVO_ID,
      })
        .then((datos) => {
          if (consultaAnterior.esVigente()) {
            marcadoresEnPantalla = mapearIncidentesAMarcadores(datos)
          }
        })
        .catch(() => {
          // Si el adaptador rechaza por aborto, se ignora sin tocar el estado
        })

      await primeraSolicitudEnVuelo

      // 2. El usuario cambia el filtro antes de que responda la primera solicitud
      const consultaReciente = coordinador.iniciarConsulta()
      assert.equal(consultaAnterior.signal.aborted, true)
      assert.equal(consultaAnterior.esVigente(), false)
      assert.equal(consultaReciente.esVigente(), true)

      const datosRecientes = await obtenerIncidentesParaMapa(
        consultaReciente.signal,
        { estadoId: ESTADO_ATENCION_ID },
      )
      if (consultaReciente.esVigente()) {
        marcadoresEnPantalla = mapearIncidentesAMarcadores(datosRecientes)
      }

      // 3. La primera consulta responde tardíamente, pero no reemplaza la selección reciente
      resolverPrimeraConsulta([incidenteBase])
      await promesaAnterior

      assert.deepEqual(
        marcadoresEnPantalla.map((m) => m.id),
        ['INC-2'],
      )
    } finally {
      apiClient.defaults.adapter = adapterOriginal
    }
  })

  it('cierra el popup seleccionado si su marcador desaparece por los filtros y lo conserva si permanece visible', () => {
    const marcadoresIniciales = mapearIncidentesAMarcadores([
      incidenteBase,
      incidenteSecundario,
    ])
    const marcadoresFiltrados = mapearIncidentesAMarcadores([
      incidenteSecundario,
    ])

    // Inicialmente INC-1 tiene el popup abierto y está presente
    assert.equal(debeCerrarPopupMapa('INC-1', marcadoresIniciales), false)
    assert.equal(
      sincronizarIncidenteSeleccionado('INC-1', marcadoresIniciales),
      'INC-1',
    )

    // Al filtrar, INC-1 desaparece: debe cerrarse el popup
    assert.equal(debeCerrarPopupMapa('INC-1', marcadoresFiltrados), true)
    assert.equal(
      sincronizarIncidenteSeleccionado('INC-1', marcadoresFiltrados),
      null,
    )

    // Si el popup abierto era INC-2 y sigue en los resultados filtrados, se conserva
    assert.equal(debeCerrarPopupMapa('INC-2', marcadoresFiltrados), false)
    assert.equal(
      sincronizarIncidenteSeleccionado('INC-2', marcadoresFiltrados),
      'INC-2',
    )
  })
})