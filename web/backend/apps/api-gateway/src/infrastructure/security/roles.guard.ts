import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import type { Request } from 'express';

import { ROLES_KEY, Role } from './roles.decorator';
import { ProxyService } from '../../application/services/proxy.service';

interface AuthenticatedUser {
  sub?: string;
  roles?: string[];
}

interface PerfilUsuario {
  id: string;
  roles: string[];
}

const ROLES_POR_DEFECTO: Role[] = [
  Role.ADMIN,
  Role.CIUDADANO,
];

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly proxyService: ProxyService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<Request>();

    const user = (
      request as Request & { user?: AuthenticatedUser }
    ).user;

    if (!user?.sub) {
      throw new UnauthorizedException('Usuario no autenticado');
    }

    const requiredRoles =
      this.reflector.getAllAndOverride<Role[]>(ROLES_KEY, [
        context.getHandler(),
        context.getClass(),
      ]) ?? ROLES_POR_DEFECTO;

    // Obtener la dirección del microservicio auth-usuarios.
    const authServiceUrl = this.proxyService.getServiceUrl('usuarios');

    if (!authServiceUrl) {
      throw new ServiceUnavailableException(
        'Servicio de autenticación no disponible',
      );
    }

    // Consultar los roles actuales del usuario.
    let response: Response;

    try {
      response = await fetch(
        `${authServiceUrl.replace(/\/+$/, '')}/usuarios/me`,
        {
          method: 'GET',
          headers: {
            Authorization: request.headers.authorization ?? '',
          },
          signal: AbortSignal.timeout(5000),
        },
      );
    } catch {
      // Nunca autorizar utilizando los roles antiguos del JWT.
      throw new ServiceUnavailableException(
        'No fue posible verificar los permisos actuales',
      );
    }

    if (response.status === 401 || response.status === 404) {
      throw new UnauthorizedException(
        'Usuario no autenticado o inexistente',
      );
    }

    if (!response.ok) {
      throw new ServiceUnavailableException(
        'No fue posible verificar los permisos actuales',
      );
    }

    let perfil: PerfilUsuario;

    try {
      perfil = (await response.json()) as PerfilUsuario;
    } catch {
      throw new ServiceUnavailableException(
        'Respuesta inválida del servicio de autenticación',
      );
    }

    // Comprobar que el perfil corresponda al usuario del JWT.
    if (
      perfil?.id !== user.sub ||
      !Array.isArray(perfil.roles)
    ) {
      throw new UnauthorizedException(
        'No fue posible validar la identidad del usuario',
      );
    }

    // Utilizar exclusivamente los roles vigentes en PostgreSQL.
    const rolesActuales = perfil.roles;

    const hasPermission = requiredRoles.some(
      (role) => rolesActuales.includes(role),
    );

    if (!hasPermission) {
      throw new ForbiddenException(
        'No tienes permisos suficientes para acceder a este recurso',
      );
    }

    return true;
  }
}
