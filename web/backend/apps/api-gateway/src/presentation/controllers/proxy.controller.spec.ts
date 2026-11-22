import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { ProxyController } from './proxy.controller';
import { ProxyService } from '../../application/services/proxy.service';
import type { Request, Response } from 'express';

describe('ProxyController', () => {
  let controller: ProxyController;
  let service: ProxyService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [ProxyController],
      providers: [
        {
          provide: ProxyService,
          useValue: {
            forward: jest.fn().mockResolvedValue(undefined),
          },
        },
        {
          provide: JwtService,
          useValue: {
            verifyAsync: jest.fn().mockResolvedValue({
              sub: '123',
            }),
          },
        },
      ],
    }).compile();

    controller = module.get<ProxyController>(ProxyController);
    service = module.get<ProxyService>(ProxyService);
  });

  it('debe estar definido', () => {
    expect(controller).toBeDefined();
  });

  it('handleAuthProxy debe delegar en proxyService.forward', async () => {
    const req = {} as Request;
    const res = {} as Response;

    await controller.handleAuthProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
  });

  it('handleIncidentesProxy debe delegar en proxyService.forward', async () => {
    const req = {} as Request;
    const res = {} as Response;

    await controller.handleIncidentesProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
  });

  it('handleIncidentesEstadoProxy debe delegar en proxyService.forward', async () => {
    const req = {} as Request;
    const res = {} as Response;

    await controller.handleIncidentesEstadoProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
  });

  it('handleIncidentesPrioridadProxy debe delegar en proxyService.forward', async () => {
    const req = {} as Request;
    const res = {} as Response;

    await controller.handleIncidentesPrioridadProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
  });

  // Issue #114: consultar notificaciones del usuario autenticado
  it('handleMisNotificacionesProxy debe delegar en proxyService.forward', async () => {
    const req = {
      method: 'GET',
      path: '/api/v1/notificaciones',
      headers: {
        authorization: 'Bearer token-de-prueba',
      },
    } as Request;

    const res = {} as Response;

    await controller.handleMisNotificacionesProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
    expect(service.forward).toHaveBeenCalledTimes(1);
  });

  it('handleNotificacionesProxy debe delegar en proxyService.forward', async () => {
    const req = {} as Request;
    const res = {} as Response;

    await controller.handleNotificacionesProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
  });

  // Issue #327: marcar notificaciones como leídas
  it('handleMarcarTodasLeidasProxy debe delegar en proxyService.forward', async () => {
    const req = {
      method: 'PATCH',
      path: '/api/v1/notificaciones/leidas',
      headers: { authorization: 'Bearer token-de-prueba' },
    } as Request;
    const res = {} as Response;

    await controller.handleMarcarTodasLeidasProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
    expect(service.forward).toHaveBeenCalledTimes(1);
  });

  it('handleMarcarLeidaProxy debe delegar en proxyService.forward', async () => {
    const req = {
      method: 'PATCH',
      path: '/api/v1/notificaciones/30000000-0000-4000-8000-000000000001/leida',
      headers: { authorization: 'Bearer token-de-prueba' },
    } as Request;
    const res = {} as Response;

    await controller.handleMarcarLeidaProxy(req, res);

    expect(service.forward).toHaveBeenCalledWith(req, res);
    expect(service.forward).toHaveBeenCalledTimes(1);
  });
});
