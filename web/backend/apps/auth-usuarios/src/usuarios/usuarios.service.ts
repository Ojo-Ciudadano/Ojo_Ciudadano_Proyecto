
import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ListarUsuariosDto } from './dto/listar-usuarios.dto';
import { Role } from '../auth/decorators/roles.decorator';

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

    const skip = (page - 1) * limit;

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

    const data = usuarios.map((usuario) => ({
      id: usuario.id,
      nombre: usuario.nombre,
      email: usuario.email,
      telefono: usuario.telefono,
      fechaRegistro: usuario.fechaRegistro,
      roles: usuario.roles.map((relacion) => relacion.rol.nombre),
    }));

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

    if (!usuario) {
      throw new NotFoundException('Usuario no encontrado.');
    }

    return {
      id: usuario.id,
      nombre: usuario.nombre,
      email: usuario.email,
      telefono: usuario.telefono,
      fechaRegistro: usuario.fechaRegistro,
      roles: usuario.roles.map((relacion) => relacion.rol.nombre),
    };
  }

  // Cambiar el rol de un usuario (Issue #122)
  async actualizarRolUsuario(
    usuarioId: string,
    nuevoRol: Role,
    administradorId: string,
  ) {
    // Ejecutar la actualización dentro de una transacción
    return this.prisma.$transaction(async (tx) => {
      // Buscar al usuario y sus roles actuales
      const usuario = await tx.usuario.findUnique({
        where: { id: usuarioId },
        select: {
          id: true,
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

      if (!usuario) {
        throw new NotFoundException('Usuario no encontrado.');
      }

      const rolesActuales = usuario.roles.map(
        (relacion) => relacion.rol.nombre,
      );

      // Impedir que un administrador se quite su propio rol ADMIN
      if (
        usuarioId === administradorId &&
        nuevoRol === Role.CIUDADANO &&
        rolesActuales.includes(Role.ADMIN)
      ) {
        throw new ForbiddenException(
          'No puedes quitarte tu propio rol de administrador.',
        );
      }

      // Si ya tiene exactamente el rol solicitado, no modificar nada
      if (
        rolesActuales.length === 1 &&
        rolesActuales[0] === nuevoRol
      ) {
        return {
          id: usuario.id,
          roles: [nuevoRol],
        };
      }

      // Buscar o crear el rol solicitado
      const rol = await tx.rol.upsert({
        where: { nombre: nuevoRol },
        update: {},
        create: { nombre: nuevoRol },
      });

      // Eliminar los roles anteriores
      await tx.usuarioRol.deleteMany({
        where: { usuarioId },
      });

      // Asignar únicamente el nuevo rol
      await tx.usuarioRol.create({
        data: {
          usuarioId,
          rolId: rol.id,
        },
      });

      // Respuesta sin información sensible
      return {
        id: usuario.id,
        roles: [nuevoRol],
      };
    });
  }
}
