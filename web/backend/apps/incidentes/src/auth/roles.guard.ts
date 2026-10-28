import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import type { Request } from 'express';

import { ROLES_KEY, Role } from './roles.decorator';

interface UsuarioAutenticado {
  sub?: string;
  roles?: string[];
}

/**
 * Autorizacion por rol reutilizable (Issue #100).
 * Debe ejecutarse despues de JwtAuthGuard, que es quien carga `request.user`.
 */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const rolesRequeridos = this.reflector.getAllAndOverride<Role[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    const request = context.switchToHttp().getRequest<Request>();
    const usuario = (request as Request & { user?: UsuarioAutenticado }).user;

    if (!usuario?.sub) {
      throw new UnauthorizedException('Usuario no autenticado');
    }

    // Si el endpoint no declara roles explicitos, solo se exige autenticacion.
    if (!rolesRequeridos || rolesRequeridos.length === 0) {
      return true;
    }

    const roles = Array.isArray(usuario.roles) ? usuario.roles : [];
    const autorizado = rolesRequeridos.some((rol) => roles.includes(rol));

    if (!autorizado) {
      throw new ForbiddenException(
        'No tienes permisos suficientes para acceder a este recurso',
      );
    }

    return true;
  }
}
