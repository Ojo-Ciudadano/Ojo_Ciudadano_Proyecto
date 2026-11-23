export interface FiltrosBarraIncidentes {
  ciudad: string
  comuna: string
  estadoId: string
  categoriaId: string
}

export interface OpcionFiltroIncidente {
  id: string
  nombre: string
}

export interface OpcionCiudadComunas {
  ciudad: string
  comunas: readonly string[]
}

export const FILTROS_BARRA_INICIALES: Readonly<FiltrosBarraIncidentes> = {
  ciudad: '',
  comuna: '',
  estadoId: '',
  categoriaId: '',
}

export function crearFiltrosBarraVacios(): FiltrosBarraIncidentes {
  return {
    ciudad: '',
    comuna: '',
    estadoId: '',
    categoriaId: '',
  }
}

function normalizarClave(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ')
}

function deduplicarYOrdenar(valores: Iterable<string>): string[] {
  const mapa = new Map<string, string>()

  for (const valor of valores) {
    const limpio = valor.trim()
    if (!limpio) {
      continue
    }

    const clave = normalizarClave(limpio)
    if (!mapa.has(clave)) {
      mapa.set(clave, limpio)
    }
  }

  return Array.from(mapa.values()).sort((a, b) =>
    a.localeCompare(b, 'es', { sensitivity: 'base' }),
  )
}

export function obtenerCiudadesDisponibles(
  ciudades: readonly OpcionCiudadComunas[],
): string[] {
  return deduplicarYOrdenar(ciudades.map((item) => item.ciudad))
}

export function obtenerComunasDisponibles(
  ciudades: readonly OpcionCiudadComunas[],
  ciudadSeleccionada: string,
): string[] {
  const claveCiudad = normalizarClave(ciudadSeleccionada)

  if (!claveCiudad) {
    const todas: string[] = []
    for (const item of ciudades) {
      for (const comuna of item.comunas) {
        todas.push(comuna)
      }
    }
    return deduplicarYOrdenar(todas)
  }

  const coincidencia = ciudades.find(
    (item) => normalizarClave(item.ciudad) === claveCiudad,
  )

  if (!coincidencia) {
    return []
  }

  return deduplicarYOrdenar(coincidencia.comunas)
}

export function resolverCambioCiudad(
  filtros: FiltrosBarraIncidentes,
  nuevaCiudad: string,
  ciudades: readonly OpcionCiudadComunas[],
): FiltrosBarraIncidentes {
  const comunasValidas = obtenerComunasDisponibles(ciudades, nuevaCiudad)
  const claveComunaActual = normalizarClave(filtros.comuna)

  const comunaCoincidente = claveComunaActual
    ? comunasValidas.find(
        (comuna) => normalizarClave(comuna) === claveComunaActual,
      )
    : undefined

  return {
    ...filtros,
    ciudad: nuevaCiudad,
    comuna: comunaCoincidente ?? '',
  }
}

export function contarFiltrosActivos(
  filtros: FiltrosBarraIncidentes,
): number {
  let total = 0

  if (filtros.ciudad.trim()) total += 1
  if (filtros.comuna.trim()) total += 1
  if (filtros.estadoId.trim()) total += 1
  if (filtros.categoriaId.trim()) total += 1

  return total
}

export function textoContadorFiltros(cantidad: number): string {
  if (cantidad <= 0) {
    return 'Sin filtros activos'
  }

  if (cantidad === 1) {
    return '1 filtro activo'
  }

  return `${cantidad} filtros activos`
}
