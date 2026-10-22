
import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import type { Request } from 'express';

interface AuthenticatedUser {
  sub?: string;
  roles?: string[];
}

@Injectable()
export class RolesGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request>();
    const user = (request as Request & { user?: AuthenticatedUser }).user;

    if (!user?.sub) {
      throw new UnauthorizedException('Usuario no autenticado');
    }

    const roles = Array.isArray(user.roles) ? user.roles : [];

    const allowedRoles = ['ADMIN', 'CIUDADANO'];

    const hasPermission = roles.some((role) =>
      allowedRoles.includes(role),
    );

    if (!hasPermission) {
      throw new ForbiddenException(
        'No tienes permisos suficientes para acceder a este recurso',
      );
    }

    return true;
  }
}
