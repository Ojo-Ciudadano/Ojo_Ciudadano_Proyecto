import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';

import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { RolesGuard } from './auth/roles.guard';
import { HttpExceptionFilter } from './filters/http-exception.filter';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';
import { PrismaService } from './prisma/prisma.service';

const JWT_SECRET = 'test-jwt-secret';
const INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13';
const OTRO_INCIDENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a23';
const INCIDENTE_SIN_HISTORIAL_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33';
const INCIDENTE_INEXISTENTE_ID = 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a43';
const USUARIO_SOLICITANTE_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12';
const USUARIO_ACTOR_ID = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a99';
const ESTADO_NUEVO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14';
const ESTADO_REVISION_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15';
const ESTADO_ATENCION_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16';
const ESTADO_RESUELTO_ID = 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17';

const ESTADOS = {
  [ESTADO_NUEVO_ID]: { id: ESTADO_NUEVO_ID, nombre: 'Nuevo', orden: 1 },
  [ESTADO_REVISION_ID]: {
    id: ESTADO_REVISION_ID,
    nombre: 'En revisión',
    orden: 2,
  },
  [ESTADO_ATENCION_ID]: {
    id: ESTADO_ATENCION_ID,
    nombre: 'En atención',
    orden: 3,
  },
  [ESTADO_RESUELTO_ID]: {
    id: ESTADO_RESUELTO_ID,
    nombre: 'Resuelto',
    orden: 4,
  },
};

type HistorialFila = {
  id: string;
  incidenteId: string;
  estadoId: string;
  usuarioId: string;
  observacion: string | null;
  fecha: Date;
};

type OrdenHistorial = Array<{ fecha: 'asc' | 'desc'; id?: 'asc' | 'desc' }>;

