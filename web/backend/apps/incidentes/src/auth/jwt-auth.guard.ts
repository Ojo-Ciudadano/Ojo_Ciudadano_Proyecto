
import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
  ForbiddenException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import type { Request } from 'express';

interface JwtPayload {
  sub: string;
  email?: string;
  roles: string[];
}

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly jwtService: JwtService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<Request>();

    const [scheme, token] =
      request.headers.authorization?.split(' ') ?? [];

    if (scheme !== 'Bearer' || !token) {
      throw new UnauthorizedException('Se requiere un token Bearer válido');
    }

    let payload: JwtPayload;

    try {
      payload = await this.jwtService.verifyAsync<JwtPayload>(token);
    } catch {
      throw new UnauthorizedException('Token inválido o expirado');
    }

    if (!payload?.sub || typeof payload.sub !== 'string') {
      throw new UnauthorizedException('Token sin usuario válido');
    }

    const roles = Array.isArray(payload.roles) ? payload.roles : [];

    if (!roles.some((rol) => ['ADMIN', 'CIUDADANO'].includes(rol))) {
      throw new ForbiddenException(
        'No tienes permisos suficientes para acceder a este recurso',
      );
    }

    (request as Request & { user?: JwtPayload }).user = payload;

    return true;
  }
}
