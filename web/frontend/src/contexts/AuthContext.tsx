
import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react'

import type { User, RoleName } from '../types'
import { authService } from '../services/auth.service'

import {
  clearSessionStorage,
  onSessionInvalidated,
  startNewSession,
} from '../services/api'

import { hasKnownRole } from '../config/navigation'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  sessionExpiredMessage: string | null
  clearSessionExpiredMessage: () => void
  hasRole: (role: RoleName | string) => boolean
  isAdmin: boolean
  isCiudadano: boolean
  login: (email: string, password: string) => Promise<User>
  logout: () => Promise<void>
  refreshProfile: () => Promise<User | null>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  // No asumir un rol desde localStorage antes de consultar /usuarios/me (#130)
  const [user, setUser] = useState<User | null>(null)

  const [sessionExpiredMessage, setSessionExpiredMessage] =
    useState<string | null>(null)

  const [isLoading, setIsLoading] = useState<boolean>(() => {
    return Boolean(localStorage.getItem('access_token'))
  })

  // Escuchar invalidaciones de sesión desde el cliente HTTP
  useEffect(() => {
    const unsubscribe = onSessionInvalidated((event) => {
      setUser(null)
      setIsLoading(false)
      setSessionExpiredMessage(event.message)
    })

    return unsubscribe
  }, [])

  const clearSessionExpiredMessage = useCallback(() => {
    setSessionExpiredMessage(null)
  }, [])

  // Recuperar el perfil real desde el backend (#118)
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
        clearSessionStorage()
        setUser(null)
        return null
      }

      localStorage.setItem('user', JSON.stringify(profile))
      setUser(profile)

      return profile
    } catch {
      clearSessionStorage()
      setUser(null)
      return null
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('access_token')

    if (token) {
      void refreshProfile()
    } else {
      localStorage.removeItem('user')
      setIsLoading(false)
    }
  }, [refreshProfile])

  // Iniciar sesión y consultar roles vigentes
  const login = useCallback(
    async (email: string, password: string): Promise<User> => {
      setIsLoading(true)

      try {
        const response = await authService.login({
          email,
          password,
        })

        startNewSession(response.access_token)

        const profile = await authService.getProfile()

        if (!hasKnownRole(profile.roles)) {
          clearSessionStorage()
          setUser(null)

          throw new Error(
            'El usuario no posee un rol válido autorizado en la plataforma',
          )
        }

        localStorage.setItem('user', JSON.stringify(profile))
        setSessionExpiredMessage(null)
        setUser(profile)

        return profile
      } catch (err) {
        clearSessionStorage()
        setUser(null)
        throw err
      } finally {
        setIsLoading(false)
      }
    },
    [],
  )

  // Cerrar sesión
  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
  }, [])

  // Verificar roles del usuario
  const hasRole = useCallback(
    (role: RoleName | string): boolean => {
      if (!user || !Array.isArray(user.roles)) {
        return false
      }

      return user.roles.includes(role)
    },
    [user],
  )

  const isAdmin = hasRole('ADMIN')
  const isCiudadano = hasRole('CIUDADANO')

  const contextValue: AuthContextType = {
    user,
    isAuthenticated: Boolean(
      user &&
      hasKnownRole(user.roles) &&
      localStorage.getItem('access_token'),
    ),
    isLoading,
    sessionExpiredMessage,
    clearSessionExpiredMessage,
    hasRole,
    isAdmin,
    isCiudadano,
    login,
    logout,
    refreshProfile,
  }

  return React.createElement(
    AuthContext.Provider,
    { value: contextValue },
    children,
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (context === undefined) {
    throw new Error(
      'useAuth debe ser usado dentro de un AuthProvider',
    )
  }

  return context
}
