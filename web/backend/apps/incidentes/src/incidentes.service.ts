import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { isUUID } from 'class-validator';

import { PrismaService } from './prisma/prisma.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';
import { UpdatePrioridadDto } from './dto/update-prioridad.dto';
import {
  esPrioridadValida,
  PRIORIDAD_MAXIMA,
  PRIORIDAD_MINIMA,
} from './prioridad/prioridad.constants';

const include = { categoria: true, estado: true, ubicacion: true };

@Injectable()
export class IncidentesService {
  constructor(private readonly prisma: PrismaService) {}

  findAll(usuarioReportaId?: string) {
    return this.prisma.incidente.findMany({
      where: usuarioReportaId ? { usuarioReportaId } : undefined,
      include,
      orderBy: { fechaCreacion: 'desc' },
    });
  }

  async findOne(id: string) {
    const incidente = await this.prisma.incidente.findUnique({ where: { id }, include });
    if (!incidente) throw new NotFoundException(`Incidente ${id} no encontrado`);
    return incidente;
  }

  async create(dto: CreateIncidenteDto) {
    if (!dto.titulo?.trim() || !dto.descripcion?.trim() || !dto.ubicacion)
      throw new BadRequestException('titulo, descripcion y ubicacion son obligatorios');

    const categoria = await this.prisma.categoria.findUnique({ where: { id: dto.categoriaId } });
    if (!categoria) throw new BadRequestException('La categoria indicada no existe');
    if (!esPrioridadValida(categoria.prioridadBase))
      throw new BadRequestException(
        `La categoria no tiene una prioridad base valida (debe ser un entero entre ${PRIORIDAD_MINIMA} y ${PRIORIDAD_MAXIMA})`,
      );

    const estado = await this.prisma.estado.findFirst({ where: { nombre: 'Nuevo' } });
    if (!estado) throw new BadRequestException('Falta seed de estados');

    return this.prisma.$transaction(async (tx) => {
      const ubicacion = await tx.ubicacion.create({ data: dto.ubicacion });
      const incidente = await tx.incidente.create({
        data: {
          titulo: dto.titulo,
          descripcion: dto.descripcion,
          categoriaId: dto.categoriaId,
          estadoId: estado.id,
          ubicacionId: ubicacion.id,
          usuarioReportaId: dto.usuarioReportaId,
          // La prioridad inicial siempre se calcula desde la categoria; cualquier valor
          // de prioridad enviado por el cliente se ignora.
          nivelPrioridad: categoria.prioridadBase,
        },
        include,
      });
      await tx.historialEstado.create({
        data: {
          incidenteId: incidente.id,
          estadoId: estado.id,
          usuarioId: dto.usuarioReportaId,
          observacion: 'Reporte creado',
        },
      });
      return incidente;
    });
  }

  async updatePrioridad(id: string, dto: UpdatePrioridadDto) {
    if (!isUUID(id, 'all'))
      throw new BadRequestException('El id del incidente debe ser un UUID valido');

    if (!esPrioridadValida(dto?.nivelPrioridad))
      throw new BadRequestException(
        `El nivel de prioridad debe ser un entero entre ${PRIORIDAD_MINIMA} y ${PRIORIDAD_MAXIMA}`,
      );

    const existente = await this.prisma.incidente.findUnique({
      where: { id },
      select: { id: true },
    });
    if (!existente) throw new NotFoundException(`Incidente ${id} no encontrado`);

    // Solo se actualiza nivelPrioridad: categoria, estado y asignaciones quedan intactas.
    return this.prisma.incidente.update({
      where: { id },
      data: { nivelPrioridad: dto.nivelPrioridad },
      include,
    });
  }
}
