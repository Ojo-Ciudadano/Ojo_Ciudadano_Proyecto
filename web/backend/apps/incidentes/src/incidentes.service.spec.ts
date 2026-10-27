import { BadRequestException, NotFoundException } from '@nestjs/common';

import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';
import { CreateIncidenteDto } from './dto/create-incidente.dto';

const CATEGORIA_ID = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
const USUARIO_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const ESTADO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';

const ubicacionValida = {
  latitud: -38.7359,
  longitud: -72.5904,
  direccionReferencia: 'Av. Alemania 0211',
  comuna: 'Temuco',
  region: 'Araucania',
  sector: 'Centro',
};

const crearDto = (
  extra: Partial<CreateIncidenteDto> = {},
): CreateIncidenteDto => ({
  titulo: 'Fuga de agua',
  descripcion: 'Fuga en la via publica',
  categoriaId: CATEGORIA_ID,
  usuarioReportaId: USUARIO_ID,
  ubicacion: ubicacionValida,
  ...extra,
});

describe('IncidentesService', () => {
  const tx = {
    ubicacion: { create: jest.fn() },
    incidente: { create: jest.fn() },
    historialEstado: { create: jest.fn() },
  };

  const prisma = {
    categoria: { findUnique: jest.fn() },
    estado: { findFirst: jest.fn() },
    incidente: { findUnique: jest.fn(), update: jest.fn() },
    $transaction: jest.fn(),
  };

  let service: IncidentesService;

  const categoriaCon = (prioridadBase: unknown) => ({
    id: CATEGORIA_ID,
    nombre: 'Agua y alcantarillado',
    prioridadBase,
  });

  beforeEach(() => {
    jest.resetAllMocks();

    prisma.categoria.findUnique.mockResolvedValue(categoriaCon(3));
    prisma.estado.findFirst.mockResolvedValue({
      id: ESTADO_ID,
      nombre: 'Nuevo',
    });
    prisma.$transaction.mockImplementation(async (callback: (tx: unknown) => unknown) =>
      callback(tx),
    );
    tx.ubicacion.create.mockResolvedValue({ id: 'ubicacion-1' });
    tx.incidente.create.mockImplementation(async ({ data }: { data: object }) => ({
      id: INCIDENTE_ID,
      ...data,
    }));
    tx.historialEstado.create.mockResolvedValue({ id: 'historial-1' });

    service = new IncidentesService(prisma as unknown as PrismaService);
  });

  describe('create', () => {
    it('asigna como nivelPrioridad la prioridadBase de la categoria', async () => {
      await service.create(crearDto());

      expect(tx.incidente.create).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({
            categoriaId: CATEGORIA_ID,
            nivelPrioridad: 3,
          }),
        }),
      );
    });

    it('ignora cualquier prioridad enviada por el cliente', async () => {
      const dto = crearDto() as CreateIncidenteDto & {
        nivelPrioridad: number;
        prioridad: number;
      };
      dto.nivelPrioridad = 5;
      dto.prioridad = 5;

      await service.create(dto);

      const data = tx.incidente.create.mock.calls[0][0].data;

      expect(data.nivelPrioridad).toBe(3);
      expect(Object.keys(data)).not.toContain('prioridad');
    });

    it('responde 400 si la categoria no existe y no crea el incidente', async () => {
      prisma.categoria.findUnique.mockResolvedValue(null);

      await expect(service.create(crearDto())).rejects.toBeInstanceOf(
        BadRequestException,
      );

      expect(prisma.$transaction).not.toHaveBeenCalled();
      expect(tx.incidente.create).not.toHaveBeenCalled();
    });

    it.each([
      ['cero', 0],
      ['negativa', -2],
      ['sobre la escala', 6],
      ['decimal', 2.5],
      ['texto', '3'],
      ['nula', null],
    ])(
      'responde 400 si la prioridad base es %s y no crea el incidente',
      async (_caso, prioridadBase) => {
        prisma.categoria.findUnique.mockResolvedValue(
          categoriaCon(prioridadBase),
        );

        await expect(service.create(crearDto())).rejects.toBeInstanceOf(
          BadRequestException,
        );

        expect(prisma.$transaction).not.toHaveBeenCalled();
        expect(tx.incidente.create).not.toHaveBeenCalled();
      },
    );
  });

  describe('updatePrioridad', () => {
    beforeEach(() => {
      prisma.incidente.findUnique.mockResolvedValue({ id: INCIDENTE_ID });
      prisma.incidente.update.mockImplementation(
        async ({ data }: { data: { nivelPrioridad: number } }) => ({
          id: INCIDENTE_ID,
          nivelPrioridad: data.nivelPrioridad,
        }),
      );
    });

    it('actualiza solo el nivel de prioridad y devuelve la prioridad confirmada', async () => {
      const resultado = await service.updatePrioridad(INCIDENTE_ID, {
        nivelPrioridad: 4,
      });

      expect(prisma.incidente.update).toHaveBeenCalledWith({
        where: { id: INCIDENTE_ID },
        data: { nivelPrioridad: 4 },
        include: { categoria: true, estado: true, ubicacion: true },
      });
      expect(Object.keys(prisma.incidente.update.mock.calls[0][0].data)).toEqual([
        'nivelPrioridad',
      ]);
      expect(resultado.nivelPrioridad).toBe(4);
    });

    it('responde 404 si el incidente no existe', async () => {
      prisma.incidente.findUnique.mockResolvedValue(null);

      await expect(
        service.updatePrioridad(INCIDENTE_ID, { nivelPrioridad: 4 }),
      ).rejects.toBeInstanceOf(NotFoundException);

      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it('responde 400 si el id no es un UUID valido', async () => {
      await expect(
        service.updatePrioridad('no-es-un-uuid', { nivelPrioridad: 4 }),
      ).rejects.toBeInstanceOf(BadRequestException);

      expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
      expect(prisma.incidente.update).not.toHaveBeenCalled();
    });

    it.each([
      ['cero', 0],
      ['sobre la escala', 6],
      ['decimal', 2.5],
      ['texto', '4'],
      ['nula', null],
      ['ausente', undefined],
    ])(
      'responde 400 si la prioridad solicitada es %s',
      async (_caso, nivelPrioridad) => {
        await expect(
          service.updatePrioridad(INCIDENTE_ID, {
            nivelPrioridad: nivelPrioridad as number,
          }),
        ).rejects.toBeInstanceOf(BadRequestException);

        expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
        expect(prisma.incidente.update).not.toHaveBeenCalled();
      },
    );
  });
});
