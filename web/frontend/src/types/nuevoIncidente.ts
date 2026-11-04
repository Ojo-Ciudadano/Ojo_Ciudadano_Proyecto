export interface CategoriaReporte {
  id: string
  nombre: string
  descripcion?: string | null
}

export interface CoordenadasReporte {
  latitud: number
  longitud: number
}

export interface UbicacionNuevoIncidente extends CoordenadasReporte {
  direccionReferencia: string
  comuna: string
  region: string
  sector: string
}

export interface NuevoIncidente {
  titulo: string
  descripcion: string
  categoriaId: string
  usuarioReportaId: string
  ubicacion: UbicacionNuevoIncidente
}

export interface IncidenteCreado {
  id: string
  titulo: string
}

export function sonCoordenadasValidas(
  coordenadas: CoordenadasReporte | null,
): coordenadas is CoordenadasReporte {
  return (
    coordenadas !== null &&
    Number.isFinite(coordenadas.latitud) &&
    coordenadas.latitud >= -90 &&
    coordenadas.latitud <= 90 &&
    Number.isFinite(coordenadas.longitud) &&
    coordenadas.longitud >= -180 &&
    coordenadas.longitud <= 180
  )
}