import {
  ORIGEN_NAVEGACION_MAPA,
  type EstadoNavegacionDetalle,
} from '../../components/map/incidentPopup'
import { ROLES } from '../../config/navigation'

export interface EnlaceRetornoDetalle {
  destino: string
  etiqueta: string
}

/**
 * Determina si el usuario autenticado puede ejecutar acciones administrativas
 * (cambio de estado, asignación de responsable y cambio de prioridad) (#142).
 */
export function puedeGestionarIncidente(
  roles?: readonly string[] | null,
): boolean {
  if (!Array.isArray(roles)) {
    return false
  }
  return roles.includes(ROLES.ADMIN)
}

/**
 * Resuelve el destino y texto del enlace de retorno en el detalle (#142).
 * - Si se abrió desde el mapa (`state.origen === 'mapa'`), regresa a `/mapa`.
 * - Si no viene del mapa y es Administrador, regresa a `/incidentes`.
 * - Si es Ciudadano, regresa a `/mapa`.
 */
export function resolverEnlaceRetornoDetalle(
  locationState: unknown,
  roles?: readonly string[] | null,
): EnlaceRetornoDetalle {
  const origen =
    typeof locationState === 'object' && locationState !== null
      ? (locationState as EstadoNavegacionDetalle).origen
      : undefined

  if (origen === ORIGEN_NAVEGACION_MAPA) {
    return {
      destino: '/mapa',
      etiqueta: '← Volver al mapa',
    }
  }

  if (puedeGestionarIncidente(roles)) {
    return {
      destino: '/incidentes',
      etiqueta: '← Volver a la tabla de incidentes',
    }
  }

  return {
    destino: '/mapa',
    etiqueta: '← Volver al mapa',
  }
}
