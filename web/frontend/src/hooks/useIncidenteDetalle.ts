import { useEffect, useState } from 'react'
import { obtenerIncidenteDetalle } from '../services/incidentes.service'
import type { Incidente } from '../types/incidente'
import type {
  AsignacionActiva,
  IncidenteDetalle,
} from '../types/incidenteDetalle'

export interface ErrorDetalle {
  mensaje: string
  permiteReintentar: boolean
  accesoDenegado?: boolean
}

interface ResultadoConsulta {
  clave: string
  incidente: IncidenteDetalle | null
  error: ErrorDetalle | null
  noEncontrado: boolean
}

export function interpretarErrorDetalleIncidente(error: unknown): {
  error: ErrorDetalle | null
  noEncontrado: boolean
} {
  const candidato =
    typeof error === 'object' && error !== null
      ? (error as {
          statusCode?: unknown
          status?: unknown
          response?: { status?: unknown } | null
        })
      : null

  const statusCode = Number(
    candidato?.statusCode ?? candidato?.response?.status ?? candidato?.status,
  )

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
        accesoDenegado: true,
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

  const idNormalizado = id?.trim() ?? ''
  const claveConsulta = JSON.stringify([idNormalizado, intento])

  useEffect(() => {
    const controller = new AbortController()

    async function cargarDetalle() {
      if (!idNormalizado) {
        setResultado({
          clave: claveConsulta,
          incidente: null,
          error: {
            mensaje: 'El identificador del incidente no es válido.',
            permiteReintentar: false,
          },
          noEncontrado: false,
        })
        return
      }

      try {
        const incidente = await obtenerIncidenteDetalle(
          idNormalizado,
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

        const fallo = interpretarErrorDetalleIncidente(error)

        setResultado({
          clave: claveConsulta,
          incidente: null,
          ...fallo,
        })
      }
    }

    void cargarDetalle()

    return () => controller.abort()
  }, [idNormalizado, claveConsulta])

  const resultadoActual =
    resultado?.clave === claveConsulta ? resultado : null

  function reintentar() {
    setIntento((actual) => actual + 1)
  }

  function actualizarEstadoConfirmado(confirmado: Incidente) {
    const estadoIdConfirmado = confirmado.estadoId

    if (
      typeof estadoIdConfirmado !== 'string' ||
      !estadoIdConfirmado.trim()
    ) {
      throw new Error(
        'El servidor no devolvió un identificador de estado válido.',
      )
    }

    setResultado((actual) => {
      if (
        !actual ||
        actual.clave !== claveConsulta ||
        !actual.incidente ||
        actual.incidente.id !== confirmado.id
      ) {
        return actual
      }

      return {
        ...actual,
        incidente: {
          ...actual.incidente,
          estadoId: estadoIdConfirmado,
          estado: confirmado.estado,
          fechaActualizacion:
            confirmado.fechaActualizacion ??
            actual.incidente.fechaActualizacion,
        },
      }
    })
  }

  function actualizarAsignacionConfirmada(
    asignacion: AsignacionActiva,
  ) {
    setResultado((actual) => {
      if (
        !actual ||
        actual.clave !== claveConsulta ||
        !actual.incidente ||
        actual.incidente.id !== asignacion.incidenteId ||
        !asignacion.activa
      ) {
        return actual
      }

      return {
        ...actual,
        incidente: {
          ...actual.incidente,
          asignacionActiva: asignacion,
        },
      }
    })
  }

  function actualizarPrioridadConfirmada(confirmado: Incidente) {
    const prioridad = confirmado.nivelPrioridad

    if (
      typeof prioridad !== 'number' ||
      !Number.isInteger(prioridad) ||
      prioridad < 1 ||
      prioridad > 5
    ) {
      throw new Error(
        'El servidor no devolvió una prioridad válida.',
      )
    }

    setResultado((actual) => {
      if (
        !actual ||
        actual.clave !== claveConsulta ||
        !actual.incidente ||
        actual.incidente.id !== confirmado.id
      ) {
        return actual
      }

      return {
        ...actual,
        incidente: {
          ...actual.incidente,
          nivelPrioridad: prioridad,
          fechaActualizacion:
            confirmado.fechaActualizacion ??
            actual.incidente.fechaActualizacion,
        },
      }
    })
  }

  async function recargarDetalle(): Promise<void> {
    if (!idNormalizado) {
      throw new Error('Falta el identificador del incidente.')
    }

    const actualizado = await obtenerIncidenteDetalle(idNormalizado)

    setResultado((actual) => {
      if (
        !actual ||
        actual.clave !== claveConsulta ||
        actual.incidente?.id !== actualizado.id
      ) {
        return actual
      }

      return {
        ...actual,
        incidente: actualizado,
        error: null,
        noEncontrado: false,
      }
    })
  }

  return {
    incidente: resultadoActual?.incidente ?? null,
    cargando: resultadoActual === null,
    error: resultadoActual?.error ?? null,
    noEncontrado: resultadoActual?.noEncontrado ?? false,
    reintentar,
    actualizarEstadoConfirmado,
    actualizarAsignacionConfirmada,
    actualizarPrioridadConfirmada,
    recargarDetalle,
  }
}
