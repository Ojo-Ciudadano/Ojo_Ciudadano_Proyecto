import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import type { Request } from 'express';

export interface UsuarioAutenticado {
  sub: string;
  email?: string;
  roles?: string[];
}

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly jwtService: JwtService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<
      Request & { user?: UsuarioAutenticado }
    >();

    const [scheme, token] =
      request.headers.authorization?.split(' ') ?? [];

    if (scheme !== 'Bearer' || !token) {
      throw new UnauthorizedException(
        'Se requiere un token Bearer valido',
      );
    }

    let payload: UsuarioAutenticado;

    try {
      payload = await this.jwtService.verifyAsync<UsuarioAutenticado>(token);
    } catch {
      throw new UnauthorizedException(
        'Token invalido o expirado',
      );
    }

    if (!payload?.sub || typeof payload.sub !== 'string') {
      throw new UnauthorizedException(
        'Token sin usuario valido',
      );
    }

    request.user = payload;

    return true;
  }
}