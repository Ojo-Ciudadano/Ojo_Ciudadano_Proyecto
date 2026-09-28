import { Test, TestingModule } from '@nestjs/testing';
import { ProxyService } from './proxy.service';
import type { Request, Response } from 'express';

describe('ProxyService', () => {
  let service: ProxyService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ProxyService],
    }).compile();

    service = module.get<ProxyService>(ProxyService);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('debe estar definido', () => {
    expect(service).toBeDefined();
  });

  describe('getServiceUrl', () => {
    it('debe resolver URL para auth, usuarios y roles', () => {
      expect(service.getServiceUrl('auth')).toContain('3001');
      expect(service.getServiceUrl('usuarios')).toContain('3001');
      expect(service.getServiceUrl('roles')).toContain('3001');
    });

    it('debe resolver URL para incidentes, categorias y evidencias', () => {
      expect(service.getServiceUrl('incidentes')).toContain('3002');
      expect(service.getServiceUrl('categorias')).toContain('3002');
      expect(service.getServiceUrl('evidencias')).toContain('3002');
    });

    it('debe resolver URL para notificaciones', () => {
      expect(service.getServiceUrl('notificaciones')).toContain('3003');
    });

    it('debe retornar null para prefijos no reconocidos', () => {
      expect(service.getServiceUrl('desconocido')).toBeNull();
    });
  });

  describe('forward', () => {
    it('debe responder 404 si la ruta no tiene microservicio configurado', async () => {
      const mockReq = {
        path: '/api/v1/inexistente',
        query: {},
        headers: {},
        method: 'GET',
      } as unknown as Request;

      const mockJson = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
      const mockRes = {
        status: mockStatus,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(mockStatus).toHaveBeenCalledWith(404);
      expect(mockJson).toHaveBeenCalledWith(
        expect.objectContaining({
          statusCode: 404,
          error: 'Not Found',
        }),
      );
    });

    it('debe reenviar una peticion GET exitosa y retornar JSON', async () => {
      const mockData = [{ id: '1', titulo: 'Bache' }];
      global.fetch = jest.fn().mockResolvedValue({
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: jest.fn().mockResolvedValue(mockData),
      });

      const mockReq = {
        path: '/api/v1/incidentes',
        query: {},
        headers: { authorization: 'Bearer token-test' },
        method: 'GET',
      } as unknown as Request;

      const mockJson = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
      const mockRes = {
        status: mockStatus,
        json: mockJson,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:3002/incidentes',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            authorization: 'Bearer token-test',
          }),
        }),
      );
      expect(mockStatus).toHaveBeenCalledWith(200);
      expect(mockJson).toHaveBeenCalledWith(mockData);
    });

    it('debe reenviar una peticion POST con body serializado', async () => {
      const mockCreated = { id: 'uuid-123', titulo: 'Bache reparado' };
      global.fetch = jest.fn().mockResolvedValue({
        status: 201,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: jest.fn().mockResolvedValue(mockCreated),
      });

      const mockReq = {
        path: '/api/v1/incidentes',
        query: {},
        headers: {},
        method: 'POST',
        body: { titulo: 'Bache reparado' },
      } as unknown as Request;

      const mockJson = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
      const mockRes = {
        status: mockStatus,
        json: mockJson,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:3002/incidentes',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ titulo: 'Bache reparado' }),
        }),
      );
      expect(mockStatus).toHaveBeenCalledWith(201);
      expect(mockJson).toHaveBeenCalledWith(mockCreated);
    });

    it('debe responder 503 si el microservicio destino esta caido o falla la conexion', async () => {
      global.fetch = jest.fn().mockRejectedValue(new Error('Connection refused'));

      const mockReq = {
        path: '/api/v1/incidentes',
        query: {},
        headers: {},
        method: 'GET',
      } as unknown as Request;

      const mockJson = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
      const mockRes = {
        status: mockStatus,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(mockStatus).toHaveBeenCalledWith(503);
      expect(mockJson).toHaveBeenCalledWith(
        expect.objectContaining({
          statusCode: 503,
          error: 'Service Unavailable',
        }),
      );
    });
  });
});
