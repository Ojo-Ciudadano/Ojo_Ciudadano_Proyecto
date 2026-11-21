export type VarianteEstadoIncidente =
  | 'nuevo'
  | 'asignado'
  | 'atencion'
  | 'cerrado'
  | 'rechazado'
  | 'otro'

export type VariantePrioridadIncidente =
  | 'critica'
  | 'alta'
  | 'media'
  | 'moderada'
  | 'baja'
  | 'desconocida'

export function normalizarTextoIndicador(valor?: string | null): string {
  return (valor ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
}

export function obtenerVarianteEstado(
  nombre?: string | null,
): VarianteEstadoIncidente {
  switch (normalizarTextoIndicador(nombre)) {
    case 'nuevo':
    case 'en revision':
      return 'nuevo'
    case 'validado':
    case 'asignado':
      return 'asignado'
    case 'en atencion':
      return 'atencion'
    case 'resuelto':
    case 'cerrado':
      return 'cerrado'
    case 'rechazado':
      return 'rechazado'
    default:
      return 'otro'
  }
}

export function obtenerTextoEstado(nombre?: string | null): string {
  return nombre?.trim() || 'Sin información'
}

export function obtenerVariantePrioridad(
  nivel?: number | null,
): VariantePrioridadIncidente {
  switch (nivel) {
    case 5:
      return 'critica'
    case 4:
      return 'alta'
    case 3:
      return 'media'
    case 2:
      return 'moderada'
    case 1:
      return 'baja'
    default:
      return 'desconocida'
  }
}

export function obtenerTextoPrioridad(nivel?: number | null): string {
  return typeof nivel === 'number' && Number.isFinite(nivel)
    ? `Nivel ${nivel}`
    : 'Sin prioridad'
}
