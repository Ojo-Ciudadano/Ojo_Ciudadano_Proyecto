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

export function getInitialRouteForRoles(
  roles?: string[] | null,
): string {
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

export function hasKnownRole(roles?: string[] | null): boolean {
  if (!Array.isArray(roles)) return false

  return roles.includes(ROLES.ADMIN) || roles.includes(ROLES.CIUDADANO)
}

export function isRoleAuthorized(
  userRoles: string[] | null | undefined,
  allowedRoles: readonly (RoleName | string)[],
): boolean {
  if (!Array.isArray(userRoles) || userRoles.length === 0) {
    return false
  }

  return allowedRoles.some((allowed) => userRoles.includes(allowed))
}

export const NAVIGATION_RULES: readonly NavigationRule[] = [
  {
    path: '/',
    label: 'Inicio',
    allowedRoles: [ROLES.CIUDADANO],
    section: 'main',
  },
  {
    path: '/reportes/nuevo',
    label: 'Nuevo reporte',
    allowedRoles: [ROLES.CIUDADANO],
    section: 'main',
  },
  {
    path: '/dashboard',
    label: 'Inicio',
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
    label: 'Mapa de incidentes',
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

export function getVisibleNavigation(
  userRoles?: string[] | null,
  section?: 'main' | 'admin',
): NavigationRule[] {
  if (!hasKnownRole(userRoles)) {
    return []
  }

  const esAdministrador = userRoles?.includes(ROLES.ADMIN) ?? false

  return NAVIGATION_RULES.filter((rule) => {
    if (rule.path === '/' && esAdministrador) {
      return false
    }

    const matchesSection = section ? rule.section === section : true

    return (
      matchesSection &&
      isRoleAuthorized(userRoles, rule.allowedRoles)
    )
  })
}