describe('IncidentesController - historial (integracion)', () => {
  let app: INestApplication<App>;
  let tokenAdmin: string;
  let tokenCiudadano: string;
  let historial: HistorialFila[];
  let incidentes: Set<string>;
  let estadoActual: string;

  const prisma = {
    incidente: {
      findUnique: jest.fn(),
      update: jest.fn(),
    },
    historialEstado: {
      findMany: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
    },
    $transaction: jest.fn(),
  };

  const tx = {
    $queryRaw: jest.fn(),
    incidente: { findUnique: jest.fn(), update: jest.fn() },
    estado: { findUnique: jest.fn() },
    historialEstado: { create: jest.fn() },
  };

  const getHistorial = (id: string, token?: string) => {
    const peticion = request(app.getHttpServer()).get(
      `/incidentes/${id}/historial`,
    );
    return token ? peticion.set('Authorization', `Bearer ${token}`) : peticion;
  };

  beforeEach(async () => {
    jest.resetAllMocks();

    incidentes = new Set([
      INCIDENTE_ID,
      OTRO_INCIDENTE_ID,
      INCIDENTE_SIN_HISTORIAL_ID,
    ]);
    estadoActual = 'Nuevo';
    historial = [
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
        incidenteId: INCIDENTE_ID,
        estadoId: ESTADO_NUEVO_ID,
        usuarioId: USUARIO_SOLICITANTE_ID,
        observacion: 'Reporte creado',
        fecha: new Date('2026-10-29T10:00:00.000Z'),
      },
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
        incidenteId: INCIDENTE_ID,
        estadoId: ESTADO_ATENCION_ID,
        usuarioId: USUARIO_ACTOR_ID,
        observacion: 'Paso a atencion',
        fecha: new Date('2026-10-30T10:00:00.000Z'),
      },
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
        incidenteId: INCIDENTE_ID,
        estadoId: ESTADO_RESUELTO_ID,
        usuarioId: USUARIO_ACTOR_ID,
        observacion: null,
        fecha: new Date('2026-10-30T10:00:00.000Z'),
      },
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        incidenteId: OTRO_INCIDENTE_ID,
        estadoId: ESTADO_NUEVO_ID,
        usuarioId: USUARIO_ACTOR_ID,
        observacion: 'Registro de otro incidente',
        fecha: new Date('2026-10-28T10:00:00.000Z'),
      },
    ];

    const jwt = new JwtService({ secret: JWT_SECRET });
    tokenAdmin = jwt.sign({ sub: USUARIO_ACTOR_ID, roles: ['ADMIN'] });
    tokenCiudadano = jwt.sign({
      sub: USUARIO_SOLICITANTE_ID,
      roles: ['CIUDADANO'],
    });

    prisma.incidente.findUnique.mockImplementation(
      async ({ where }: { where: { id: string } }) =>
        incidentes.has(where.id) ? { id: where.id } : null,
    );

    prisma.historialEstado.findMany.mockImplementation(
      async ({
        where,
        orderBy,
      }: {
        where: { incidenteId: string };
        orderBy: OrdenHistorial;
      }) => {
        const filas = historial
          .filter((fila) => fila.incidenteId === where.incidenteId)
          .map((fila) => ({
            ...fila,
            estado: ESTADOS[fila.estadoId],
          }));

        const [primerOrden] = orderBy;
        const segundoOrden = orderBy[1];
        filas.sort((a, b) => {
          const comparacionFecha = a.fecha.getTime() - b.fecha.getTime();
          if (comparacionFecha !== 0) {
            return primerOrden.fecha === 'asc'
              ? comparacionFecha
              : -comparacionFecha;
          }
          const comparacionId = a.id.localeCompare(b.id);
          return segundoOrden?.id === 'desc' ? -comparacionId : comparacionId;
        });
        return filas;
      },
    );

    tx.$queryRaw.mockResolvedValue([{ id: INCIDENTE_ID }]);
    tx.incidente.findUnique.mockImplementation(async () => ({
      id: INCIDENTE_ID,
      estado: { nombre: estadoActual },
    }));
    tx.estado.findUnique.mockImplementation(
      async ({ where }: { where: { nombre: string } }) => {
        const encontrado = Object.values(ESTADOS).find(
          (estado) => estado.nombre === where.nombre,
        );
        return encontrado
          ? { id: encontrado.id, nombre: encontrado.nombre }
          : null;
      },
    );
    tx.incidente.update.mockImplementation(
      async ({ data }: { data: { estadoId: string } }) => {
        const destino = Object.values(ESTADOS).find(
          (estado) => estado.id === data.estadoId,
        )!;
        estadoActual = destino.nombre;
        return {
          id: INCIDENTE_ID,
          estadoId: data.estadoId,
          estado: destino,
          evidencias: [],
        };
      },
    );
    tx.historialEstado.create.mockImplementation(
      async ({
        data,
      }: {
        data: {
          incidenteId: string;
          estadoId: string;
          usuarioId: string;
          observacion: string;
        };
      }) => {
        const nuevo: HistorialFila = {
          id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a99',
          ...data,
          fecha: new Date('2026-10-31T10:00:00.000Z'),
        };
        historial.push(nuevo);
        return nuevo;
      },
    );

    prisma.$transaction.mockImplementation(
      async (callback: (cliente: unknown) => unknown) => callback(tx),
    );

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [IncidentesController],
      providers: [
        IncidentesService,
        { provide: PrismaService, useValue: prisma },
        JwtAuthGuard,
        RolesGuard,
        { provide: JwtService, useValue: jwt },
        { provide: Reflector, useValue: new Reflector() },
      ],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalFilters(new HttpExceptionFilter());
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
      }),
    );
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  it('responde 401 sin autenticacion y no consulta el historial', async () => {
    await getHistorial(INCIDENTE_ID).expect(401);

    expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
  });

  it('responde 403 si el token no tiene un rol permitido', async () => {
    const tokenSinRol = new JwtService({ secret: JWT_SECRET }).sign({
      sub: USUARIO_SOLICITANTE_ID,
      roles: ['OTRO'],
    });

    await getHistorial(INCIDENTE_ID, tokenSinRol).expect(403);

    expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
  });

  it('permite consultar a un Ciudadano autenticado', async () => {
    await getHistorial(INCIDENTE_ID, tokenCiudadano).expect(200);

    expect(prisma.historialEstado.findMany).toHaveBeenCalled();
  });

  it('devuelve el contrato y el orden cronologico con desempate por id', async () => {
    const respuesta = await getHistorial(
      INCIDENTE_ID,
      tokenCiudadano,
    ).expect(200);

    expect(respuesta.body).toEqual([
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
        incidenteId: INCIDENTE_ID,
        estado: ESTADOS[ESTADO_NUEVO_ID],
        usuarioId: USUARIO_SOLICITANTE_ID,
        observacion: 'Reporte creado',
        fecha: '2026-10-29T10:00:00.000Z',
      },
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
        incidenteId: INCIDENTE_ID,
        estado: ESTADOS[ESTADO_ATENCION_ID],
        usuarioId: USUARIO_ACTOR_ID,
        observacion: 'Paso a atencion',
        fecha: '2026-10-30T10:00:00.000Z',
      },
      {
        id: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03',
        incidenteId: INCIDENTE_ID,
        estado: ESTADOS[ESTADO_RESUELTO_ID],
        usuarioId: USUARIO_ACTOR_ID,
        observacion: null,
        fecha: '2026-10-30T10:00:00.000Z',
      },
    ]);

    expect(prisma.historialEstado.findMany).toHaveBeenCalledWith({
      where: { incidenteId: INCIDENTE_ID },
      include: { estado: true },
      orderBy: [{ fecha: 'asc' }, { id: 'asc' }],
    });
  });

  it('usa el actor persistido y no al usuario que consulta', async () => {
    const respuesta = await getHistorial(
      INCIDENTE_ID,
      tokenCiudadano,
    ).expect(200);

    expect(respuesta.body[1].usuarioId).toBe(USUARIO_ACTOR_ID);
    expect(respuesta.body[1].usuarioId).not.toBe(USUARIO_SOLICITANTE_ID);
  });

  it('no mezcla registros de otros incidentes', async () => {
    const respuesta = await getHistorial(
      INCIDENTE_ID,
      tokenCiudadano,
    ).expect(200);

    expect(respuesta.body).toHaveLength(3);
    expect(
      respuesta.body.every(
        (fila: { incidenteId: string }) => fila.incidenteId === INCIDENTE_ID,
      ),
    ).toBe(true);
    expect(respuesta.body).not.toContainEqual(
      expect.objectContaining({ incidenteId: OTRO_INCIDENTE_ID }),
    );
  });

  it('devuelve una coleccion vacia si el incidente existe sin historial', async () => {
    const respuesta = await getHistorial(
      INCIDENTE_SIN_HISTORIAL_ID,
      tokenCiudadano,
    ).expect(200);

    expect(respuesta.body).toEqual([]);
  });

  it('responde 404 si el incidente no existe y no consulta historial', async () => {
    await getHistorial(INCIDENTE_INEXISTENTE_ID, tokenCiudadano).expect(404);

    expect(prisma.incidente.findUnique).toHaveBeenCalledWith({
      where: { id: INCIDENTE_INEXISTENTE_ID },
      select: { id: true },
    });
    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
  });

  it('responde 400 si el id no es un UUID valido', async () => {
    const respuesta = await getHistorial(
      'no-es-un-uuid',
      tokenCiudadano,
    ).expect(400);

    expect(JSON.stringify(respuesta.body.message)).toContain('UUID');
    expect(prisma.incidente.findUnique).not.toHaveBeenCalled();
    expect(prisma.historialEstado.findMany).not.toHaveBeenCalled();
  });

  it('no modifica el incidente ni sus registros al consultar', async () => {
    await getHistorial(INCIDENTE_ID, tokenCiudadano).expect(200);

    expect(prisma.incidente.update).not.toHaveBeenCalled();
    expect(prisma.historialEstado.create).not.toHaveBeenCalled();
    expect(prisma.historialEstado.update).not.toHaveBeenCalled();
    expect(prisma.historialEstado.delete).not.toHaveBeenCalled();
  });

  it('muestra el cambio persistido por #117 con el actor que lo ejecuto', async () => {
    await request(app.getHttpServer())
      .patch(`/incidentes/${INCIDENTE_ID}/estado`)
      .set('Authorization', `Bearer ${tokenAdmin}`)
      .send({
        estado: 'En revisión',
        observacion: '  Se inicia la evaluacion  ',
      })
      .expect(200);

    const respuesta = await getHistorial(
      INCIDENTE_ID,
      tokenCiudadano,
    ).expect(200);

    const registro = respuesta.body.find(
      (fila: { estado: { nombre: string } }) =>
        fila.estado.nombre === 'En revisión',
    );

    expect(registro).toMatchObject({
      incidenteId: INCIDENTE_ID,
      estado: { id: ESTADO_REVISION_ID, nombre: 'En revisión', orden: 2 },
      usuarioId: USUARIO_ACTOR_ID,
      observacion: 'Se inicia la evaluacion',
      fecha: '2026-10-31T10:00:00.000Z',
    });
  });
});