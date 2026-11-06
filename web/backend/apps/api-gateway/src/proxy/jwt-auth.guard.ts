import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import type { Request } from 'express';

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly jwtService: JwtService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<Request>();
    const [scheme, token] = request.headers.authorization?.split(' ') ?? [];

    if (scheme !== 'Bearer' || !token) {
      throw new UnauthorizedException('Se requiere un token Bearer valido');
    }

   try {
  const payload = await this.jwtService.verifyAsync<{
    sub?: string;
    email?: string;
    roles?: string[];
  }>(token);

  if (!payload?.sub || typeof payload.sub !== 'string') {
    throw new UnauthorizedException('Token sin usuario válido');
  }

  (request as Request & { user?: typeof payload }).user = payload;
  return true;
} catch {
  throw new UnauthorizedException('Token inválido o expirado');
}
  }
}