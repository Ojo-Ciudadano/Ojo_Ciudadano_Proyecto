import { ConflictException, Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import bcrypt from 'bcryptjs';
import { PrismaService } from '../prisma/prisma.service';
import { UsuariosService } from '../usuarios/usuarios.service';
import { CreateUsuarioDto } from '../usuarios/dto/create-usuario.dto';
import { LoginDto } from '../dto/login.dto';

const SALT_ROUNDS = 10;

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
    private readonly usuariosService: UsuariosService,
  ) {}

  async register(dto: CreateUsuarioDto) {
    const existente = await this.usuariosService.findByEmail(dto.email);
    if (existente) {
      throw new ConflictException('Ya existe una cuenta con ese correo');
    }

    const passwordHash = bcrypt.hashSync(dto.password, SALT_ROUNDS);

    const usuario = await this.usuariosService.create({
      nombre: dto.nombre,
      email: dto.email,
      passwordHash,
      telefono: dto.telefono,
    });

    const { passwordHash: _omit, ...usuarioSinPassword } = usuario;
    return usuarioSinPassword;
  }

  async login(loginDto: LoginDto) {
    const { email, password } = loginDto;

    const usuario = await this.prisma.usuario.findUnique({
      where: { email },
      include: {
        roles: {
          include: {
            rol: true,
          },
        },
      },
    });

    if (!usuario) {
      throw new UnauthorizedException('Credenciales inválidas');
    }

    const isPasswordValid = await bcrypt.compare(password, usuario.passwordHash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Credenciales inválidas');
    }

    const listaRoles = usuario.roles.map((r) => r.rol.nombre);

    const payload = {
      sub: usuario.id,
      email: usuario.email,
      roles: listaRoles,
    };

    return {
      access_token: await this.jwtService.signAsync(payload),
      token_type: 'Bearer',
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        email: usuario.email,
        roles: listaRoles,
      },
    };
  }
}