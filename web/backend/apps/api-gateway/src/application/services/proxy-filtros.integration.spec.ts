/// <reference types="jest" />
import { INestApplication } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { JwtService } from '@nestjs/jwt';
import { Test, TestingModule } from '@nestjs/testing';
import type { Request, Response } from 'express';
import request from 'supertest';
import { App } from 'supertest/types';
import { JwtAuthGuard } from '../../infrastructure/security/jwt-auth.guard';
import { RolesGuard } from '../../infrastructure/security/roles.guard';
import { ProxyController } from '../../presentation/controllers/proxy.controller';
import { ProxyService } from './proxy.service';

describe('ProxyService - filtros de incidentes (integracion)', () => {
  const originalFetch = global.fetch;
  let service: ProxyService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ProxyService],
    }).compile();

    service = module.get<ProxyService>(ProxyService);
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  function crearMockResponse() {
    const mockJson = jest.fn();
    const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
    const mockRes = {
      status: mockStatus,
      json: mockJson,
    } as unknown as Response;

    return { mockJson, mockStatus, mockRes };
  }

  it('reenvia los filtros de consulta incluyendo ciudad al microservicio de incidentes (#119, #315)', async () => {
    const datos = [{ id: 'inc-1' }];

    global.fetch = jest.fn().mockResolvedValue({
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: jest.fn().mockResolvedValue(datos),
    });

    const mockReq = {
      path: '/api/v1/incidentes',
      query: {
        usuarioReportaId: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        estadoId: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        categoriaId: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        ciudad: 'Temuco',
        comuna: 'Padre Las Casas',
      },
      headers: { authorization: 'Bearer token-test' },
      method: 'GET',
    } as unknown as Request;

    const { mockJson, mockStatus, mockRes } = crearMockResponse();

    await service.forward(mockReq, mockRes);

    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:3002/incidentes?usuarioReportaId=b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12&estadoId=d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14&categoriaId=a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11&ciudad=Temuco&comuna=Padre+Las+Casas',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(mockStatus).toHaveBeenCalledWith(200);
    expect(mockJson).toHaveBeenCalledWith(datos);
  });

  it('reenvia ciudad y comuna combinados y conserva el resultado filtrado del microservicio (#315)', async () => {
    const datos = [{ id: 'inc-temuco-plc', ubicacion: { comuna: 'Padre Las Casas' } }];

    global.fetch = jest.fn().mockResolvedValue({
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: jest.fn().mockResolvedValue(datos),
    });

    const mockReq = {
      path: '/api/v1/incidentes',
      query: {
        ciudad: 'Temuco',
        comuna: 'Padre Las Casas',
      },
      headers: { authorization: 'Bearer token-test' },
      method: 'GET',
    } as unknown as Request;

    const { mockJson, mockStatus, mockRes } = crearMockResponse();

    await service.forward(mockReq, mockRes);

    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:3002/incidentes?ciudad=Temuco&comuna=Padre+Las+Casas',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(mockStatus).toHaveBeenCalledWith(200);
    expect(mockJson).toHaveBeenCalledWith(datos);
  });

  it('conserva el comportamiento vigente cuando se omite el filtro ciudad (#315)', async () => {
    const datos = [{ id: 'inc-1' }, { id: 'inc-2' }];

    global.fetch = jest.fn().mockResolvedValue({
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: jest.fn().mockResolvedValue(datos),
    });

    const mockReq = {
      path: '/api/v1/incidentes',
      query: {
        estadoId: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        comuna: 'Temuco',
      },
      headers: { authorization: 'Bearer token-test' },
      method: 'GET',
    } as unknown as Request;

    const { mockJson, mockStatus, mockRes } = crearMockResponse();

    await service.forward(mockReq, mockRes);

    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:3002/incidentes?estadoId=d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14&comuna=Temuco',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(mockStatus).toHaveBeenCalledWith(200);
    expect(mockJson).toHaveBeenCalledWith(datos);
  });

  it('reenvia parametros repetidos sin concatenarlos con comas y omite cabeceras hop-by-hop', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: jest.fn().mockResolvedValue([]),
    });

    const mockReq = {
      path: '/api/v1/incidentes',
      query: {
        ciudad: ['Temuco', 'Villarrica'],
      },
      headers: {
        authorization: 'Bearer token-test',
        host: 'localhost:3000',
        'transfer-encoding': 'chunked',
        expect: '100-continue',
      },
      method: 'GET',
    } as unknown as Request;

    const { mockStatus, mockRes } = crearMockResponse();

    await service.forward(mockReq, mockRes);

    const [url, options] = (global.fetch as jest.Mock).mock.calls[0];
    expect(url).toBe('http://localhost:3002/incidentes?ciudad=Temuco&ciudad=Villarrica');
    expect(options.headers).toEqual({ authorization: 'Bearer token-test' });
    expect(mockStatus).toHaveBeenCalledWith(200);
  });
});

