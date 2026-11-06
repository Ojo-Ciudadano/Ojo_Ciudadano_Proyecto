export const MAX_FOTOGRAFIA_BYTES = 10 * 1024 * 1024

export const TIPOS_FOTOGRAFIA_PERMITIDOS: readonly string[] = [
  'image/jpeg',
  'image/png',
  'image/webp',
]

export const ACCEPT_FOTOGRAFIAS =
  'image/jpeg,image/png,image/webp'

export interface EvidenciaCreada {
  id: string
  incidenteId: string
  tipoMime: string
  tamanoArchivo: number
  fechaCreacion: string
  url: string
}

export type EstadoFotografia =
  | 'pendiente'
  | 'subiendo'
  | 'confirmada'
  | 'fallida'

export interface FotografiaReporte {
  id: string
  archivo: File
  urlPrevisualizacion: string
  estado: EstadoFotografia
  error: string | null
}

export function validarFotografia(archivo: File): string | null {
  if (!TIPOS_FOTOGRAFIA_PERMITIDOS.includes(archivo.type)) {
    return 'Formato no permitido. Selecciona una fotografía JPEG, PNG o WebP.'
  }

  if (archivo.size === 0) {
    return 'El archivo está vacío. Selecciona otra fotografía.'
  }

  if (archivo.size > MAX_FOTOGRAFIA_BYTES) {
    return 'La fotografía supera el máximo permitido de 10 MB.'
  }

  return null
}