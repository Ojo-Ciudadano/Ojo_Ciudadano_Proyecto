import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import type { User, RoleName } from '../types'
import { authService } from '../services/auth.service'
import { hasKnownRole } from '../config/navigation'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  hasRole: (role: RoleName | string) => boolean
  isAdmin: boolean
  isCiudadano: boolean
  login: (email: string, password: string) => Promise<User>
  logout: () => Promise<void>
  refreshProfile: () => Promise<User | null>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  // Inicializa en null cuando hay token para no asumir un rol desde localStorage antes de /usuarios/me (#130)
  const [user, setUser] = useState<User | null>(null)

  const [isLoading, setIsLoading] = useState<boolean>(() => {
    return Boolean(localStorage.getItem('access_token'))
  })

  // Consulta el perfil real al montar o al recargar la página (Issue #118)
  const refreshProfile = useCallback(async (): Promise<User | null> => {
    const token = localStorage.getItem('access_token')
    if (!token) {
      setUser(null)
      setIsLoading(false)
      return null
    }

    setIsLoading(true)
    try {
      const profile = await authService.getProfile()
      if (!hasKnownRole(profile.roles)) {
        // Un rol desconocido no concede acceso a rutas protegidas (#130)
        localStorage.removeItem('access_token')
        localStorage.removeItem('user')
        setUser(null)
        return null
      }
      localStorage.setItem('user', JSON.stringify(profile))
      setUser(profile)
      return profile
    } catch {
      // Si el token es inválido o expiró, limpia la sesión
      localStorage.removeItem('access_token')
      localStorage.removeItem('user')
      setUser(null)
      return null
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('access_token')
    if (token) {
      refreshProfile()
    } else {
      localStorage.removeItem('user')
      setIsLoading(false)
    }
  }, [refreshProfile])

  const login = useCallback(async (email: string, password: string): Promise<User> => {
    setIsLoading(true)
    try {
      const response = await authService.login({ email, password })
      localStorage.setItem('access_token', response.access_token)

      // Obtenemos inmediatamente el perfil real y rol vigente desde /usuarios/me (#118)
      const profile = await authService.getProfile()
      if (!hasKnownRole(profile.roles)) {
        localStorage.removeItem('access_token')
        localStorage.removeItem('user')
        setUser(null)
        throw new Error('El usuario no posee un rol válido autorizado en la plataforma')
      }

      localStorage.setItem('user', JSON.stringify(profile))
      setUser(profile)
      return profile
    } catch (err) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('user')
      setUser(null)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
  }, [])

  const hasRole = useCallback((role: RoleName | string): boolean => {
    if (!user || !Array.isArray(user.roles)) return false
    return user.roles.includes(role)
  }, [user])

  const isAdmin = hasRole('ADMIN')
  const isCiudadano = hasRole('CIUDADANO')

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: Boolean(
          user &&
          hasKnownRole(user.roles) &&
          localStorage.getItem('access_token'),
        ),
        isLoading,
        hasRole,
        isAdmin,
        isCiudadano,
        login,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth debe ser usado dentro de un AuthProvider')
  }
  return context
}
