
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

interface AuthenticatedUser {
  sub?: string;
  roles?: string[];
}

const ROLES_POR_DEFECTO: Role[] = [Role.ADMIN, Role.CIUDADANO];

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request>();
    const user = (request as Request & { user?: AuthenticatedUser }).user;

    if (!user?.sub) {
      throw new UnauthorizedException('Usuario no autenticado');
    }

    // Los endpoints sin @Roles() mantienen el comportamiento general: cualquier
    // usuario autenticado con rol ADMIN o CIUDADANO. Los endpoints restringidos
    // declaran explicitamente el rol requerido.
    const requiredRoles =
      this.reflector.getAllAndOverride<Role[]>(ROLES_KEY, [
        context.getHandler(),
        context.getClass(),
      ]) ?? ROLES_POR_DEFECTO;

    const roles = Array.isArray(user.roles) ? user.roles : [];

    const hasPermission = requiredRoles.some((role) => roles.includes(role));

    if (!hasPermission) {
      throw new ForbiddenException(
        'No tienes permisos suficientes para acceder a este recurso',
      );
    }

    return true;
  }
}
