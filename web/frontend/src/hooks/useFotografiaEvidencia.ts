import {
  useCallback,
  useEffect,
  useState,
  useSyncExternalStore,
} from 'react'
import { useAuth } from '../contexts/AuthContext'
import {
  getSessionEpoch,
  subscribeSessionChanges,
} from '../services/api'
import {
  interpretarErrorEvidencia,
  obtenerArchivoEvidencia,
} from '../services/evidenciasIncidente.service'
import type { EvidenciaIncidente } from '../types/evidenciaIncidente'

interface ErrorFotografia {
  mensaje: string
  permiteReintentar: boolean
}

interface ResultadoFotografia {
  clave: string
  url: string | null
  error: ErrorFotografia | null
}

export function useFotografiaEvidencia(
  evidencia: EvidenciaIncidente,
) {
  const { user, isAuthenticated, isLoading } = useAuth()

  const epocaSesion = useSyncExternalStore(
    subscribeSessionChanges,
    getSessionEpoch,
    () => 0,
  )

  const sesionDisponible =
    isAuthenticated && !isLoading && Boolean(user?.id)

  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] =
    useState<ResultadoFotografia | null>(null)

  const {
    id,
    incidenteId,
    tipoMime,
    tamanoArchivo,
    fechaCreacion,
    url: referencia,
  } = evidencia

  const clave = JSON.stringify([
    user?.id,
    epocaSesion,
    sesionDisponible,
    id,
    incidenteId,
    tipoMime,
    tamanoArchivo,
    fechaCreacion,
    referencia,
    intento,
  ])

  useEffect(() => {
    if (!sesionDisponible) return

    const controller = new AbortController()
    let vigente = true
    let urlTemporal: string | null = null

    async function cargar() {
      try {
        const archivo = await obtenerArchivoEvidencia(
          {
            id,
            incidenteId,
            tipoMime,
            tamanoArchivo,
            fechaCreacion,
            url: referencia,
          },
          controller.signal,
        )

        if (
          !vigente ||
          controller.signal.aborted ||
          getSessionEpoch() !== epocaSesion
        ) {
          return
        }

        urlTemporal = URL.createObjectURL(archivo)

        setResultado({
          clave,
          url: urlTemporal,
          error: null,
        })
      } catch (error: unknown) {
        if (
          !vigente ||
          controller.signal.aborted ||
          getSessionEpoch() !== epocaSesion
        ) {
          return
        }

        setResultado({
          clave,
          url: null,
          error: interpretarErrorEvidencia(error),
        })
      }
    }

    void cargar()

    return () => {
      vigente = false
      controller.abort()

      if (urlTemporal) {
        URL.revokeObjectURL(urlTemporal)
      }
    }
  }, [
    clave,
    sesionDisponible,
    epocaSesion,
    id,
    incidenteId,
    tipoMime,
    tamanoArchivo,
    fechaCreacion,
    referencia,
  ])

  const resultadoActual =
    sesionDisponible && resultado?.clave === clave
      ? resultado
      : null

  const reintentar = useCallback(() => {
    setIntento((actual) => actual + 1)
  }, [])

  const informarErrorImagen = useCallback(() => {
    setResultado((actual) => {
      if (!actual || actual.clave !== clave) return actual

      return {
        ...actual,
        url: null,
        error: {
          mensaje:
            'El navegador no pudo mostrar esta fotografía. Puedes reintentar.',
          permiteReintentar: true,
        },
      }
    })
  }, [clave])

  return {
    url: resultadoActual?.url ?? null,
    error: resultadoActual?.error ?? null,
    cargando:
      isLoading ||
      (sesionDisponible && resultadoActual === null),
    sesionDisponible,
    reintentar,
    informarErrorImagen,
  }
}
