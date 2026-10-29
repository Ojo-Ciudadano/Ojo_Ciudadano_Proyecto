import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { LoadingScreen } from './LoadingScreen'
import { getInitialRouteForRoles, hasKnownRole, isRoleAuthorized } from '../../config/navigation'
import type { RoleName } from '../../types'
import type { ReactNode } from 'react'

interface RoleGuardProps {
  children: ReactNode
  /**
   * Roles autorizados para acceder a la ruta (ej: ['ADMIN']).
   */
  allowedRoles: (RoleName | string)[]
  /**
   * Ruta opcional hacia donde redirigir si no tiene permisos.
   */
  redirectTo?: string
}

/**
 * Guard reutilizable para proteger rutas según rol del usuario autenticado (Issue #130).
 * - Mientras se obtiene el perfil desde /usuarios/me, muestra LoadingScreen sin asumir rol.
 * - Si no está autenticado o posee un rol desconocido, redirige a /login.
 * - Si el usuario no posee ninguno de los roles requeridos, impide el acceso y redirige a su ruta permitida.
 */
export function RoleGuard({ children, allowedRoles, redirectTo }: RoleGuardProps) {
  const { user, isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <LoadingScreen />
  }

  if (!isAuthenticated || !user || !hasKnownRole(user.roles)) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  const hasPermission = isRoleAuthorized(user.roles, allowedRoles)

  if (!hasPermission) {
    const defaultFallback = getInitialRouteForRoles(user.roles)
    const target = redirectTo ?? defaultFallback

    if (location.pathname === target) {
      return null
    }

    return <Navigate to={target} replace />
  }

  return <>{children}</>
}
