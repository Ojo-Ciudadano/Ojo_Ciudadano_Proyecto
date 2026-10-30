import {
  BadRequestException,
  Injectable,
  InternalServerErrorException,
  Logger,
} from '@nestjs/common';
import { isUUID } from 'class-validator';

import { CreateNotificacionDto } from './dto/create-notificacion.dto';
import { PrismaService } from './prisma/prisma.service';

@Injectable()
export class NotificacionesService {
  private readonly logger = new Logger(NotificacionesService.name);

  constructor(private readonly prisma: PrismaService) {}

  getHello(): string {
    return 'Hello World!';
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
