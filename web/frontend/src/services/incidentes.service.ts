import { apiClient } from './api'
import type {
  FiltrosIncidentes,
  Incidente,
} from '../types/incidente'

/** Campos donde un backend puede anidar el listado de incidentes. */
const CAMPOS_LISTADO = [
  'incidentes',
  'items',
  'results',
  'content',
  'registros',
] as const

/** Campos habituales para metadatos de paginación. */
const CAMPOS_PAGINA = ['page', 'pagina', 'currentPage', 'paginaActual'] as const
const CAMPOS_TOTAL_PAGINAS = [
  'totalPages',
  'totalPaginas',
  'lastPage',
  'ultimaPagina',
  'pages',
] as const
const CAMPOS_TOTAL = [
  'total',
  'totalItems',
  'totalRegistros',
  'count',
] as const

/**
 * Tope defensivo de páginas para el mapa. La API actual devuelve la colección
 * completa en una sola respuesta; el límite evita bucles si un backend futuro
 * ignora los parámetros de paginación.
 */
export const MAX_PAGINAS_MAPA = 20

export interface PaginaIncidentes {
  incidentes: Incidente[]
  pagina?: number
  totalPaginas?: number
  total?: number
}

function esRegistro(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null && !Array.isArray(valor)
}

function leerNumero(
  fuentes: readonly Record<string, unknown>[],
  claves: readonly string[],
): number | undefined {
  for (const fuente of fuentes) {
    for (const clave of claves) {
      const valor = fuente[clave]

      if (typeof valor === 'number' && Number.isFinite(valor)) {
        return valor
      }

      if (
        typeof valor === 'string' &&
        valor.trim() !== '' &&
        Number.isFinite(Number(valor))
      ) {
        return Number(valor)
      }
    }
  }

  return undefined
}

/**
 * Normaliza respuestas de `GET /incidentes`. Acepta tanto el arreglo plano que
 * entrega la API actual como envelopes paginados (`data`, `items`, `results`,
 * `meta.pagination`, etc.). Devuelve `null` cuando no hay un listado válido.
 */
export function extraerIncidentesDeRespuesta(
  payload: unknown,
): PaginaIncidentes | null {
  if (Array.isArray(payload)) {
    return { incidentes: payload as Incidente[] }
  }

  if (!esRegistro(payload)) {
    return null
  }

  const contenedores = [
    payload,
    payload.data,
    payload.meta,
    payload.pagination,
  ].filter(esRegistro)

  let incidentes: Incidente[] | null = null

  for (const contenedor of contenedores) {
    for (const campo of CAMPOS_LISTADO) {
      const valor = contenedor[campo]
      if (Array.isArray(valor)) {
        incidentes = valor as Incidente[]
        break
      }
    }
    if (incidentes) {
      break
    }
  }

  if (!incidentes && Array.isArray(payload.data)) {
    incidentes = payload.data as Incidente[]
  }

  if (!incidentes) {
    return null
  }

  const fuentes: Record<string, unknown>[] = [
    payload,
    payload.data,
    payload.meta,
    payload.pagination,
  ].filter(esRegistro)

  // Algunos backends anidan la paginación dentro de `data` o `meta`.
  for (const fuente of [...fuentes]) {
    for (const clave of ['meta', 'pagination', 'paginacion']) {
      const anidado = fuente[clave]
      if (esRegistro(anidado) && !fuentes.includes(anidado)) {
        fuentes.push(anidado)
      }
    }
  }

  return {
    incidentes,
    pagina: leerNumero(fuentes, CAMPOS_PAGINA),
    totalPaginas: leerNumero(fuentes, CAMPOS_TOTAL_PAGINAS),
    total: leerNumero(fuentes, CAMPOS_TOTAL),
  }
}

/**
 * Obtiene la colección completa de incidentes para el mapa.
 *
 * - Reutiliza el cliente autenticado (`apiClient`) para respetar la sesión.
 * - La primera petición no envía parámetros de paginación: el DTO actual usa
 *   `forbidNonWhitelisted`, por lo que `page`/`limit` provocarían un 400.
 * - Si la respuesta incluye metadatos de paginación, itera las páginas
 *   restantes, consolida y deduplica por `id`.
 * - Se detiene cuando el backend ignora la paginación o no aporta elementos
 *   nuevos, evitando consultas redundantes.
 */
export async function obtenerIncidentesParaMapa(
  signal?: AbortSignal,
): Promise<Incidente[]> {
  const acumulados: Incidente[] = []
  const ids = new Set<string>()

  let pagina = 1
  let totalPaginas = 1

  while (pagina <= totalPaginas && pagina <= MAX_PAGINAS_MAPA) {
    const response = await apiClient.get<unknown>('/incidentes', {
      signal,
      // Solo se pagina cuando una respuesta previa lo indicó.
      params: pagina > 1 ? { page: pagina } : undefined,
    })

    const extraida = extraerIncidentesDeRespuesta(response.data)

    if (!extraida) {
      throw new Error(
        'El servidor devolvió una lista de incidentes inválida.',
      )
    }

    let nuevos = 0

    for (const incidente of extraida.incidentes) {
      if (!incidente || typeof incidente.id !== 'string') {
        continue
      }

      if (ids.has(incidente.id)) {
        continue
      }

      ids.add(incidente.id)
      acumulados.push(incidente)
      nuevos += 1
    }

    const paginasInformadas = extraida.totalPaginas
    const totalInformado = extraida.total

    if (paginasInformadas !== undefined) {
      totalPaginas = paginasInformadas
    } else if (totalInformado !== undefined && extraida.incidentes.length > 0) {
      totalPaginas = Math.ceil(totalInformado / extraida.incidentes.length)
    } else {
      // Respuesta plana o sin metadatos: no hay más páginas que recorrer.
      break
    }

    if (nuevos === 0 || extraida.incidentes.length === 0) {
      break
    }

    pagina += 1
  }

  return acumulados
}

export async function obtenerIncidentes(
  signal?: AbortSignal,
  filtros: Partial<FiltrosIncidentes> = {},
): Promise<Incidente[]> {
  const params: Record<string, string> = {}

  if (filtros.estadoId) {
    params.estadoId = filtros.estadoId
  }

  if (filtros.categoriaId) {
    params.categoriaId = filtros.categoriaId
  }

  const comuna = filtros.comuna?.trim()

  if (comuna) {
    params.comuna = comuna
  }

  const response = await apiClient.get<Incidente[]>('/incidentes', {
    signal,
    params,
  })

  if (!Array.isArray(response.data)) {
    throw new Error(
      'El servidor devolvió una lista de incidentes inválida.',
    )
  }

  return response.data
}
