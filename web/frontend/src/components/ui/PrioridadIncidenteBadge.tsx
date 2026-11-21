import {
  obtenerTextoPrioridad,
  obtenerVariantePrioridad,
} from './incidenteIndicadores.utils'

export interface PrioridadIncidenteBadgeProps {
  nivel?: number | null
  className?: string
}

export function PrioridadIncidenteBadge({
  nivel,
  className,
}: PrioridadIncidenteBadgeProps) {
  const variante = obtenerVariantePrioridad(nivel)
  const texto = obtenerTextoPrioridad(nivel)
  const clases = className
    ? `ui-priority-indicator ui-priority-indicator--${variante} ${className}`
    : `ui-priority-indicator ui-priority-indicator--${variante}`

  return (
    <span className={clases} data-prioridad-variante={variante}>
      <span className="ui-priority-dot" aria-hidden="true" />
      {texto}
    </span>
  )
}
