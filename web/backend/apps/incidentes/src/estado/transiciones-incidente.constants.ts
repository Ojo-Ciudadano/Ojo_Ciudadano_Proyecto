/**
 * Reglas de cambio de estado de incidentes definidas en la Issue #116.
 * La fuente de verdad funcional es docs/MATRIZ_TRANSICIONES_INCIDENTES.md.
 *
 * Los estados se identifican por su nombre canonico (no por UUID), porque los
 * identificadores de la tabla `estado` pueden variar entre ambientes.
 */
export const ESTADOS_INCIDENTE = [
  'Nuevo',
  'En revisión',
  'En atención',
  'Resuelto',
  'Cerrado',
] as const;

export type EstadoIncidente = (typeof ESTADOS_INCIDENTE)[number];

export const TRANSICIONES_PERMITIDAS: Readonly<
  Record<EstadoIncidente, readonly EstadoIncidente[]>
> = {
  Nuevo: ['En revisión'],
  'En revisión': ['En atención'],
  'En atención': ['Resuelto'],
  Resuelto: ['En atención', 'Cerrado'],
  Cerrado: [],
};

export function esEstadoIncidente(valor: unknown): valor is EstadoIncidente {
  return (
    typeof valor === 'string' &&
    (ESTADOS_INCIDENTE as readonly string[]).includes(valor)
  );
}

export function esTransicionPermitida(
  origen: unknown,
  destino: unknown,
): boolean {
  if (!esEstadoIncidente(origen) || !esEstadoIncidente(destino)) {
    return false;
  }

  return TRANSICIONES_PERMITIDAS[origen].includes(destino);
}
