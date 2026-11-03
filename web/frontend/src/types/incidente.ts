export interface UbicacionIncidente {
  id?: string | null
  latitud?: number | null
  longitud?: number | null
  comuna?: string | null
  region?: string | null
  direccionReferencia?: string | null
  sector?: string | null
}

export interface Incidente {
  id: string
  titulo?: string | null
  descripcion?: string | null
  fechaCreacion?: string | null
  categoriaId?: string | null
  estadoId?: string | null
  usuarioReportaId?: string | null
  fechaActualizacion?: string | null
  categoria?: {
    id?: string | null
    nombre?: string | null
  } | null
  ubicacion?: UbicacionIncidente | null
  nivelPrioridad?: number | null
  estado?: {
    id?: string | null
    nombre?: string | null
  } | null
}

export interface FiltrosIncidentes {
  estadoId: string
  comuna: string
  categoriaId: string
}
