import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
  Optional,
  UnauthorizedException,
} from '@nestjs/common';
import { isUUID } from 'class-validator';

import { PrismaService } from './prisma/prisma.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';
import { UpdateEstadoDto } from './dto/update-estado.dto';
import { UpdatePrioridadDto } from './dto/update-prioridad.dto';
import {
  esEstadoIncidente,
  esTransicionPermitida,
} from './estado/transiciones-incidente.constants';
import {
  esPrioridadValida,
  PRIORIDAD_MAXIMA,
  PRIORIDAD_MINIMA,
} from './prioridad/prioridad.constants';
import { evidenciaSelect } from './mappers/evidencia.mapper';
import { toIncidenteResponse } from './mappers/incidente.mapper';
import { IncidentesNotificacionesClient } from './notificaciones/incidentes-notificaciones.client';

const include = {
  categoria: true,
  estado: true,
  ubicacion: true,
  evidencias: { select: evidenciaSelect, orderBy: { fechaCreacion: 'asc' as const } },
};
@Injectable()
export class IncidentesService {
  constructor(
    private readonly prisma: PrismaService,
    @Optional()
    private readonly notificacionesClient?: IncidentesNotificacionesClient,
  ) {}

  async findAll(usuarioReportaId?: string) {
    const incidentes = await this.prisma.incidente.findMany({
      where: usuarioReportaId ? { usuarioReportaId } : undefined,
      include,
      orderBy: { fechaCreacion: 'desc' },
    });
    return incidentes.map(toIncidenteResponse);
  }

  async findOne(id: string) {
    const incidente = await this.prisma.incidente.findUnique({ where: { id }, include });
    if (!incidente) throw new NotFoundException(`Incidente ${id} no encontrado`);
    return toIncidenteResponse(incidente);
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
      return toIncidenteResponse(incidente);
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
    const incidente = await this.prisma.incidente.update({
      where: { id },
      data: { nivelPrioridad: dto.nivelPrioridad },
      include,
    });

    return toIncidenteResponse(incidente);
  }

  async updateEstado(id: string, dto: UpdateEstadoDto, usuarioId: string) {
    if (!isUUID(id, 'all'))
      throw new BadRequestException('El id del incidente debe ser un UUID valido');

    if (!esEstadoIncidente(dto?.estado))
      throw new BadRequestException('El estado solicitado no es valido');

    if (typeof dto?.observacion !== 'string' || !dto.observacion.trim())
      throw new BadRequestException('La observacion es obligatoria');

    if (!isUUID(usuarioId, 'all'))
      throw new UnauthorizedException('Usuario autenticado no identificado');

    const observacion = dto.observacion.trim();

    // La lectura del estado actual, la validacion de la transicion y las dos
    // escrituras ocurren en la misma transaccion. El bloqueo FOR UPDATE evita
    // que dos solicitudes concurrentes validen contra el mismo estado de origen.
    const { actualizado, estadoAnterior, usuarioReportaId, titulo } =
      await this.prisma.$transaction(async (tx) => {
        const filas = await tx.$queryRaw<Array<{ id: string }>>`
          SELECT id FROM incidente WHERE id = ${id}::uuid FOR UPDATE
        `;

        if (filas.length === 0)
          throw new NotFoundException(`Incidente ${id} no encontrado`);

        const incidente = await tx.incidente.findUnique({
          where: { id },
          select: {
            id: true,
            titulo: true,
            usuarioReportaId: true,
            estado: { select: { nombre: true } },
          },
        });
        if (!incidente)
          throw new NotFoundException(`Incidente ${id} no encontrado`);

        const destino = await tx.estado.findUnique({
          where: { nombre: dto.estado },
        });
        if (!destino)
          throw new NotFoundException(`Estado ${dto.estado} no encontrado`);

        const estadoActual = incidente.estado?.nombre;
        if (!esTransicionPermitida(estadoActual, destino.nombre)) {
          throw new ConflictException(
            `Transicion no permitida de "${estadoActual ?? 'desconocido'}" a "${destino.nombre}"`,
          );
        }

        const incidenteActualizado = await tx.incidente.update({
          where: { id },
          data: { estadoId: destino.id },
          include,
        });

        await tx.historialEstado.create({
          data: {
            incidenteId: id,
            estadoId: destino.id,
            usuarioId,
            observacion,
          },
        });

        const propietarioReportaId =
          incidente.usuarioReportaId ??
          (incidenteActualizado as { usuarioReportaId?: string }).usuarioReportaId ??
          '';
        const tituloIncidente =
          incidente.titulo ??
          (incidenteActualizado as { titulo?: string }).titulo;

        return {
          actualizado: incidenteActualizado,
          estadoAnterior: estadoActual,
          usuarioReportaId: propietarioReportaId,
          titulo: tituloIncidente,
        };
      });

    // La notificacion se emite una unica vez tras confirmar la transaccion de BD.
    // Cualquier error de comunicacion o persistencia en el microservicio de
    // notificaciones es gestionado de forma controlada por el cliente sin
    // revertir el cambio de estado ya aplicado (#113).
    if (this.notificacionesClient && usuarioReportaId) {
      await this.notificacionesClient.notificarCambioEstado({
        incidenteId: id,
        titulo,
        usuarioReportaId,
        estadoAnterior,
        estadoNuevo: dto.estado,
        observacion,
      });
    }

    return toIncidenteResponse(actualizado);
  }
}
