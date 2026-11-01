import { BadRequestException, NotFoundException } from '@nestjs/common';
import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';

const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const OTRO_INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23';
const USUARIO_ACTOR_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';
const FECHA = new Date('2026-10-30T10:00:00.000Z');

const estado = { id: ESTADO_ID, nombre: 'En revisión', orden: 2 };

const registro = {
  id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
  incidenteId: INCIDENTE_ID,
  estadoId: ESTADO_ID,
  usuarioId: USUARIO_ACTOR_ID,
  observacion: 'Se inicia la evaluacion',
  fecha: FECHA,
  estado,
};

describe('IncidentesService - historial (#120)', () => {
  const prisma = {
    incidente: { findUnique: jest.fn() },
    historialEstado: {
      findMany: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
    },
  };

  let service: IncidentesService;

  beforeEach(() => {
    jest.resetAllMocks();
    prisma.incidente.findUnique.mockResolvedValue({ id: INCIDENTE_ID });
    prisma.historialEstado.findMany.mockResolvedValue([]);
    service = new IncidentesService(prisma as unknown as PrismaService);
  });

  it('consulta solo el incidente solicitado y ordena por fecha y id ascendente', async () => {
    await service.findHistorial(INCIDENTE_ID);

    expect(prisma.incidente.findUnique).toHaveBeenCalledWith({
      where: { id: INCIDENTE_ID },
      select: { id: true },
    });
    expect(prisma.historialEstado.findMany).toHaveBeenCalledWith({
      where: { incidenteId: INCIDENTE_ID },
      include: { estado: true },
      orderBy: [{ fecha: 'asc' }, { id: 'asc' }],
    });
  });

  it('mapea estado, actor persistido, observacion y fecha', async () => {
    prisma.historialEstado.findMany.mockResolvedValue([registro]);

    const resultado = await service.findHistorial(INCIDENTE_ID);

    expect(resultado).toEqual([
      {
        id: registro.id,
        incidenteId: INCIDENTE_ID,
        estado,
        usuarioId: USUARIO_ACTOR_ID,
        observacion: 'Se inicia la evaluacion',
        fecha: FECHA,
      },
    ]);
    expect(resultado[0].usuarioId).toBe(USUARIO_ACTOR_ID);
  });

  it('representa una observacion ausente como null', async () => {
    prisma.historialEstado.findMany.mockResolvedValue([
      { ...registro, observacion: null },
    ]);

    const [resultado] = await service.findHistorial(INCIDENTE_ID);

    expect(resultado).toHaveProperty('observacion', null);
  });

  it('no mezcla registros de otros incidentes', async () => {
    prisma.historialEstado.findMany.mockImplementation(
      async ({ where }: { where: { incidenteId: string } }) =>
        [
          registro,
          {
            ...registro,
            id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
            incidenteId: OTRO_INCIDENTE_ID,
          },
        ].filter((fila) => fila.incidenteId === where.incidenteId),
    );

    const resultado = await service.findHistorial(INCIDENTE_ID);

    expect(resultado).toHaveLength(1);
    expect(resultado[0].incidenteId).toBe(INCIDENTE_ID);
  });

  it('devuelve una coleccion vacia si el incidente existe sin historial', async () => {
    await expect(service.findHistorial(INCIDENTE_ID)).resolves.toEqual([]);
  });

  it('responde 404 si el incidente no existe y no consulta historial', async () => {
    prisma.incidente.findUnique.mockResolvedValue(null);

    await expect(
      service.findHistorial(OTRO_INCIDENTE_ID),
    ).rejects.toBeInstanceOf(NotFoundException);

    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
  });

  it('responde 400 si el id no es un UUID valido y no consulta la base', async () => {
    await expect(service.findHistorial('no-es-un-uuid')).rejects.toBeInstanceOf(
      BadRequestException,
    );

    expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
  });

  it('no escribe ni modifica registros al consultar', async () => {
    await service.findHistorial(INCIDENTE_ID);
    expect(prisma.historialEstado.create).not.toHaveBeenCalled();
    expect(prisma.historialEstado.update).not.toHaveBeenCalled();
    expect(prisma.historialEstado.delete).not.toHaveBeenCalled();
  });
});