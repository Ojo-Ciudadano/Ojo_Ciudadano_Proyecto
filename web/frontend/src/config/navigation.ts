import type { RoleName } from '../types'

export interface NavigationRule {
  path: string
  label: string
  allowedRoles: RoleName[]
  section: 'main' | 'admin'
}

export const ROLES: Record<RoleName, RoleName> = {
  ADMIN: 'ADMIN',
  CIUDADANO: 'CIUDADANO',
}

/**
 * Destino inicial por rol autenticado (Issue #130).
 * - ADMIN -> /dashboard (gestión administrativa)
 * - CIUDADANO -> / (inicio de reportes ciudadanos)
 * - Rol desconocido -> /login
 */
export function getInitialRouteForRoles(roles?: string[] | null): string {
  if (!Array.isArray(roles) || roles.length === 0) {
    return '/login'
  }
  if (roles.includes(ROLES.ADMIN)) {
    return '/dashboard'
  }
  if (roles.includes(ROLES.CIUDADANO)) {
    return '/'
  }
  return '/login'
}

/**
 * Verifica si una lista de roles contiene al menos un rol válido del sistema.
 */
export function hasKnownRole(roles?: string[] | null): boolean {
  if (!Array.isArray(roles)) return false
  return roles.includes(ROLES.ADMIN) || roles.includes(ROLES.CIUDADANO)
}

/**
 * Verifica si los roles del usuario cumplen con los roles permitidos de una ruta.
 */
export function isRoleAuthorized(
  userRoles: string[] | null | undefined,
  allowedRoles: readonly (RoleName | string)[],
): boolean {
  if (!Array.isArray(userRoles) || userRoles.length === 0) {
    return false
  }
  return allowedRoles.some((allowed) => userRoles.includes(allowed))
}

/**
 * Reglas centralizadas de rutas y menú Web para Sprint 2 (Issue #130).
 */
export const NAVIGATION_RULES: readonly NavigationRule[] = [
  {
    path: '/',
    label: 'Inicio',
    allowedRoles: [ROLES.CIUDADANO, ROLES.ADMIN],
    section: 'main',
  },
  {
    path: '/dashboard',
    label: 'Dashboard',
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    path: '/incidentes',
    label: 'Incidentes',
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    path: '/mapa',
    label: 'Mapa territorial',
    allowedRoles: [ROLES.CIUDADANO, ROLES.ADMIN],
    section: 'main',
  },
  {
    path: '/admin',
    label: 'Usuarios y reglas',
    allowedRoles: [ROLES.ADMIN],
    section: 'admin',
  },
] as const

/**
 * Filtra las opciones de menú visibles según los roles del usuario autenticado.
 */
export function getVisibleNavigation(
  userRoles?: string[] | null,
  section?: 'main' | 'admin',
): NavigationRule[] {
  if (!hasKnownRole(userRoles)) {
    return []
  }
  return NAVIGATION_RULES.filter((rule) => {
    const matchesSection = section ? rule.section === section : true
    return matchesSection && isRoleAuthorized(userRoles, rule.allowedRoles)
  })
}
