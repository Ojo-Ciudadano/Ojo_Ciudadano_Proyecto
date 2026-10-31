/// <reference types="jest" />
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';

import { FindIncidentesQueryDto } from './find-incidentes-query.dto';
import { Test, TestingModule } from '@nestjs/testing';
import type { Request, Response } from 'express';
import { ProxyService } from './proxy.service';

describe('ProxyService - filtros de incidentes (integracion)', () => {
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

  it('reenvia los filtros de consulta al microservicio de incidentes', async () => {
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
        comuna: 'Temuco',
      },
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
      'http://localhost:3002/incidentes?usuarioReportaId=b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12&estadoId=d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14&categoriaId=a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11&comuna=Temuco',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(mockStatus).toHaveBeenCalledWith(200);
    expect(mockJson).toHaveBeenCalledWith(datos);
  });
});