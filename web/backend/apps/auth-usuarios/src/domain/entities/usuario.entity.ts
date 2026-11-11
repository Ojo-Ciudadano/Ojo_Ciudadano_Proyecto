export interface RolEntity {
  id: string;
  nombre: string;
}

export interface UsuarioRolEntity {
  usuarioId: string;
  rolId: string;
  rol?: RolEntity;
}

export interface UsuarioEntity {
  id: string;
  nombre: string;
  email: string;
  passwordHash: string;
  telefono: string | null;
  fechaRegistro: Date;
  roles?: UsuarioRolEntity[];
}
