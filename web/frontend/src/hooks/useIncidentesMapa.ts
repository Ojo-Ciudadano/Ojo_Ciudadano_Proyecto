import { useCallback, useEffect, useMemo, useState } from 'react'
import { obtenerIncidentesParaMapa } from '../services/incidentes.service'
import {
  interpretarErrorMapa,
  mapearIncidentesAMarcadores,
  type ErrorMapa,
} from '../components/map/incidentMarkers'
import type { IncidentMapMarker } from '../components/map/mapConfig'
import type { Incidente } from '../types/incidente'

export interface IncidentesMapaState {
  marcadores: IncidentMapMarker[]
  cargando: boolean
  error: ErrorMapa | null
  reintentar: () => void
  totalIncidentes: number
  totalGeorreferenciados: number
  totalOmitidos: number
}

/**
 * Consulta los incidentes y los transforma en marcadores para el mapa (#140).
 *
 * Mantiene los marcadores anteriores mientras se refresca la consulta y los
 * reemplaza por completo al finalizar, de modo que `IncidentMap` limpia el
 * `LayerGroup` y evita duplicados.
 */
export function useIncidentesMapa(): IncidentesMapaState {
  const [incidentes, setIncidentes] = useState<Incidente[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<ErrorMapa | null>(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function cargarIncidentes() {
      setCargando(true)
      setError(null)

      try {
        const datos = await obtenerIncidentesParaMapa(controller.signal)

        if (controller.signal.aborted) {
          return
        }

        setIncidentes(datos)
      } catch (err: unknown) {
        if (controller.signal.aborted) {
          return
        }

        setIncidentes([])
        setError(interpretarErrorMapa(err))
      } finally {
        if (!controller.signal.aborted) {
          setCargando(false)
        }
      }
    }

    void cargarIncidentes()

    return () => controller.abort()
  }, [intento])

  const marcadores = useMemo(
    () => mapearIncidentesAMarcadores(incidentes),
    [incidentes],
  )

  const reintentar = useCallback(() => {
    setIntento((actual) => actual + 1)
  }, [])

  const totalIncidentes = incidentes.length
  const totalGeorreferenciados = marcadores.length

  return {
    marcadores,
    cargando,
    error,
    reintentar,
    totalIncidentes,
    totalGeorreferenciados,
    totalOmitidos: Math.max(0, totalIncidentes - totalGeorreferenciados),
  }
}
