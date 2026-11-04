
import {
  useCallback,
  useEffect,
  useState,
  useSyncExternalStore,
} from 'react'
import axios from 'axios'
import { useAuth } from '../contexts/AuthContext'
import {
  getSessionEpoch,
  subscribeSessionChanges,
} from '../services/api'
import { obtenerNotificaciones } from '../services/notificaciones.service'
import type { Notificacion } from '../types/notificacion'

interface EstadoNotificaciones {
  sesion: string
  notificaciones: Notificacion[]
  cargando: boolean
  error: string | null
}

interface UseNotificacionesResult {
  notificaciones: Notificacion[]
  cargando: boolean
  error: string | null
  reintentar: () => void
}

export function useNotificaciones(): UseNotificacionesResult {
  const { user, isAuthenticated, isLoading } = useAuth()

  const sessionEpoch = useSyncExternalStore(
    subscribeSessionChanges,
    getSessionEpoch,
    () => 0,
  )

  const sesionActiva =
    !isLoading && isAuthenticated && Boolean(user)

  const claveSesion = sesionActiva
    ? `${sessionEpoch}:${user?.id}`
    : ''

  const [estado, setEstado] = useState<EstadoNotificaciones>({
    sesion: '',
    notificaciones: [],
    cargando: false,
    error: null,
  })

  const [intento, setIntento] = useState(0)

  useEffect(() => {
    if (!claveSesion) {
      return
    }

    const controller = new AbortController()
    let vigente = true

    setEstado({
      sesion: claveSesion,
      notificaciones: [],
      cargando: true,
      error: null,
    })

    async function consultar() {
      try {
        const resultado = await obtenerNotificaciones(
          controller.signal,
        )

        if (
          !vigente ||
          controller.signal.aborted ||
          getSessionEpoch() !== sessionEpoch
        ) {
          return
        }

        setEstado({
          sesion: claveSesion,
          notificaciones: resultado,
          cargando: false,
          error: null,
        })
      } catch (err: unknown) {
        if (
          !vigente ||
          controller.signal.aborted ||
          axios.isCancel(err) ||
          getSessionEpoch() !== sessionEpoch
        ) {
          return
        }

        let mensaje = 'No fue posible cargar las notificaciones.'

        if (
          err instanceof Error &&
          err.message
        ) {
          mensaje = err.message
        } else if (
          typeof err === 'object' &&
          err !== null &&
          'message' in err &&
          typeof err.message === 'string'
        ) {
          mensaje = err.message
        }

        setEstado({
          sesion: claveSesion,
          notificaciones: [],
          cargando: false,
          error: mensaje,
        })
      }
    }

    void consultar()

    return () => {
      vigente = false
      controller.abort()
    }
  }, [claveSesion, sessionEpoch, intento])

  const reintentar = useCallback(() => {
    if (claveSesion) {
      setIntento((actual) => actual + 1)
    }
  }, [claveSesion])

  // Nunca exponer datos de una sesión anterior.
  if (!claveSesion || estado.sesion !== claveSesion) {
    return {
      notificaciones: [],
      cargando: Boolean(claveSesion),
      error: null,
      reintentar,
    }
  }

  return {
    notificaciones: estado.notificaciones,
    cargando: estado.cargando,
    error: estado.error,
    reintentar,
  }
}
