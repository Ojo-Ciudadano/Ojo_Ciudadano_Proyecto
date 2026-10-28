import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ListarUsuariosDto } from './dto/listar-usuarios.dto';

interface CrearUsuarioConHash {
  nombre: string;
  email: string;
  passwordHash: string;
  telefono?: string;
}

const ROL_POR_DEFECTO = 'CIUDADANO';

@Injectable()
export class UsuariosService {
  constructor(private readonly prisma: PrismaService) {}

  // Buscar un usuario por su correo electrónico
  async findByEmail(email: string) {
    return this.prisma.usuario.findUnique({
      where: { email },
    });
  }

  // Crear un usuario y asignarle el rol CIUDADANO
  async create(datos: CrearUsuarioConHash) {
    const usuario = await this.prisma.usuario.create({
      data: {
        nombre: datos.nombre,
        email: datos.email,
        passwordHash: datos.passwordHash,
        telefono: datos.telefono,
      },
    });

    const rol = await this.prisma.rol.upsert({
      where: { nombre: ROL_POR_DEFECTO },
      update: {},
      create: { nombre: ROL_POR_DEFECTO },
    });

    await this.prisma.usuarioRol.create({
      data: {
        usuarioId: usuario.id,
        rolId: rol.id,
      },
    });

    return usuario;
  }

  // Listar usuarios con paginación y filtro opcional por rol (Issue #121)
  async listarUsuarios(filtros: ListarUsuariosDto) {
    const page = filtros.page ?? 1;
    const limit = filtros.limit ?? 10;

    // Calcular cuántos registros se deben omitir
    const skip = (page - 1) * limit;

    // Aplicar el filtro por rol solamente cuando se proporciona
    const where = filtros.rol
      ? {
          roles: {
            some: {
              rol: {
                nombre: filtros.rol,
              },
            },
          },
        }
      : {};

    // Obtener usuarios y contar el total que cumple el filtro
    const [usuarios, total] = await this.prisma.$transaction([
      this.prisma.usuario.findMany({
        where,
        skip,
        take: limit,
        orderBy: [
          { fechaRegistro: 'desc' },
          { id: 'asc' },
        ],
        select: {
          id: true,
          nombre: true,
          email: true,
          telefono: true,
          fechaRegistro: true,
          roles: {
            select: {
              rol: {
                select: {
                  nombre: true,
                },
              },
            },
          },
        },
      }),

      this.prisma.usuario.count({
        where,
      }),
    ]);

    // Preparar la respuesta sin incluir passwordHash
    const data = usuarios.map((usuario) => ({
      id: usuario.id,
      nombre: usuario.nombre,
      email: usuario.email,
      telefono: usuario.telefono,
      fechaRegistro: usuario.fechaRegistro,
      roles: usuario.roles.map((relacion) => relacion.rol.nombre),
    }));

    // Devolver usuarios y metadatos de paginación
    return {
      data,
      meta: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  // Obtener el perfil del usuario autenticado (Issue #118)
  async obtenerMiPerfil(usuarioId: string) {
    // Buscar el usuario utilizando el ID obtenido del JWT
    const usuario = await this.prisma.usuario.findUnique({
      where: {
        id: usuarioId,
      },
      select: {
        id: true,
        nombre: true,
        email: true,
        telefono: true,
        fechaRegistro: true,
        roles: {
          select: {
            rol: {
              select: {
                nombre: true,
              },
            },
          },
        },
      },
    });

    // Si el usuario ya no existe, devolver un error controlado
    if (!usuario) {
      throw new NotFoundException('Usuario no encontrado.');
    }

    // Devolver el perfil con los roles vigentes en la base de datos
    return {
      id: usuario.id,
      nombre: usuario.nombre,
      email: usuario.email,
      telefono: usuario.telefono,
      fechaRegistro: usuario.fechaRegistro,
      roles: usuario.roles.map((relacion) => relacion.rol.nombre),
    };
  }
}