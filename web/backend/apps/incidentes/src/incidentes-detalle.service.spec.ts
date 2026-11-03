import {
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';

import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';

const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const OTRO_INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23';
const USUARIO_REPORTA_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const USUARIO_RESPONSABLE_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a31';
const CATEGORIA_ID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';
const UBICACION_ID = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a41';
const ASIGNACION_ID = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51';
const OTRA_ASIGNACION_ID = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52';

const FECHA_CREACION = new Date('2026-10-30T10:00:00.000Z');
const FECHA_ACTUALIZACION = new Date('2026-10-30T11:00:00.000Z');
const FECHA_HISTORIAL = new Date('2026-10-30T10:30:00.000Z');

const categoria = {
  id: CATEGORIA_ID,
  nombre: 'Agua y alcantarillado',
  descripcion: 'Fugas y problemas de agua',
  prioridadBase: 3,
  esEmergenciaCritica: false,
  canalOficialContacto: null,
};

const estado = {
  id: ESTADO_ID,
  nombre: 'Nuevo',
  orden: 1,
};

const ubicacion = {
  id: UBICACION_ID,
  latitud: -38.7359,
  longitud: -72.5904,
  direccionReferencia: 'Av. Alemania 0211',
  comuna: 'Temuco',
  region: 'Araucania',
  sector: 'Centro',
};

const evidencia = {
  id: 'evi-1',
  incidenteId: INCIDENTE_ID,
  tipoMime: 'image/png',
  tamanoArchivo: 100,
  fechaCreacion: FECHA_CREACION,
};

const incidente = {
  id: INCIDENTE_ID,
  titulo: 'Fuga de agua',
  descripcion: 'Fuga en la via publica',
  categoriaId: CATEGORIA_ID,
  estadoId: ESTADO_ID,
  ubicacionId: UBICACION_ID,
  usuarioReportaId: USUARIO_REPORTA_ID,
  nivelPrioridad: 3,
  fechaCreacion: FECHA_CREACION,
  fechaActualizacion: FECHA_ACTUALIZACION,
  categoria,
  estado,
  ubicacion,
  evidencias: [evidencia],
};

const historial = [
  {
    id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    incidenteId: INCIDENTE_ID,
    estadoId: ESTADO_ID,
    usuarioId: USUARIO_REPORTA_ID,
    observacion: 'Reporte creado',
    fecha: FECHA_HISTORIAL,
    estado,
  },
];

const asignacion = {
  id: ASIGNACION_ID,
  incidenteId: INCIDENTE_ID,
  usuarioAsignadoId: USUARIO_RESPONSABLE_ID,
  asignadoPorId: USUARIO_REPORTA_ID,
  fechaAsignacion: FECHA_ACTUALIZACION,
  activa: true,
};

describe('IncidentesService - detalle con historial y asignacion activa (#125)', () => {
  const prisma = {
    incidente: {
      findUnique: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
    },
    historialEstado: {
      findMany: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
    },
    asignacion: {
      findFirst: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
    },
  };

  let service: IncidentesService;

  beforeEach(() => {
    jest.resetAllMocks();

    prisma.incidente.findUnique.mockResolvedValue(incidente);
    prisma.historialEstado.findMany.mockResolvedValue(historial);
    prisma.asignacion.findFirst.mockResolvedValue(asignacion);

    service = new IncidentesService(prisma as unknown as PrismaService);
  });

  it('devuelve el detalle ampliado conservando los campos existentes y las evidencias', async () => {
    const resultado = await service.findOne(INCIDENTE_ID);

    expect(resultado).toMatchObject({
      id: INCIDENTE_ID,
      titulo: 'Fuga de agua',
      descripcion: 'Fuga en la via publica',
      categoriaId: CATEGORIA_ID,
      estadoId: ESTADO_ID,
      ubicacionId: UBICACION_ID,
      usuarioReportaId: USUARIO_REPORTA_ID,
      nivelPrioridad: 3,
      fechaCreacion: FECHA_CREACION,
      fechaActualizacion: FECHA_ACTUALIZACION,
      categoria,
      estado,
      ubicacion,
    });

    expect(resultado.evidencias).toEqual([
      { ...evidencia, url: '/evidencias/evi-1/archivo' },
    ]);
  });

  it('incluye el historial con el mismo contrato y orden de #120', async () => {
    const resultado = await service.findOne(INCIDENTE_ID);

    expect(resultado.historial).toEqual([
      {
        id: historial[0].id,
        incidenteId: INCIDENTE_ID,
        estado,
        usuarioId: USUARIO_REPORTA_ID,
        observacion: 'Reporte creado',
        fecha: FECHA_HISTORIAL,
      },
    ]);

    expect(prisma.historialEstado.findMany).toHaveBeenCalledWith({
      where: { incidenteId: INCIDENTE_ID },
      include: { estado: true },
      orderBy: [{ fecha: 'asc' }, { id: 'asc' }],
    });
  });

  it('incluye solo la asignacion activa del incidente solicitado', async () => {
    const resultado = await service.findOne(INCIDENTE_ID);

    expect(resultado.asignacionActiva).toEqual(asignacion);
    expect(prisma.asignacion.findFirst).toHaveBeenCalledWith({
      where: { incidenteId: INCIDENTE_ID, activa: true },
      orderBy: { fechaAsignacion: 'desc' },
    });
  });

  it('representa un historial vacio y la ausencia de responsable activo', async () => {
    prisma.historialEstado.findMany.mockResolvedValue([]);
    prisma.asignacion.findFirst.mockResolvedValue(null);

    const resultado = await service.findOne(INCIDENTE_ID);

    expect(resultado.historial).toEqual([]);
    expect(resultado.asignacionActiva).toBeNull();
  });

  it('no mezcla historial ni asignaciones de otros incidentes', async () => {
    prisma.historialEstado.findMany.mockImplementation(
      async ({ where }: { where: { incidenteId: string } }) =>
        [
          ...historial,
          {
            ...historial[0],
            id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
            incidenteId: OTRO_INCIDENTE_ID,
          },
        ].filter((registro) => registro.incidenteId === where.incidenteId),
    );

    prisma.asignacion.findFirst.mockImplementation(
      async ({
        where,
      }: {
        where: { incidenteId: string; activa: boolean };
      }) =>
        [
          asignacion,
          {
            ...asignacion,
            id: OTRA_ASIGNACION_ID,
            incidenteId: OTRO_INCIDENTE_ID,
          },
        ].find(
          (fila) =>
            fila.incidenteId === where.incidenteId &&
            fila.activa === where.activa,
        ) ?? null,
    );

    const resultado = await service.findOne(INCIDENTE_ID);

    expect(resultado.historial).toHaveLength(1);
    expect(resultado.historial[0].incidenteId).toBe(INCIDENTE_ID);
    expect(resultado.asignacionActiva?.incidenteId).toBe(INCIDENTE_ID);
  });

  it('responde 400 si el id no es un UUID valido y no consulta la base', async () => {
    await expect(service.findOne('no-es-un-uuid')).rejects.toBeInstanceOf(
      BadRequestException,
    );

    expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
    expect(prisma.asignacion.findFirst).not.toHaveBeenCalled();
  });

  it('responde 404 si el incidente no existe y no consulta historial ni asignaciones', async () => {
    prisma.incidente.findUnique.mockResolvedValue(null);

    await expect(service.findOne(INCIDENTE_ID)).rejects.toBeInstanceOf(
      NotFoundException,
    );

    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
    expect(prisma.asignacion.findFirst).not.toHaveBeenCalled();
  });

  it('es una consulta de solo lectura', async () => {
    await service.findOne(INCIDENTE_ID);

    expect(prisma.incidente.update).not.toHaveBeenCalled();
    expect(prisma.incidente.delete).not.toHaveBeenCalled();
    expect(prisma.historialEstado.create).not.toHaveBeenCalled();
    expect(prisma.historialEstado.update).not.toHaveBeenCalled();
    expect(prisma.historialEstado.delete).not.toHaveBeenCalled();
    expect(prisma.asignacion.create).not.toHaveBeenCalled();
    expect(prisma.asignacion.update).not.toHaveBeenCalled();
    expect(prisma.asignacion.delete).not.toHaveBeenCalled();
  });
});
