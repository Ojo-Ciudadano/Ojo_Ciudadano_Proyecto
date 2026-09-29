import { Test, TestingModule } from '@nestjs/testing';
import { JwtService } from '@nestjs/jwt';
import { ProxyController } from './proxy.controller';
import { ProxyService } from './proxy.service';
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
            verifyAsync: jest.fn().mockResolvedValue({ sub: '123' }),
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

  it('handleNotificacionesProxy debe delegar en proxyService.forward', async () => {
    const req = {} as Request;
    const res = {} as Response;
    await controller.handleNotificacionesProxy(req, res);
    expect(service.forward).toHaveBeenCalledWith(req, res);
  });
});
