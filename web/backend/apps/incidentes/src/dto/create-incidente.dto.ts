export class CreateUbicacionDto {
  latitud!: number; longitud!: number;
  direccionReferencia!: string; comuna!: string;
  region!: string; sector!: string;
}
export class CreateIncidenteDto {
  titulo!: string; descripcion!: string;
  categoriaId!: string;           // uuid
  usuarioReportaId!: string;      // uuid plano, FK externa a auth
  ubicacion!: CreateUbicacionDto; // se crea anidada
}