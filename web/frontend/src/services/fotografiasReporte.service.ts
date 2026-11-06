import { apiClient } from './api'
import {
  validarFotografia,
  type EvidenciaCreada,
} from '../types/fotografiaReporte'

type ConfirmacionFotografia = Pick<
  EvidenciaCreada,
  'id' | 'incidenteId'
>

export async function subirFotografiaReporte(
  incidenteId: string,
  archivo: File,
): Promise<ConfirmacionFotografia> {
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
      // El navegador establece multipart/form-data y su boundary.
      headers: {
        'Content-Type': null,
      },
      // Libera el formulario si el servidor no responde en dos minutos.
      timeout: 120_000,
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
    evidencia.incidenteId !== identificador
  ) {
    throw new Error(
      'El servidor respondió, pero no se pudo verificar la confirmación de la fotografía. Puede haberse guardado; comprueba las evidencias del incidente antes de reenviarla.',
    )
  }

  return {
    id: evidencia.id,
    incidenteId: identificador,
  }
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
      return 'El servicio no pudo confirmar el almacenamiento de la fotografía. El incidente creado se conserva.'
    default:
      return 'No se pudo confirmar la subida. La fotografía podría haberse guardado; comprueba las evidencias del incidente antes de reenviarla.'
  }
}