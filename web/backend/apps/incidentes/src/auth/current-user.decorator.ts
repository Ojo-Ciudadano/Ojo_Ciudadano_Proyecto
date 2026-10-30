import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import type { Request } from 'express';

export interface UsuarioAutenticado {
  sub: string;
  email?: string;
  roles?: string[];
}

/**
 * Expone la identidad cargada por JwtAuthGuard desde el token verificado.
 * La identidad del actor nunca debe tomarse del cuerpo de la peticion.
 */
export const CurrentUser = createParamDecorator(
  (_data: unknown, context: ExecutionContext): UsuarioAutenticado | undefined =>
    context.switchToHttp().getRequest<Request & { user?: UsuarioAutenticado }>()
      .user,
);
