import {
  esCoordenadaValida,
  normalizarMarcadores,
  type IncidentMapMarker,
} from './mapConfig'
import { normalizarFiltrosMapa } from '../../services/incidentes.service'
import type {
  FiltrosMapaIncidentes,
  Incidente,
  OpcionCatalogoMapa,
} from '../../types/incidente'

export interface ErrorMapa {
  mensaje: string
  permiteReintentar: boolean
}

export const FILTROS_MAPA_INICIALES: FiltrosMapaIncidentes = {
  estadoId: '',
  categoriaId: '',
}

export interface ConsultaMapaActiva {
  readonly id: number
  readonly signal: AbortSignal
  esVigente: () => boolean
  cancelar: () => void
}

function textoOpcional(valor: unknown): string | undefined {
  if (typeof valor !== 'string') {
    return undefined
  }

  const texto = valor.trim()
  return texto === '' ? undefined : texto
}

function numeroOpcional(valor: unknown): number | undefined {
  if (typeof valor === 'number') {
    return Number.isFinite(valor) ? valor : undefined
  }

  if (typeof valor === 'string' && valor.trim() !== '') {
    const numero = Number(valor)
    return Number.isFinite(numero) ? numero : undefined
  }

  return undefined
}

/**
 * Convierte un incidente de la API en un marcador del mapa.
 *
 * Devuelve `null` cuando el incidente no tiene `id` o cuando sus coordenadas
 * son ausentes, no numéricas o están fuera de rango. Un incidente inválido no
 * interrumpe el resto del listado.
 */
export function mapearIncidenteAMarcador(
  incidente: Incidente | null | undefined,
): IncidentMapMarker | null {
  if (!incidente || typeof incidente.id !== 'string' || incidente.id.trim() === '') {
    return null
  }

  const ubicacion = incidente.ubicacion
  if (!ubicacion) {
    return null
  }

  const latitud = numeroOpcional(ubicacion.latitud)
  const longitud = numeroOpcional(ubicacion.longitud)

  if (!esCoordenadaValida(latitud, longitud)) {
    return null
  }

  const titulo =
    textoOpcional(incidente.titulo) ??
    textoOpcional(ubicacion.direccionReferencia) ??
    `Incidente ${incidente.id}`

  const descripcion =
    textoOpcional(incidente.descripcion) ??
    textoOpcional(ubicacion.direccionReferencia) ??
    null

  return {
    id: incidente.id,
    latitud: latitud as number,
    longitud: longitud as number,
    titulo,
    descripcion,
    prioridad:
      typeof incidente.nivelPrioridad === 'number'
        ? incidente.nivelPrioridad
        : null,
    categoria: textoOpcional(incidente.categoria?.nombre),
    estado: textoOpcional(incidente.estado?.nombre),
    fechaCreacion: textoOpcional(incidente.fechaCreacion),
  }
}

/**
 * Normaliza un listado completo de incidentes en marcadores listos para
 * `IncidentMap`. Descarta inválidos y duplicados por `id`, conservando el
 * orden entregado por la API.
 */
export function mapearIncidentesAMarcadores(
  incidentes?: readonly Incidente[] | null,
): IncidentMapMarker[] {
  if (!Array.isArray(incidentes)) {
    return []
  }

  const marcadores: IncidentMapMarker[] = []

  for (const incidente of incidentes) {
    const marcador = mapearIncidenteAMarcador(incidente)
    if (marcador) {
      marcadores.push(marcador)
    }
  }

  return normalizarMarcadores(marcadores)
}

/**
 * Extrae las opciones únicas y válidas de un catálogo (`estado` o `categoria`)
 * a partir de los incidentes cargados (#143).
 */
export function obtenerOpcionesFiltroMapa(
  incidentes: readonly Incidente[] | null | undefined,
  campo: 'estado' | 'categoria',
): OpcionCatalogoMapa[] {
  if (!Array.isArray(incidentes)) {
    return []
  }

  const opciones = new Map<string, string>()

  for (const incidente of incidentes) {
    if (!incidente) {
      continue
    }

    const relacion = incidente[campo]
    const idCrudo =
      relacion?.id ??
      (campo === 'estado' ? incidente.estadoId : incidente.categoriaId)

    const id = textoOpcional(idCrudo)
    const nombre = textoOpcional(relacion?.nombre)

    if (id && nombre) {
      opciones.set(id, nombre)
    }
  }

  return Array.from(opciones, ([id, nombre]) => ({ id, nombre })).sort((a, b) =>
    a.nombre.localeCompare(b.nombre, 'es'),
  )
}

