import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { isUUID } from 'class-validator';

import {
  CreateNotificacionDto,
  NotificacionResponseDto,
} from '../dto/create-notificacion.dto';
import { PrismaService } from '../../infrastructure/persistence/prisma/prisma.service';

@Injectable()
export class NotificacionesService {
  private readonly logger = new Logger(NotificacionesService.name);

  constructor(private readonly prisma: PrismaService) {}

  getHello(): string {
    return 'Hello World!';
  }
  // Issue #114: obtener las notificaciones del usuario autenticado
async obtenerMisNotificaciones(usuarioId: string) {
  if (!isUUID(usuarioId, 'all')) {
    throw new BadRequestException(
      'El id de usuario debe ser un UUID valido',
    );
  }

  try {
    return await this.prisma.notificacion.findMany({
      where: {
        usuarioId,
      },
      orderBy: [
        { fecha: 'desc' },
        { id: 'desc' },
      ],
    });
  } catch (error) {
    const detalle =
      error instanceof Error ? error.message : 'Error desconocido';

    this.logger.error(
      `Error al consultar notificaciones: ${detalle}`,
    );

    throw new InternalServerErrorException(
      'No se pudieron obtener las notificaciones',
    );
  }
}

  async marcarLeida(
    id: string,
    usuarioId: string,
  ): Promise<NotificacionResponseDto> {
    if (!isUUID(id, 'all') || !isUUID(usuarioId, 'all')) {
      throw new BadRequestException('El id debe ser un UUID válido');
    }

    const propia = await this.prisma.notificacion.findFirst({
      where: { id, usuarioId },
    });
    if (!propia) {
      throw new ForbiddenException('No tiene permiso sobre esa notificación');
    }

    return this.prisma.notificacion.update({
      where: { id },
      data: { leido: true },
    });
  }

  async marcarTodasLeidas(usuarioId: string): Promise<{ marcadas: number }> {
    if (!isUUID(usuarioId, 'all')) {
      throw new BadRequestException('El usuario debe ser un UUID válido');
    }

    const resultado = await this.prisma.notificacion.updateMany({
      where: { usuarioId, leido: false },
      data: { leido: true },
    });
    return { marcadas: resultado.count };
  }

  async create(dto: CreateNotificacionDto) {
    if (!isUUID(dto?.usuarioId, 'all')) {
      throw new BadRequestException(
        'El id de usuario debe ser un UUID valido',
      );
    }

    if (!isUUID(dto?.incidenteId, 'all')) {
      throw new BadRequestException(
        'El id de incidente debe ser un UUID valido',
      );
    }

    if (typeof dto?.mensaje !== 'string' || !dto.mensaje.trim()) {
      throw new BadRequestException('El mensaje de notificacion es requerido');
    }

    const mensaje = dto.mensaje.trim();

    try {
      return await this.prisma.notificacion.create({
        data: {
          usuarioId: dto.usuarioId,
          incidenteId: dto.incidenteId,
          mensaje,
        },
      });
    } catch (error) {
      const detalle =
        error instanceof Error ? error.message : 'Error desconocido';
      this.logger.error(
        `Error al persistir notificacion para incidente ${dto.incidenteId} y usuario ${dto.usuarioId}: ${detalle}`,
      );
      throw new InternalServerErrorException(
        'No se pudo persistir la notificacion',
      );
    }
  }
}
