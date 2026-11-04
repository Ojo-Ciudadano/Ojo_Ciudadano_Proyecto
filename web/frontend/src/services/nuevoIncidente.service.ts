import { apiClient } from './api'
import {
  sonCoordenadasValidas,
  type CategoriaReporte,
  type IncidenteCreado,
  type NuevoIncidente,
} from '../types/nuevoIncidente'

export async function obtenerCategoriasReporte(
  signal?: AbortSignal,
): Promise<CategoriaReporte[]> {
  const response = await apiClient.get<CategoriaReporte[]>(
    '/categorias',
    { signal },
  )

  const categorias = response.data

  if (
    !Array.isArray(categorias) ||
    !categorias.every(
      (categoria) =>
        categoria !== null &&
        typeof categoria === 'object' &&
        typeof categoria.id === 'string' &&
        categoria.id.trim().length > 0 &&
        typeof categoria.nombre === 'string' &&
        categoria.nombre.trim().length > 0,
    )
  ) {
    throw new Error(
      'El servidor devolvió una lista de categorías inválida.',
    )
  }

  return categorias
}

export async function crearIncidente(
  datos: NuevoIncidente,
): Promise<IncidenteCreado> {
  if (!sonCoordenadasValidas(datos.ubicacion)) {
    throw new Error(
      'Debes obtener una ubicación válida antes de enviar el reporte.',
    )
  }

  const cuerpo: NuevoIncidente = {
    titulo: datos.titulo.trim(),
    descripcion: datos.descripcion.trim(),
    categoriaId: datos.categoriaId.trim(),
    usuarioReportaId: datos.usuarioReportaId.trim(),
    ubicacion: {
      latitud: datos.ubicacion.latitud,
      longitud: datos.ubicacion.longitud,
      direccionReferencia:
        datos.ubicacion.direccionReferencia.trim(),
      comuna: datos.ubicacion.comuna.trim(),
      region: datos.ubicacion.region.trim(),
      sector: datos.ubicacion.sector.trim(),
    },
  }

  const camposObligatorios = [
    cuerpo.titulo,
    cuerpo.descripcion,
    cuerpo.categoriaId,
    cuerpo.usuarioReportaId,
    cuerpo.ubicacion.direccionReferencia,
    cuerpo.ubicacion.comuna,
    cuerpo.ubicacion.region,
    cuerpo.ubicacion.sector,
  ]

  if (camposObligatorios.some((campo) => !campo)) {
    throw new Error('Completa todos los campos obligatorios.')
  }

  const response = await apiClient.post<IncidenteCreado>(
    '/incidentes',
    cuerpo,
  )

  const incidente = response.data

  if (
    !incidente ||
    typeof incidente !== 'object' ||
    typeof incidente.id !== 'string' ||
    !incidente.id.trim()
  ) {
    throw new Error(
      'No se pudo verificar el identificador del reporte creado. Comprueba tus reportes antes de volver a enviarlo.',
    )
  }

  return incidente
}
