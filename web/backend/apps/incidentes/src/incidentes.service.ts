import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from './prisma/prisma.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';

const include = { categoria: true, estado: true, ubicacion: true };

@Injectable()
export class IncidentesService {
  constructor(private readonly prisma: PrismaService) {}

  findAll() {
    return this.prisma.incidente.findMany({ include, orderBy: { fechaCreacion: 'desc' } });
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
    if (!categoria) throw new NotFoundException('Categoria no existe');
    const estado = await this.prisma.estado.findFirst({ where: { nombre: 'Nuevo' } });
    if (!estado) throw new BadRequestException('Falta seed de estados');
    return this.prisma.$transaction(async (tx) => {
      const ubicacion = await tx.ubicacion.create({ data: dto.ubicacion });
      const incidente = await tx.incidente.create({
        data: { titulo: dto.titulo, descripcion: dto.descripcion,
          categoriaId: dto.categoriaId, estadoId: estado.id,
          ubicacionId: ubicacion.id, usuarioReportaId: dto.usuarioReportaId },
        include,
      });
      await tx.historialEstado.create({
        data: { incidenteId: incidente.id, estadoId: estado.id,
          usuarioId: dto.usuarioReportaId, observacion: 'Reporte creado' },
      });
      return incidente;
    });
  }
}