/**
 * Conserva la colección de referencia para los selectores de estado y categoría
 * aunque una consulta filtrada devuelva un subconjunto o cero resultados (#143).
 */
export function actualizarColeccionCatalogoMapa(
  catalogoActual: readonly Incidente[],
  nuevosIncidentes: readonly Incidente[],
  sinFiltrosActivos: boolean,
): Incidente[] {
  if (sinFiltrosActivos) {
    return [...nuevosIncidentes]
  }

  if (catalogoActual.length === 0) {
    return [...nuevosIncidentes]
  }

  const idsExistentes = new Set(catalogoActual.map((item) => item.id))
  const combinados = [...catalogoActual]

  for (const incidente of nuevosIncidentes) {
    if (incidente?.id && !idsExistentes.has(incidente.id)) {
      idsExistentes.add(incidente.id)
      combinados.push(incidente)
    }
  }

  return combinados
}

/** Indica si existe al menos un filtro de estado o categoría activo. */
export function tieneFiltrosMapaActivos(
  filtros?: Partial<FiltrosMapaIncidentes> | null,
): boolean {
  const { estadoId, categoriaId } = normalizarFiltrosMapa(filtros)
  return Boolean(estadoId || categoriaId)
}

/** Devuelve una copia limpia de los filtros iniciales del mapa. */
export function restablecerFiltrosMapa(): FiltrosMapaIncidentes {
  return { ...FILTROS_MAPA_INICIALES }
}

/**
 * Conserva el identificador del incidente con popup abierto únicamente si su
 * marcador continúa presente tras actualizar los resultados filtrados (#143).
 */
export function sincronizarIncidenteSeleccionado(
  incidenteSeleccionadoId: string | null | undefined,
  marcadores: readonly IncidentMapMarker[] | null | undefined,
): string | null {
  const id = textoOpcional(incidenteSeleccionadoId)
  if (!id) {
    return null
  }

  const visibles = normalizarMarcadores(marcadores)
  const sigueVisible = visibles.some((marcador) => marcador.id === id)

  return sigueVisible ? id : null
}

/**
 * Determina si debe cerrarse el popup actual porque su incidente dejó de estar
 * representado en los marcadores visibles (#143).
 */
export function debeCerrarPopupMapa(
  incidenteSeleccionadoId: string | null | undefined,
  marcadores: readonly IncidentMapMarker[] | null | undefined,
): boolean {
  const id = textoOpcional(incidenteSeleccionadoId)
  if (!id) {
    return false
  }

  return sincronizarIncidenteSeleccionado(id, marcadores) === null
}

/**
 * Coordina solicitudes concurrentes de filtrado en el mapa para cancelar
 * consultas previas e impedir que respuestas tardías sobrescriban una selección
 * más reciente (#143).
 */
export function crearCoordinadorConsultasMapa() {
  let secuencia = 0
  let controladorActual: AbortController | null = null

  return {
    iniciarConsulta(): ConsultaMapaActiva {
      controladorActual?.abort()

      secuencia += 1
      const id = secuencia
      const controller = new AbortController()
      controladorActual = controller

      return {
        id,
        signal: controller.signal,
        esVigente: () => !controller.signal.aborted && secuencia === id,
        cancelar: () => {
          controller.abort()
        },
      }
    },
    cancelarActual(): void {
      controladorActual?.abort()
      controladorActual = null
    },
  }
}

function extraerStatus(error: unknown): number | undefined {
  if (typeof error !== 'object' || error === null) {
    return undefined
  }

  const candidato = error as {
    statusCode?: unknown
    status?: unknown
    response?: { status?: unknown } | null
  }

  for (const valor of [
    candidato.statusCode,
    candidato.response?.status,
    candidato.status,
  ]) {
    const numero = Number(valor)
    if (Number.isFinite(numero)) {
      return numero
    }
  }

  return undefined
}

/** Traduce errores de la consulta de incidentes a un mensaje para la pantalla. */
export function interpretarErrorMapa(error: unknown): ErrorMapa {
  const statusCode = extraerStatus(error)

  if (statusCode === 401) {
    return {
      mensaje:
        'Tu sesión no es válida. Inicia sesión nuevamente para ver el mapa.',
      permiteReintentar: false,
    }
  }

  if (statusCode === 403) {
    return {
      mensaje: 'No tienes permisos para consultar los incidentes del mapa.',
      permiteReintentar: false,
    }
  }

  return {
    mensaje:
      'No se pudieron cargar los incidentes georreferenciados. Inténtalo nuevamente.',
    permiteReintentar: true,
  }
}
