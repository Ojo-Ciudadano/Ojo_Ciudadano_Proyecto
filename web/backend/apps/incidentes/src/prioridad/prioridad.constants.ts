/**
 * Escala de prioridad admitida por el dominio de incidentes.
 * Coincide con los valores de `prioridadBase` definidos en el seed de categorias
 * (Aseo = 1 ... Incendio / Accidente de transito = 5).
 */
export const PRIORIDAD_MINIMA = 1;
export const PRIORIDAD_MAXIMA = 5;

export function esPrioridadValida(valor: unknown): valor is number {
  return (
    typeof valor === 'number' &&
    Number.isInteger(valor) &&
    valor >= PRIORIDAD_MINIMA &&
    valor <= PRIORIDAD_MAXIMA
  );
}
