import { useEffect, useState } from 'react'
import { obtenerIncidenteDetalle } from '../services/incidentes.service'
import type { IncidenteDetalle } from '../types/incidenteDetalle'

interface ErrorDetalle {
  mensaje: string
  permiteReintentar: boolean
}

interface ResultadoConsulta {
  clave: string
  incidente: IncidenteDetalle | null
  error: ErrorDetalle | null
  noEncontrado: boolean
}

function interpretarError(error: unknown): {
  error: ErrorDetalle | null
  noEncontrado: boolean
} {
  const statusCode =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  if (statusCode === 404) {
    return {
      error: null,
      noEncontrado: true,
    }
  }

  if (statusCode === 400) {
    return {
      error: {
        mensaje: 'El identificador del incidente no es válido.',
        permiteReintentar: false,
      },
      noEncontrado: false,
    }
  }

  if (statusCode === 401) {
    return {
      error: {
        mensaje: 'Tu sesión no es válida. Ingresa nuevamente.',
        permiteReintentar: false,
      },
      noEncontrado: false,
    }
  }

  if (statusCode === 403) {
    return {
      error: {
        mensaje: 'No tienes permisos para consultar este incidente.',
        permiteReintentar: false,
      },
      noEncontrado: false,
    }
  }

  return {
    error: {
      mensaje: 'No se pudo cargar el incidente. Inténtalo nuevamente.',
      permiteReintentar: true,
    },
    noEncontrado: false,
  }
}

export function useIncidenteDetalle(id: string | undefined) {
  const [intento, setIntento] = useState(0)
  const [resultado, setResultado] =
    useState<ResultadoConsulta | null>(null)

  const claveConsulta = JSON.stringify([id ?? '', intento])

  useEffect(() => {
    const controller = new AbortController()

    async function cargarDetalle() {
      if (!id) {
        setResultado({
          clave: claveConsulta,
          incidente: null,
          error: {
            mensaje: 'Falta el identificador del incidente.',
            permiteReintentar: false,
          },
          noEncontrado: false,
        })
        return
      }

      try {
        const incidente = await obtenerIncidenteDetalle(
          id,
          controller.signal,
        )

        if (controller.signal.aborted) return

        setResultado({
          clave: claveConsulta,
          incidente,
          error: null,
          noEncontrado: false,
        })
      } catch (error: unknown) {
        if (controller.signal.aborted) return

        const fallo = interpretarError(error)

        setResultado({
          clave: claveConsulta,
          incidente: null,
          ...fallo,
        })
      }
    }

    void cargarDetalle()

    return () => controller.abort()
  }, [id, claveConsulta])

  const resultadoActual =
    resultado?.clave === claveConsulta ? resultado : null

  function reintentar() {
    setIntento((actual) => actual + 1)
  }

  return {
    incidente: resultadoActual?.incidente ?? null,
    cargando: resultadoActual === null,
    error: resultadoActual?.error ?? null,
    noEncontrado: resultadoActual?.noEncontrado ?? false,
    reintentar,
  }
}