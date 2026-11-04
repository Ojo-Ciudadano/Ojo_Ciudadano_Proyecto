export interface AsignacionActiva {
  id: string
  incidenteId: string
  usuarioAsignadoId: string
  asignadoPorId: string
  fechaAsignacion: string
  activa: boolean
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
}