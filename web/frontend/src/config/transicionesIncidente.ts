/**
 * Transiciones definidas en:
 * docs/MATRIZ_TRANSICIONES_INCIDENTES.md — Issue #116.
 *
 * El backend valida nuevamente cada transición.
 */
export const ESTADOS_INCIDENTE = [
  'Nuevo',
  'En revisión',
  'En atención',
  'Resuelto',
  'Cerrado',
] as const

export type EstadoIncidente = (typeof ESTADOS_INCIDENTE)[number]

const TRANSICIONES_PERMITIDAS: Readonly<
  Record<EstadoIncidente, readonly EstadoIncidente[]>
> = {
  Nuevo: ['En revisión'],
  'En revisión': ['En atención'],
  'En atención': ['Resuelto'],
  Resuelto: ['En atención', 'Cerrado'],
  Cerrado: [],
}

export function esEstadoIncidente(
  valor: unknown,
): valor is EstadoIncidente {
  return (
    typeof valor === 'string' &&
    (ESTADOS_INCIDENTE as readonly string[]).includes(valor)
  )
}

export function obtenerTransicionesPermitidas(
  estadoActual: unknown,
): readonly EstadoIncidente[] {
  if (!esEstadoIncidente(estadoActual)) {
    return []
  }

  return TRANSICIONES_PERMITIDAS[estadoActual]
}