import { apiClient } from './api'
import {
  validarFotografia,
  type EvidenciaCreada,
} from '../types/fotografiaReporte'

export async function subirFotografiaReporte(
  incidenteId: string,
  archivo: File,
): Promise<EvidenciaCreada> {
  const identificador = incidenteId.trim()

  if (!identificador) {
    throw new Error(
      'Falta el identificador del incidente creado.',
    )
  }

  const errorValidacion = validarFotografia(archivo)

  if (errorValidacion) {
    throw new Error(errorValidacion)
  }

  const formulario = new FormData()
  formulario.append('archivo', archivo, archivo.name)

  const response = await apiClient.post<unknown>(
    `/incidentes/${encodeURIComponent(identificador)}/evidencias`,
    formulario,
    {
      headers: {
        'Content-Type': null,
      },
    },
  )

  const evidencia = response.data

  if (
    typeof evidencia !== 'object' ||
    evidencia === null ||
    !('id' in evidencia) ||
    typeof evidencia.id !== 'string' ||
    !evidencia.id.trim() ||
    !('incidenteId' in evidencia) ||
    evidencia.incidenteId !== identificador ||
    !('tipoMime' in evidencia) ||
    evidencia.tipoMime !== archivo.type ||
    !('tamanoArchivo' in evidencia) ||
    evidencia.tamanoArchivo !== archivo.size ||
    !('fechaCreacion' in evidencia) ||
    typeof evidencia.fechaCreacion !== 'string' ||
    !('url' in evidencia) ||
    typeof evidencia.url !== 'string'
  ) {
    throw new Error(
      'No se pudo verificar la confirmación de la fotografía. El incidente creado se conserva.',
    )
  }

  return evidencia as EvidenciaCreada
}

export function mensajeErrorFotografia(error: unknown): string {
  const codigo =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  switch (codigo) {
    case 400:
      return 'El servidor rechazó la fotografía. Comprueba que sea una imagen JPEG, PNG o WebP válida.'
    case 401:
      return 'Tu sesión ya no es válida. Inicia sesión nuevamente.'
    case 403:
      return 'No tienes permisos para subir esta fotografía.'
    case 404:
      return 'El incidente no está disponible para asociar la fotografía.'
    case 413:
      return 'La fotografía supera el máximo permitido de 10 MB.'
    case 502:
    case 503:
      return 'El servicio no pudo almacenar la fotografía. Puedes reintentar su subida.'
    default:
      return error instanceof Error
        ? error.message
        : 'No se pudo confirmar la subida. El incidente se conserva; puedes reintentar esta fotografía.'
  }
}