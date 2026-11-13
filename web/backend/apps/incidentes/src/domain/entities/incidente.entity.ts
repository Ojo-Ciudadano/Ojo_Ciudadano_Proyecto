export interface CategoriaEntity {
  id: string;
  nombre: string;
  descripcion: string;
  prioridadBase: number;
  esEmergenciaCritica: boolean;
  canalOficialContacto: string | null;
}

export interface UbicacionEntity {
  id: string;
  latitud: number;
  longitud: number;
  direccionReferencia: string;
  comuna: string;
  region: string;
  sector: string;
}

export interface EstadoEntity {
  id: string;
  nombre: string;
  orden: number;
}

export interface EvidenciaEntity {
  id: string;
  incidenteId: string;
  claveArchivo: string;
  tipoMime: string;
  tamanoArchivo: number;
  fechaCreacion: Date;
}

export interface AsignacionEntity {
  id: string;
  incidenteId: string;
  usuarioAsignadoId: string;
  asignadoPorId: string;
  fechaAsignacion: Date;
  activa: boolean;
}

export interface HistorialEstadoEntity {
  id: string;
  incidenteId: string;
  estadoId: string;
  usuarioId: string;
  observacion: string | null;
  fecha: Date;
  estado?: EstadoEntity;
}

export interface IncidenteEntity {
  id: string;
  titulo: string;
  descripcion: string;
  categoriaId: string;
  estadoId: string;
  ubicacionId: string;
  usuarioReportaId: string;
  nivelPrioridad: number | null;
  fechaCreacion: Date;
  fechaActualizacion: Date;
  categoria?: CategoriaEntity;
  estado?: EstadoEntity;
  ubicacion?: UbicacionEntity;
  evidencias?: EvidenciaEntity[];
  asignaciones?: AsignacionEntity[];
  historial?: HistorialEstadoEntity[];
}
