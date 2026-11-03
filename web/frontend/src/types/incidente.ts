export interface Incidente {
  id: string
  categoriaId?: string | null
  estadoId?: string | null
  categoria?: {
    id?: string | null
    nombre?: string | null
  } | null
  ubicacion?: {
    comuna?: string | null
  } | null
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