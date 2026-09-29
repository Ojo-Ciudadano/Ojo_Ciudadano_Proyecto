import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { UsuariosService } from '../usuarios/usuarios.service';
import { LoginDto } from '../dto/login.dto';
import * as bcrypt from 'bcrypt';

@Injectable()
export class AuthService {
  constructor(
    private readonly usuariosService: UsuariosService,
    private readonly jwtService: JwtService,
  ) {}

  async login(loginDto: LoginDto) {
    const { email, password } = loginDto;

    const usuario = await this.usuariosService.findByEmail(email);
    if (!usuario) {
      throw new UnauthorizedException('Credenciales inválidas');
    }

    const isPasswordValid = await bcrypt.compare(password, usuario.passwordHash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Credenciales inválidas');
    }

    const usuarioConRoles = usuario as typeof usuario & {
      roles?: Array<{ rol?: { nombre: string } }>;
    };

    const roles = Array.isArray(usuarioConRoles.roles)
      ? usuarioConRoles.roles.map((r) => r.rol?.nombre ?? 'CIUDADANO')
      : ['CIUDADANO'];

    const payload = {
      sub: usuario.id,
      email: usuario.email,
      roles,
    };

    const accessToken = await this.jwtService.signAsync(payload);

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        email: usuario.email,
        roles,
      },
      user: {
        id: usuario.id,
        nombre: usuario.nombre,
        email: usuario.email,
        roles,
      },
    };
  }
}
