import { Test, TestingModule } from '@nestjs/testing';
import { MAX_PROXY_PAYLOAD_BYTES, ProxyService } from './proxy.service';
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

    it('debe reenviar una subida multipart/form-data preservando el stream binario (#86)', async () => {
      const mockEvidencia = {
        id: 'ev-1',
        incidenteId: 'inc-1',
        tipoMime: 'image/jpeg',
        tamanoArchivo: 12,
        url: '/evidencias/ev-1/archivo',
      };
      global.fetch = jest.fn().mockResolvedValue({
        status: 201,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: jest.fn().mockResolvedValue(mockEvidencia),
      });

      const rawMultipart = Buffer.from('--boundary\r\ncontent\r\n--boundary--');
      const mockReq = {
        path: '/api/v1/incidentes/inc-1/evidencias',
        query: {},
        headers: {
          'content-type': 'multipart/form-data; boundary=boundary',
          authorization: 'Bearer token-test',
        },
        method: 'POST',
        body: {},
        async *[Symbol.asyncIterator]() {
          yield rawMultipart;
        },
      } as unknown as Request;

      const mockJson = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
      const mockRes = {
        status: mockStatus,
        json: mockJson,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:3002/incidentes/inc-1/evidencias',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'content-type': 'multipart/form-data; boundary=boundary',
          }),
          body: new Uint8Array(rawMultipart),
        }),
      );
      expect(mockStatus).toHaveBeenCalledWith(201);
      expect(mockJson).toHaveBeenCalledWith(mockEvidencia);
    });

    it('debe reenviar una respuesta binaria de imagen preservando Content-Type y Buffer (#86)', async () => {
      const imageBytes = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]);
      global.fetch = jest.fn().mockResolvedValue({
        status: 200,
        headers: new Headers({
          'content-type': 'image/jpeg',
          'content-length': imageBytes.length.toString(),
          'cache-control': 'private, max-age=3600',
        }),
        arrayBuffer: jest.fn().mockResolvedValue(
          imageBytes.buffer.slice(
            imageBytes.byteOffset,
            imageBytes.byteOffset + imageBytes.byteLength,
          ),
        ),
      });

      const mockReq = {
        path: '/api/v1/evidencias/ev-1/archivo',
        query: {},
        headers: { authorization: 'Bearer token-test' },
        method: 'GET',
      } as unknown as Request;

      const mockSend = jest.fn();
      const mockSetHeader = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ send: mockSend });
      const mockRes = {
        status: mockStatus,
        send: mockSend,
        setHeader: mockSetHeader,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(mockStatus).toHaveBeenCalledWith(200);
      expect(mockSetHeader).toHaveBeenCalledWith('Content-Type', 'image/jpeg');
      expect(mockSetHeader).toHaveBeenCalledWith(
        'Content-Length',
        imageBytes.length.toString(),
      );
      expect(mockSend).toHaveBeenCalledWith(imageBytes);
    });

    it('debe responder 413 si el payload binario excede 12 MB (#86)', async () => {
      const oversized = Buffer.alloc(MAX_PROXY_PAYLOAD_BYTES + 1);
      const mockReq = {
        path: '/api/v1/incidentes/inc-1/evidencias',
        query: {},
        headers: { 'content-type': 'multipart/form-data; boundary=x' },
        method: 'POST',
        body: oversized,
      } as unknown as Request;

      const mockJson = jest.fn();
      const mockStatus = jest.fn().mockReturnValue({ json: mockJson });
      const mockRes = {
        status: mockStatus,
        json: mockJson,
      } as unknown as Response;

      await service.forward(mockReq, mockRes);

      expect(mockStatus).toHaveBeenCalledWith(413);
      expect(mockJson).toHaveBeenCalledWith(
        expect.objectContaining({
          statusCode: 413,
          error: 'Payload Too Large',
        }),
      );
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