describe('API Gateway HTTP - filtro ciudad y comuna en GET /api/v1/incidentes (#315)', () => {
  const JWT_SECRET = 'test-jwt-secret-315';
  const originalFetch = global.fetch;
  let app: INestApplication<App>;
  let token: string;

  const catalogoCiudades: Record<string, string[]> = {
    temuco: ['temuco', 'padre las casas'],
    villarrica: ['villarrica', 'pucón'],
  };

  const incidentesDemo = [
    { id: 'inc-temuco', ubicacion: { comuna: 'Temuco' } },
    { id: 'inc-plc', ubicacion: { comuna: 'Padre Las Casas' } },
    { id: 'inc-villarrica', ubicacion: { comuna: 'Villarrica' } },
  ];

  beforeEach(async () => {
    const jwt = new JwtService({ secret: JWT_SECRET });
    token = jwt.sign({ sub: 'usuario-1', roles: ['CIUDADANO'] });

    global.fetch = jest.fn(async (input) => {
      const targetUrl = new URL(String(input));

      if (targetUrl.pathname === '/usuarios/me') {
        return {
          ok: true,
          status: 200,
          headers: new Headers({ 'content-type': 'application/json' }),
          json: async () => ({ id: 'usuario-1', roles: ['CIUDADANO'] }),
        } as unknown as globalThis.Response;
      }

      const ciudad = targetUrl.searchParams.get('ciudad')?.trim().toLowerCase();
      const comuna = targetUrl.searchParams.get('comuna')?.trim().toLowerCase();

      const filtrados = incidentesDemo.filter((inc) => {
        const comunaInc = inc.ubicacion.comuna.toLowerCase();
        if (ciudad) {
          const comunasValidas = catalogoCiudades[ciudad] ?? [];
          if (!comunasValidas.includes(comunaInc)) return false;
        }
        if (comuna && comunaInc !== comuna) return false;
        return true;
      });

      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => filtrados,
      } as unknown as globalThis.Response;
    }) as typeof fetch;

    const moduleFixture: TestingModule = await Test.createTestingModule({
      controllers: [ProxyController],
      providers: [
        ProxyService,
        JwtAuthGuard,
        RolesGuard,
        Reflector,
        { provide: JwtService, useValue: jwt },
      ],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('api/v1');
    await app.init();
  });

  afterEach(async () => {
    global.fetch = originalFetch;
    await app?.close();
  });

  it('devuelve unicamente incidentes que cumplen ciudad y comuna cuando ambos se envian', async () => {
    const res = await request(app.getHttpServer())
      .get('/api/v1/incidentes')
      .query({ ciudad: 'Temuco', comuna: 'Padre Las Casas' })
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(res.body).toEqual([
      { id: 'inc-plc', ubicacion: { comuna: 'Padre Las Casas' } },
    ]);

    const vacio = await request(app.getHttpServer())
      .get('/api/v1/incidentes')
      .query({ ciudad: 'Villarrica', comuna: 'Temuco' })
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(vacio.body).toEqual([]);
  });

  it('conserva todos los incidentes cuando se omite ciudad y comuna', async () => {
    const res = await request(app.getHttpServer())
      .get('/api/v1/incidentes')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(res.body).toHaveLength(3);
  });
});