import {
  BadRequestException,
  ConflictException,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';

import { UpdateEstadoDto } from './dto/update-estado.dto';
import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';

const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const USUARIO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';

const ESTADO_IDS: Record<string, string> = {
  Nuevo: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
  'En revisión': 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15',
  'En atención': 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16',
  Resuelto: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17',
  Cerrado: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18',
};

const NOMBRES_POR_ID = Object.fromEntries(
  Object.entries(ESTADO_IDS).map(([nombre, id]) => [id, nombre]),
);

const dto = (
  estado: string,
  observacion = 'Observacion del cambio',
): UpdateEstadoDto => ({
  estado: estado as UpdateEstadoDto['estado'],
  observacion,
});

describe('IncidentesService - cambio de estado (#117)', () => {
  let estadoPersistido: string;
  let historial: Array<Record<string, unknown>>;
  let estadoActual: string;
  let tx: {
    $queryRaw: jest.Mock;
    incidente: { findUnique: jest.Mock; update: jest.Mock };
    estado: { findUnique: jest.Mock };
    historialEstado: { create: jest.Mock };
  };
  let prisma: { $transaction: jest.Mock };
  let service: IncidentesService;

  beforeEach(() => {
    estadoPersistido = 'Nuevo';
    estadoActual = 'Nuevo';
    historial = [];

    tx = {
      $queryRaw: jest.fn().mockResolvedValue([{ id: INCIDENTE_ID }]),
      incidente: {
        findUnique: jest.fn(async () => ({
          id: INCIDENTE_ID,
          estado: { nombre: estadoActual },
        })),
        update: jest.fn(async ({ data }: { data: { estadoId: string } }) => {
          estadoPersistido = NOMBRES_POR_ID[data.estadoId];
          return {
            id: INCIDENTE_ID,
            estadoId: data.estadoId,
            estado: {
              id: data.estadoId,
              nombre: estadoPersistido,
            },
            evidencias: [],
          };
        }),
      },
      estado: {
        findUnique: jest.fn(
          async ({ where }: { where: { nombre: string } }) => {
            const id = ESTADO_IDS[where.nombre];
            return id ? { id, nombre: where.nombre } : null;
          },
        ),
      },
      historialEstado: {
        create: jest.fn(async ({ data }: { data: Record<string, unknown> }) => {
          historial.push(data);
          return { id: 'historial-1', ...data };
        }),
      },
    };

    // Simula la atomicidad de la transaccion: si el callback falla, se
    // restauran el estado y el historial previos.
    prisma = {
      $transaction: jest.fn(async (callback: (tx: unknown) => unknown) => {
        const estadoPrevio = estadoPersistido;
        const historialPrevio = [...historial];
        try {
          return await callback(tx);
        } catch (error) {
          estadoPersistido = estadoPrevio;
          historial = historialPrevio;
          throw error;
        }
      }),
    };

    service = new IncidentesService(prisma as unknown as PrismaService);
  });

  it('aplica una transicion permitida y registra un unico historial con el actor autenticado', async () => {
    const resultado = await service.updateEstado(
      INCIDENTE_ID,
      dto('En revisión', '  Se inicia la evaluacion  '),
      USUARIO_ID,
    );

    expect(resultado.estado.nombre).toBe('En revisión');
    expect(estadoPersistido).toBe('En revisión');
    expect(historial).toHaveLength(1);
    expect(historial[0]).toMatchObject({
      incidenteId: INCIDENTE_ID,
      estadoId: ESTADO_IDS['En revisión'],
      usuarioId: USUARIO_ID,
      observacion: 'Se inicia la evaluacion',
    });
    expect(tx.historialEstado.create).toHaveBeenCalledTimes(1);
    expect(prisma.$transaction).toHaveBeenCalledTimes(1);
  });

  it.each([
    ['Nuevo', 'En revisión'],
    ['En revisión', 'En atención'],
    ['En atención', 'Resuelto'],
    ['Resuelto', 'En atención'],
    ['Resuelto', 'Cerrado'],
  ])('aplica la transicion permitida %s -> %s', async (origen, destino) => {
    estadoActual = origen;
    estadoPersistido = origen;

    await service.updateEstado(INCIDENTE_ID, dto(destino), USUARIO_ID);

    expect(estadoPersistido).toBe(destino);
    expect(historial).toHaveLength(1);
    expect(historial[0].estadoId).toBe(ESTADO_IDS[destino]);
  });

  it('responde 409 al solicitar el mismo estado y no escribe estado ni historial', async () => {
    estadoActual = 'En revisión';
    estadoPersistido = 'En revisión';

    await expect(
      service.updateEstado(INCIDENTE_ID, dto('En revisión'), USUARIO_ID),
    ).rejects.toBeInstanceOf(ConflictException);

    expect(estadoPersistido).toBe('En revisión');
    expect(historial).toHaveLength(0);
    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });

  it.each([
    ['Nuevo', 'En atención'],
    ['Nuevo', 'Resuelto'],
    ['Nuevo', 'Cerrado'],
    ['En revisión', 'Nuevo'],
    ['En revisión', 'Resuelto'],
    ['En atención', 'Cerrado'],
    ['Cerrado', 'En atención'],
  ])(
    'responde 409 y conserva el estado y el historial en la transicion prohibida %s -> %s',
    async (origen, destino) => {
      estadoActual = origen;
      estadoPersistido = origen;

      await expect(
        service.updateEstado(INCIDENTE_ID, dto(destino), USUARIO_ID),
      ).rejects.toBeInstanceOf(ConflictException);

      expect(estadoPersistido).toBe(origen);
      expect(historial).toHaveLength(0);
      expect(tx.incidente.update).not.toHaveBeenCalled();
      expect(tx.historialEstado.create).not.toHaveBeenCalled();
    },
  );

  it('responde 404 si el incidente no existe sin escribir estado ni historial', async () => {
    tx.$queryRaw.mockResolvedValue([]);

    await expect(
      service.updateEstado(INCIDENTE_ID, dto('En revisión'), USUARIO_ID),
    ).rejects.toBeInstanceOf(NotFoundException);

    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });

  it('responde 404 si el estado destino no existe sin escribir estado ni historial', async () => {
    tx.estado.findUnique.mockResolvedValue(null);

    await expect(
      service.updateEstado(INCIDENTE_ID, dto('En revisión'), USUARIO_ID),
    ).rejects.toBeInstanceOf(NotFoundException);

    expect(tx.incidente.update).not.toHaveBeenCalled();
    expect(tx.historialEstado.create).not.toHaveBeenCalled();
  });

  it('responde 400 si el id del incidente no es un UUID valido', async () => {
    await expect(
      service.updateEstado('no-es-un-uuid', dto('En revisión'), USUARIO_ID),
    ).rejects.toBeInstanceOf(BadRequestException);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it.each(['Pendiente', 'En revision', ''])(
    'responde 400 si el estado solicitado no es canonico (%s)',
    async (estado) => {
      await expect(
        service.updateEstado(INCIDENTE_ID, dto(estado), USUARIO_ID),
      ).rejects.toBeInstanceOf(BadRequestException);

      expect(prisma.$transaction).not.toHaveBeenCalled();
    },
  );

  it.each(['', '   '])(
    'responde 400 si la observacion esta vacia (%p)',
    async (observacion) => {
      await expect(
        service.updateEstado(
          INCIDENTE_ID,
          dto('En revisión', observacion),
          USUARIO_ID,
        ),
      ).rejects.toBeInstanceOf(BadRequestException);

      expect(prisma.$transaction).not.toHaveBeenCalled();
    },
  );

  it('responde 401 si la identidad autenticada no esta disponible', async () => {
    await expect(
      service.updateEstado(INCIDENTE_ID, dto('En revisión'), ''),
    ).rejects.toBeInstanceOf(UnauthorizedException);

    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('revierte el estado si falla la persistencia del historial', async () => {
    tx.historialEstado.create.mockRejectedValue(
      new Error('fallo de historial'),
    );

    await expect(
      service.updateEstado(INCIDENTE_ID, dto('En revisión'), USUARIO_ID),
    ).rejects.toThrow('fallo de historial');

    expect(tx.incidente.update).toHaveBeenCalledTimes(1);
    expect(estadoPersistido).toBe('Nuevo');
    expect(historial).toHaveLength(0);
  });
});
