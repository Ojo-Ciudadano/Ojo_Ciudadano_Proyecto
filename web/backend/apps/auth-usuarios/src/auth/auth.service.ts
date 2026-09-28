import { ConflictException, Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { UsuariosService } from '../usuarios/usuarios.service';
import { CreateUsuarioDto } from '../usuarios/dto/create-usuario.dto';
import { LoginDto } from './dto/login.dto';

const SALT_ROUNDS = 10;

@Injectable()
export class AuthService {
  constructor(
    private readonly usuariosService: UsuariosService,
    private readonly jwtService: JwtService,
  ) {}

  async register(dto: CreateUsuarioDto) {
    const existente = await this.usuariosService.findByEmail(dto.email);
    if (existente) {
      throw new ConflictException('Ya existe una cuenta con ese correo');
    }

    const passwordHash = await bcrypt.hash(dto.password, SALT_ROUNDS);

    const usuario = await this.usuariosService.create({
      nombre: dto.nombre,
      email: dto.email,
      passwordHash,
      telefono: dto.telefono,
    });

    const { passwordHash: _omit, ...usuarioSinPassword } = usuario;
    return usuarioSinPassword;
  }

  async login(dto: LoginDto) {
    const usuario = await this.usuariosService.findByEmail(dto.email);
    if (!usuario || !(await bcrypt.compare(dto.password, usuario.passwordHash))) {
      throw new UnauthorizedException('Credenciales invalidas');
    }

    const accessToken = await this.jwtService.signAsync({
      sub: usuario.id,
      email: usuario.email,
    });

    return { access_token: accessToken, token_type: 'Bearer' };
  }
}