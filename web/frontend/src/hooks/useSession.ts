import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import type { User } from '../types'

interface UseSessionReturn {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  isInitialized: boolean
}

export function useSession(): UseSessionReturn {
  const { user, isAuthenticated, isLoading } = useAuth()
  const [isInitialized, setIsInitialized] = useState(false)

  useEffect(() => {
    const initSession = async () => {
      const token = localStorage.getItem('access_token')
      const storedUser = localStorage.getItem('user')

      if (token && storedUser) {
        try {
          JSON.parse(storedUser)
        } catch {
          localStorage.removeItem('access_token')
          localStorage.removeItem('user')
        }
      }

      setIsInitialized(true)
    }

    initSession()
  }, [])

  return {
    user,
    isAuthenticated: isAuthenticated && isInitialized,
    isLoading: isLoading || !isInitialized,
    isInitialized,
  }
}