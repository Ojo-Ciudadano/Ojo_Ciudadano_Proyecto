import { useAuth } from '../contexts/AuthContext'
import type { User } from '../types'

interface UseSessionReturn {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  isInitialized: boolean
  isAdmin: boolean
  isCiudadano: boolean
  hasRole: (role: string) => boolean
}

export function useSession(): UseSessionReturn {
  const { user, isAuthenticated, isLoading, isAdmin, isCiudadano, hasRole } = useAuth()

  return {
    user,
    isAuthenticated,
    isLoading,
    isInitialized: !isLoading,
    isAdmin,
    isCiudadano,
    hasRole,
  }
}
