import { BadRequestException, NotFoundException } from '@nestjs/common';

import { IncidentesService } from './incidentes.service';
import { PrismaService } from '../../infrastructure/persistence/prisma/prisma.service';
import { CreateIncidenteDto } from '../dto/create-incidente.dto';

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

const crearDtoConUbicacion = (
  cambios: Record<string, unknown>,
): CreateIncidenteDto => {
  const dto = crearDto();
  dto.ubicacion = {
    ...ubicacionValida,
    ...cambios,
  } as unknown as CreateIncidenteDto['ubicacion'];
  return dto;
};

const evidencia = {
  id: 'evi-1',
  incidenteId: INCIDENTE_ID,
  tipoMime: 'image/png',
  tamanoArchivo: 100,
  fechaCreacion: new Date('2026-10-22T12:00:00.000Z'),
};

describe('IncidentesService', () => {
  const tx = {
    ubicacion: { create: jest.fn() },
    incidente: { create: jest.fn() },
    historialEstado: { create: jest.fn() },
  };

  const prisma = {
    categoria: { findUnique: jest.fn() },
    estado: { findFirst: jest.fn() },
    incidente: {
      findUnique: jest.fn(),
      findMany: jest.fn(),
      update: jest.fn(),
    },
    historialEstado: {
      findMany: jest.fn(),
    },
    asignacion: {
      findFirst: jest.fn(),
    },
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
    prisma.historialEstado.findMany.mockResolvedValue([]);
    prisma.asignacion.findFirst.mockResolvedValue(null);
    prisma.$transaction.mockImplementation(
      async (callback: (tx: unknown) => unknown) => callback(tx),
    );
    tx.ubicacion.create.mockResolvedValue({ id: 'ubicacion-1' });
    tx.incidente.create.mockImplementation(
      async ({ data }: { data: object }) => ({
        id: INCIDENTE_ID,
        ...data,
      }),
    );
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

    it('guarda las coordenadas y los datos de una ubicacion manual valida', async () => {
      const dto = crearDto();

      await service.create(dto);

      expect(tx.ubicacion.create).toHaveBeenCalledWith({ data: dto.ubicacion });
      expect(prisma.$transaction).toHaveBeenCalledTimes(1);
    });

    it('conserva los reportes validos por GPS con el contrato actual', async () => {
      const dto = crearDtoConUbicacion({
        latitud: -38.7368,
        longitud: -72.6065,
      });

      await expect(service.create(dto)).resolves.toMatchObject({
        id: INCIDENTE_ID,
      });
      expect(tx.ubicacion.create).toHaveBeenCalledWith({ data: dto.ubicacion });
    });

    it.each([
      ['latitud no numerica', { latitud: 'abc' }, 'latitud'],
      ['latitud no finita', { latitud: Number.NaN }, 'latitud'],
      ['longitud no finita', { longitud: Number.POSITIVE_INFINITY }, 'longitud'],
      ['latitud fuera de La Araucania', { latitud: -40 }, 'limit'],
      ['longitud fuera de La Araucania', { longitud: -74 }, 'limit'],
    ])(
      'rechaza coordenadas invalidas: %s',
      async (_caso, cambios, mensajeEsperado) => {
        await expect(
          service.create(crearDtoConUbicacion(cambios)),
        ).rejects.toThrow(mensajeEsperado);

        expect(prisma.categoria.findUnique).not.toHaveBeenCalled();
        expect(prisma.$transaction).not.toHaveBeenCalled();
        expect(tx.ubicacion.create).not.toHaveBeenCalled();
      },
    );

    it.each([
      ['region distinta', { region: 'Biobio' }, 'region'],
      ['comuna fuera del catalogo', { comuna: 'Concepcion' }, 'comuna'],
      ['direccion vacia', { direccionReferencia: '   ' }, 'direccion'],
      ['sector vacio', { sector: '' }, 'sector'],
    ])(
      'rechaza una ubicacion incoherente: %s',
      async (_caso, cambios, mensajeEsperado) => {
        await expect(
          service.create(crearDtoConUbicacion(cambios)),
        ).rejects.toThrow(mensajeEsperado);

        expect(prisma.categoria.findUnique).not.toHaveBeenCalled();
        expect(prisma.$transaction).not.toHaveBeenCalled();
        expect(tx.ubicacion.create).not.toHaveBeenCalled();
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
        include: expect.objectContaining({
          categoria: true,
          estado: true,
          ubicacion: true,
        }),
      });
      expect(
        Object.keys(prisma.incidente.update.mock.calls[0][0].data),
      ).toEqual(['nivelPrioridad']);
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

  describe('evidencias en respuestas', () => {
    it('findAll incluye evidencias sin claveArchivo en la consulta', async () => {
      prisma.incidente.findMany.mockResolvedValue([
        { id: 'inc-1', evidencias: [evidencia] },
      ]);
      const res = await service.findAll({ usuarioReportaId: USUARIO_ID });

      const args = prisma.incidente.findMany.mock.calls[0][0];
      expect(args.include.evidencias.select).not.toHaveProperty(
        'claveArchivo',
      );
      expect(args.include.evidencias.select).toMatchObject({
        id: true,
        tipoMime: true,
      });
      expect(res[0].evidencias).toEqual([
        { ...evidencia, url: '/evidencias/evi-1/archivo' },
      ]);
    });

    it('findAll responde evidencias vacio para incidentes sin evidencias', async () => {
      prisma.incidente.findMany.mockResolvedValue([
        { id: 'inc-2', evidencias: [] },
      ]);
      const res = await service.findAll();
      expect(res[0].evidencias).toEqual([]);
    });

    it('findOne incluye evidencias asociadas', async () => {
      prisma.incidente.findUnique.mockResolvedValue({
        id: INCIDENTE_ID,
        evidencias: [evidencia],
      });
      const res = await service.findOne(INCIDENTE_ID);
      expect(res.evidencias).toHaveLength(1);
      expect(res.evidencias[0].incidenteId).toBe(INCIDENTE_ID);
    });

    it('findOne mantiene el 404 cuando no existe', async () => {
      prisma.incidente.findUnique.mockResolvedValue(null);
      await expect(service.findOne(INCIDENTE_ID)).rejects.toThrow(
        NotFoundException,
      );
    });
  });
    describe('findAll con filtros', () => {
    beforeEach(() => {
      prisma.incidente.findMany.mockResolvedValue([]);
    });

    it('sin filtros conserva where undefined y el orden original', async () => {
      await service.findAll();

      const args = prisma.incidente.findMany.mock.calls[0][0];
      expect(args.where).toBeUndefined();
      expect(args.orderBy).toEqual({ fechaCreacion: 'desc' });
      expect(args.include).toEqual(
        expect.objectContaining({
          categoria: true,
          estado: true,
          ubicacion: true,
        }),
      );
    });

    it('conserva el filtro usuarioReportaId', async () => {
      await service.findAll({ usuarioReportaId: USUARIO_ID });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { usuarioReportaId: USUARIO_ID },
        }),
      );
    });

    it('filtra por estado actual', async () => {
      await service.findAll({ estadoId: ESTADO_ID });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { estadoId: ESTADO_ID },
        }),
      );
    });

    it('filtra por categoria', async () => {
      await service.findAll({ categoriaId: CATEGORIA_ID });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { categoriaId: CATEGORIA_ID },
        }),
      );
    });

    it('filtra por comuna con coincidencia exacta sin distinguir mayusculas', async () => {
      await service.findAll({ comuna: '  Temuco  ' });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            ubicacion: {
              is: {
                comuna: { equals: 'Temuco', mode: 'insensitive' },
              },
            },
          },
        }),
      );
    });

    it('filtra por ciudad usando las comunas de su agrupación', async () => {
      await service.findAll({ ciudad: '  temuco  ' });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            ubicacion: {
              is: {
                comuna: {
                  in: ['Temuco', 'Padre Las Casas'],
                  mode: 'insensitive',
                },
              },
            },
          },
        }),
      );
    });

    it('filtra por Villarrica incluyendo Pucón', async () => {
      await service.findAll({ ciudad: 'villarrica' });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            ubicacion: {
              is: {
                comuna: {
                  in: ['Villarrica', 'Pucón'],
                  mode: 'insensitive',
                },
              },
            },
          },
        }),
      );
    });

    it('combina ciudad y comuna valida en la misma ubicacion', async () => {
      await service.findAll({ ciudad: 'Temuco', comuna: ' Padre Las Casas ' });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            ubicacion: {
              is: {
                comuna: { equals: 'Padre Las Casas', mode: 'insensitive' },
              },
            },
          },
        }),
      );
    });

    it('devuelve una coleccion vacia si la comuna no pertenece a la ciudad', async () => {
      await expect(
        service.findAll({ ciudad: 'Villarrica', comuna: 'Temuco' }),
      ).resolves.toEqual([]);
      expect(prisma.incidente.findMany).not.toHaveBeenCalled();
    });

    it('devuelve una coleccion vacia si la ciudad no esta en el catalogo', async () => {
      await expect(
        service.findAll({ ciudad: 'Ciudad Inexistente' }),
      ).resolves.toEqual([]);
      expect(prisma.incidente.findMany).not.toHaveBeenCalled();
    });

    it('combina todos los filtros y usuarioReportaId mediante AND', async () => {
      await service.findAll({
        usuarioReportaId: USUARIO_ID,
        estadoId: ESTADO_ID,
        categoriaId: CATEGORIA_ID,
        ciudad: 'Temuco',
        comuna: 'Temuco',
      });

      expect(prisma.incidente.findMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: {
            usuarioReportaId: USUARIO_ID,
            estadoId: ESTADO_ID,
            categoriaId: CATEGORIA_ID,
            ubicacion: {
              is: {
                comuna: { equals: 'Temuco', mode: 'insensitive' },
              },
            },
          },
        }),
      );
    });

    it('devuelve unicamente los incidentes de la ciudad solicitada', async () => {
      type FiltroFindMany = {
        where?: {
          usuarioReportaId?: string;
          estadoId?: string;
          categoriaId?: string;
          ubicacion?: {
            is?: {
              comuna?: { equals?: string; in?: string[] };
            };
          };
        };
      };
      const incidentes = [
        {
          id: 'inc-temuco',
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: { comuna: 'Temuco' },
          evidencias: [],
        },
        {
          id: 'inc-padre-las-casas',
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: { comuna: 'Padre Las Casas' },
          evidencias: [],
        },
        {
          id: 'inc-villarrica',
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: { comuna: 'Villarrica' },
          evidencias: [],
        },
        {
          id: 'inc-pucon',
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: { comuna: 'Pucón' },
          evidencias: [],
        },
        {
          id: 'inc-angol',
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: { comuna: 'Angol' },
          evidencias: [],
        },
        {
          id: 'inc-biobio',
          usuarioReportaId: USUARIO_ID,
          estadoId: ESTADO_ID,
          categoriaId: CATEGORIA_ID,
          ubicacion: { comuna: 'Concepción' },
          evidencias: [],
        },
      ];

      // Simula el filtro de Prisma sobre datos en memoria para comprobar el
      // resultado real, ademas del `where` construido en las pruebas anteriores.
      prisma.incidente.findMany.mockImplementation(
        async ({ where }: FiltroFindMany) =>
          incidentes.filter((incidente) => {
            const comunaFiltro = where?.ubicacion?.is?.comuna;
            const coincideComuna = !comunaFiltro
              ? true
              : comunaFiltro.in
                ? comunaFiltro.in.some(
                    (comuna) =>
                      comuna.toLowerCase() ===
                      incidente.ubicacion.comuna.toLowerCase(),
                  )
                : (comunaFiltro.equals ?? '').toLowerCase() ===
                  incidente.ubicacion.comuna.toLowerCase();
            return (
              (!where?.usuarioReportaId ||
                incidente.usuarioReportaId === where.usuarioReportaId) &&
              (!where?.estadoId || incidente.estadoId === where.estadoId) &&
              (!where?.categoriaId ||
                incidente.categoriaId === where.categoriaId) &&
              coincideComuna
            );
          }),
      );

      const resultado = await service.findAll({ ciudad: 'temuco' });
      expect(resultado.map((incidente) => incidente.id)).toEqual([
        'inc-temuco',
        'inc-padre-las-casas',
      ]);
    });

    it('devuelve una coleccion vacia cuando no hay coincidencias', async () => {
      prisma.incidente.findMany.mockResolvedValue([]);

      await expect(
        service.findAll({
          estadoId: ESTADO_ID,
          comuna: 'Sin resultados',
        }),
      ).resolves.toEqual([]);
    });

  });
});
