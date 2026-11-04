import { apiClient } from './api'
import { normalizarIncidentes } from './incidentes.service'
import type { Incidente } from '../types/incidente'

export interface ConsultaInicioCiudadano {
  estadoId?: string
  usuarioReportaId?: string
}

export interface OpcionEstadoCiudadano {
  id: string
  nombre: string
}

export async function obtenerIncidentesCiudadano(
  consulta: ConsultaInicioCiudadano = {},
  signal?: AbortSignal,
): Promise<Incidente[]> {
  const params: Record<string, string> = {}

  const estadoId = consulta.estadoId?.trim()
  const usuarioReportaId = consulta.usuarioReportaId?.trim()

  if (estadoId) {
    params.estadoId = estadoId
  }

  if (usuarioReportaId) {
    params.usuarioReportaId = usuarioReportaId
  }

  const response = await apiClient.get<unknown>('/incidentes', {
    params,
    signal,
  })

  const incidentes = normalizarIncidentes(response.data)

  if (
    !incidentes ||
    !incidentes.every(
      (incidente) =>
        incidente !== null &&
        typeof incidente === 'object' &&
        typeof incidente.id === 'string' &&
        Boolean(incidente.id.trim()),
    )
  ) {
    throw new Error(
      'El servidor devolvió un listado de incidentes inválido.',
    )
  }

  // Si el backend no respeta el filtro, no mostramos resultados ajenos
  // como si fueran reportes del usuario autenticado.
  if (
    usuarioReportaId &&
    incidentes.some(
      (incidente) => incidente.usuarioReportaId !== usuarioReportaId,
    )
  ) {
    throw new Error(
      'No se pudo verificar que los reportes correspondan a tu cuenta.',
    )
  }

  return incidentes
}

export function obtenerEstadosCiudadano(
  incidentes: Incidente[],
): OpcionEstadoCiudadano[] {
  const opciones = new Map<string, string>()

  for (const incidente of incidentes) {
    const id = incidente.estado?.id ?? incidente.estadoId
    const nombre = incidente.estado?.nombre?.trim()

    if (id && nombre) {
      opciones.set(id, nombre)
    }
  }

  return Array.from(opciones, ([id, nombre]) => ({
    id,
    nombre,
  })).sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
}