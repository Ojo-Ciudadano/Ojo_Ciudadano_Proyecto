import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import { LoginDto } from '../dto/login.dto';
import { CreateUsuarioDto } from '../usuarios/dto/create-usuario.dto';
import * as bcrypt from 'bcrypt';

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {}

  // 1. Método para registrar un nuevo usuario
  async register(dto: CreateUsuarioDto) {
    const { email, password, nombre } = dto;

    // Verificar si el usuario ya existe
    const existingUser = await this.prisma.usuario.findUnique({
      where: { email },
    });

    if (existingUser) {
      throw new ConflictException('El correo ya está registrado');
    }

    // Encriptar contraseña
    const hashedPassword = await bcrypt.hash(password, 10);

    // Crear el usuario en la base de datos
    const nuevoUsuario = await this.prisma.usuario.create({
      data: {
        email,
        nombre,
        passwordHash: hashedPassword,
      },
    });

    return {
      message: 'Usuario registrado exitosamente',
      usuario: {
        id: nuevoUsuario.id,
        email: nuevoUsuario.email,
        nombre: nuevoUsuario.nombre,
      },
    };
  }

  // 2. Método de Login con Roles incluidos en el Payload del Token
  async login(loginDto: LoginDto) {
    const { email, password } = loginDto;

    // Buscar usuario por correo e incluir sus roles
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

    // Validar contraseña
    const isPasswordValid = await bcrypt.compare(password, usuario.passwordHash);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Credenciales inválidas');
    }

    // Extraer lista de roles (ej: ['ADMIN'] o ['CIUDADANO'])
    const listaRoles = usuario.roles.map((r) => r.rol.nombre);

    const payload = {
      sub: usuario.id,
      email: usuario.email,
      roles: listaRoles,
    };

    return {
      access_token: await this.jwtService.signAsync(payload),
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        email: usuario.email,
        roles: listaRoles,
      },
    };
  }
}