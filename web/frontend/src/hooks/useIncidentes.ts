import { useEffect, useState } from 'react'
import { obtenerIncidentes } from '../services/incidentes.service'
import type { Incidente } from '../types/incidente'

interface ErrorConsulta {
  mensaje: string
  permiteReintentar: boolean
}

function interpretarError(error: unknown): ErrorConsulta {
  const statusCode =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  if (statusCode === 401) {
    return {
      mensaje: 'Tu sesión no es válida. Cierra sesión e ingresa nuevamente.',
      permiteReintentar: false,
    }
  }

  if (statusCode === 403) {
    return {
      mensaje: 'No tienes permisos para consultar los incidentes.',
      permiteReintentar: false,
    }
  }

  return {
    mensaje: 'No se pudieron cargar los incidentes. Inténtalo nuevamente.',
    permiteReintentar: true,
  }
}

export function useIncidentes() {
  const [incidentes, setIncidentes] = useState<Incidente[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<ErrorConsulta | null>(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function cargarIncidentes() {
      setCargando(true)
      setError(null)

      try {
        const datos = await obtenerIncidentes(controller.signal)

        if (!controller.signal.aborted) {
          setIncidentes(datos)
        }
      } catch (errorConsulta: unknown) {
        if (!controller.signal.aborted) {
          setIncidentes([])
          setError(interpretarError(errorConsulta))
        }
      } finally {
        if (!controller.signal.aborted) {
          setCargando(false)
        }
      }
    }

    void cargarIncidentes()

    return () => {
      controller.abort()
    }
  }, [intento])

  function reintentar() {
    setIntento((valorActual) => valorActual + 1)
  }

  return {
    incidentes,
    cargando,
    error,
    reintentar,
  }
}