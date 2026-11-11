import { Role } from '../roles/role.enum';

export interface AuthenticatedUserEntity {
  sub: string;
  email?: string;
  roles: Role[];
}

export interface PerfilUsuarioEntity {
  id: string;
  nombre?: string;
  email?: string;
  roles: string[];
}
