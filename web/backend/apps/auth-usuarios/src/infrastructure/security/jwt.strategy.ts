import {
  Injectable,
  UnauthorizedException,
  NotFoundException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { UsuariosService } from '../../application/services/usuarios.service';

export interface JwtPayload {
  sub: string;
  email: string;
  roles: string[];
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy, 'jwt') {
  constructor(
    configService: ConfigService,
    private readonly usuariosService: UsuariosService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey:
        configService.get<string>('JWT_SECRET') ||
        'clave_secreta_super_segura_ojo_ciudadano',
    });
  }

  async validate(payload: JwtPayload) {
    if (!payload?.sub || typeof payload.sub !== 'string') {
      throw new UnauthorizedException('Token inválido');
    }

    // Obtener los roles actuales desde PostgreSQL.
    let usuario;

    try {
      usuario = await this.usuariosService.obtenerMiPerfil(payload.sub);
    } catch (error) {
      // Si el usuario fue eliminado, el JWT ya no debe autenticarlo.
      if (error instanceof NotFoundException) {
        throw new UnauthorizedException('Usuario no válido');
      }

      throw error;
    }

    return {
      userId: usuario.id,
      email: usuario.email,
      roles: usuario.roles,
    };
  }
}
