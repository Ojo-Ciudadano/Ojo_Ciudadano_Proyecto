import { useRef, useState } from 'react'
import {
  cambiarEstadoIncidente,
  type CambioEstadoIncidente,
} from '../services/incidentes.service'
import type { Incidente } from '../types/incidente'

function obtenerCodigoError(error: unknown): number | undefined {
  if (!error || typeof error !== 'object') {
    return undefined
  }

  if ('statusCode' in error && typeof error.statusCode === 'number') {
    return error.statusCode
  }

  return undefined
}

function obtenerMensajeError(error: unknown): string {
  const codigo = obtenerCodigoError(error)

  switch (codigo) {
    case 400:
      return 'Revisa el estado seleccionado y la observación ingresada.'
    case 401:
      return 'Tu sesión no es válida. Inicia sesión nuevamente.'
    case 403:
      return 'No tienes permisos para cambiar el estado del incidente.'
    case 404:
      return 'El incidente o el estado solicitado ya no está disponible.'
    case 409:
      return 'El estado cambió o la transición ya no está permitida. Actualizaremos los datos del incidente.'
    default:
      return 'No se pudo confirmar el cambio de estado. Consulta el estado actual antes de reintentar. Tu observación se conservará.'
  }
}

export function useCambioEstadoIncidente() {
  const bloqueoEnvio = useRef(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)

  async function enviarCambio(
    id: string,
    cambio: CambioEstadoIncidente,
    alConfirmar: (incidente: Incidente) => void,
    alConflicto: () => Promise<void>,
  ): Promise<boolean> {
    if (bloqueoEnvio.current) {
      return false
    }

    setError(null)
    setExito(null)

    if (!cambio.observacion.trim()) {
      setError('Debes ingresar una observación.')
      return false
    }

    bloqueoEnvio.current = true
    setEnviando(true)

    try {
      const confirmado = await cambiarEstadoIncidente(id, cambio)

      alConfirmar(confirmado)
      setExito('El estado del incidente se actualizó correctamente.')

      return true
    } catch (errorConsulta: unknown) {
      setError(obtenerMensajeError(errorConsulta))

      if (obtenerCodigoError(errorConsulta) === 409) {
        try {
          await alConflicto()
        } catch {
          setError(
            'Se produjo un conflicto y no se pudo consultar el estado actual. Tu observación se conservará. Vuelve a consultar el incidente antes de reintentar.',
          )
        }
      }

      return false
    } finally {
      bloqueoEnvio.current = false
      setEnviando(false)
    }
  }

  return {
    enviando,
    error,
    exito,
    enviarCambio,
  }
}