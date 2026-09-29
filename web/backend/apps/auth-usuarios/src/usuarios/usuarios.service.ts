import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

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

  async findByEmail(email: string) {
    return this.prisma.usuario.findUnique({ where: { email } });
  }
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
      data: { usuarioId: usuario.id, rolId: rol.id },
    });

    return usuario;
  }
}