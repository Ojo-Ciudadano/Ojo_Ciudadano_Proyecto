import type { EvidenciaIncidente } from './evidenciaIncidente'

export interface AsignacionActiva {
  id: string
  incidenteId: string
  usuarioAsignadoId: string
  asignadoPorId: string
  fechaAsignacion: string
  activa: boolean
}

export interface EstadoHistorialIncidente {
  id?: string | null
  nombre?: string | null
  orden?: number | null
}

export interface RegistroHistorialIncidente {
  id: string
  incidenteId: string
  estado?: EstadoHistorialIncidente | null
  usuarioId?: string | null
  observacion?: string | null
  fecha?: string | null
  tipo?: string | null
  tipoEvento?: string | null
}

export interface IncidenteDetalle {
  id: string
  titulo?: string | null
  descripcion?: string | null
  categoriaId?: string | null
  estadoId: string
  usuarioReportaId?: string | null
  nivelPrioridad?: number | null
  fechaCreacion?: string | null
  fechaActualizacion?: string | null

  categoria?: {
    id?: string | null
    nombre?: string | null
  } | null

  estado?: {
    id?: string | null
    nombre?: string | null
  } | null

  ubicacion?: {
    id?: string | null
    latitud?: number | null
    longitud?: number | null
    direccionReferencia?: string | null
    comuna?: string | null
    region?: string | null
    sector?: string | null
  } | null

  asignacionActiva?: AsignacionActiva | null
  historial?: RegistroHistorialIncidente[] | null
  historialVersion?: number
  evidencias?: EvidenciaIncidente[] | null
}