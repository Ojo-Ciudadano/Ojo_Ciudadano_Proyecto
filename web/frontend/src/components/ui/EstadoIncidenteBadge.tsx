import {
  obtenerTextoEstado,
  obtenerVarianteEstado,
} from './incidenteIndicadores.utils'

export interface EstadoIncidenteBadgeProps {
  nombre?: string | null
  className?: string
}

export function EstadoIncidenteBadge({
  nombre,
  className,
}: EstadoIncidenteBadgeProps) {
  const variante = obtenerVarianteEstado(nombre)
  const texto = obtenerTextoEstado(nombre)
  const clases = className
    ? `ui-status-badge ui-status-badge--${variante} ${className}`
    : `ui-status-badge ui-status-badge--${variante}`

  return (
    <span className={clases} data-estado-variante={variante}>
      {texto}
    </span>
  )
}
