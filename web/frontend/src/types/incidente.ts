export interface Incidente {
  id: string
  categoria?: {
    nombre?: string | null
  } | null
  ubicacion?: {
    comuna?: string | null
  } | null
  nivelPrioridad?: number | null
  estado?: {
    nombre?: string | null
  } | null
}