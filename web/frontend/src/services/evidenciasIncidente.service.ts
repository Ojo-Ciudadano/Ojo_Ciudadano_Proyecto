import { apiClient } from './api'
import type { EvidenciaIncidente } from '../types/evidenciaIncidente'

const TIPOS_IMAGEN = [
  'image/jpeg',
  'image/png',
  'image/webp',
] as const

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null
}

function esTipoImagen(valor: unknown): valor is string {
  return (
    typeof valor === 'string' &&
    (TIPOS_IMAGEN as readonly string[]).includes(valor)
  )
}

/**
 * Solo acepta referencias a las rutas públicas del backend.
 * Nunca envía el JWT a una URL externa entregada en el payload.
 */
export function obtenerRutaArchivoEvidencia(
  evidencia: EvidenciaIncidente,
): string {
  const id = encodeURIComponent(evidencia.id)
  const incidenteId = encodeURIComponent(evidencia.incidenteId)

  const rutaSimple = `/evidencias/${id}/archivo`
  const rutaAnidada =
    `/incidentes/${incidenteId}/evidencias/${id}/archivo`

  if (
    evidencia.url !== rutaSimple &&
    evidencia.url !== rutaAnidada
  ) {
    throw new Error(
      'La referencia de la fotografía no corresponde a una ruta válida del backend.',
    )
  }

  return evidencia.url
}

export function normalizarEvidenciasIncidente(
  payload: unknown,
  incidenteId: string,
): EvidenciaIncidente[] {
  if (!Array.isArray(payload)) {
    throw new Error(
      'El servidor devolvió una colección de evidencias inválida.',
    )
  }

  const identificadores = new Set<string>()

  return payload.map((valor: unknown) => {
    if (
      !esObjeto(valor) ||
      typeof valor.id !== 'string' ||
      !valor.id.trim() ||
      valor.incidenteId !== incidenteId ||
      !esTipoImagen(valor.tipoMime) ||
      typeof valor.tamanoArchivo !== 'number' ||
      !Number.isSafeInteger(valor.tamanoArchivo) ||
      valor.tamanoArchivo <= 0 ||
      typeof valor.fechaCreacion !== 'string' ||
      Number.isNaN(Date.parse(valor.fechaCreacion)) ||
      typeof valor.url !== 'string'
    ) {
      throw new Error(
        'El servidor devolvió datos de evidencia inválidos para este incidente.',
      )
    }

    if (identificadores.has(valor.id)) {
      throw new Error(
        'El servidor devolvió identificadores de evidencia duplicados.',
      )
    }

    const evidencia: EvidenciaIncidente = {
      id: valor.id,
      incidenteId,
      tipoMime: valor.tipoMime,
      tamanoArchivo: valor.tamanoArchivo,
      fechaCreacion: valor.fechaCreacion,
      url: valor.url,
    }

    obtenerRutaArchivoEvidencia(evidencia)
    identificadores.add(evidencia.id)

    return evidencia
  })
}

export async function obtenerArchivoEvidencia(
  evidencia: EvidenciaIncidente,
  signal?: AbortSignal,
): Promise<Blob> {
  const ruta = obtenerRutaArchivoEvidencia(evidencia)

  const response = await apiClient.get<Blob>(ruta, {
    responseType: 'blob',
    signal,
  })

  const archivo = response.data

  if (!(archivo instanceof Blob) || archivo.size === 0) {
    throw new Error(
      'No se recibió un archivo de imagen válido.',
    )
  }

  const tipoMime = archivo.type
    .split(';')[0]
    .trim()
    .toLowerCase()

  if (!esTipoImagen(tipoMime)) {
    throw new Error(
      'El archivo recibido no tiene un formato de imagen admitido.',
    )
  }

  return archivo
}

export function interpretarErrorEvidencia(error: unknown): {
  mensaje: string
  permiteReintentar: boolean
} {
  const codigo =
    esObjeto(error) && 'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  switch (codigo) {
    case 401:
      return {
        mensaje: 'Tu sesión ya no es válida. Inicia sesión nuevamente.',
        permiteReintentar: false,
      }
    case 403:
      return {
        mensaje: 'No tienes permisos para consultar esta fotografía.',
        permiteReintentar: false,
      }
    case 404:
      return {
        mensaje: 'La fotografía ya no está disponible.',
        permiteReintentar: false,
      }
    default:
      return {
        mensaje:
          'No se pudo cargar la fotografía. Puedes reintentar.',
        permiteReintentar: true,
      }
  }
}