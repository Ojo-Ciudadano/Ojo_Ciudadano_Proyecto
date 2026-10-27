import { NotFoundException } from '@nestjs/common';

jest.mock('./prisma/prisma.service', () => ({ PrismaService: class {} }));

import { IncidentesService } from './incidentes.service';

const evidencia = {
  id: 'evi-1',
  incidenteId: 'inc-1',
  tipoMime: 'image/png',
  tamanoArchivo: 100,
  fechaCreacion: new Date('2026-10-22T12:00:00.000Z'),
};

describe('IncidentesService - evidencias en respuestas', () => {
  let service: IncidentesService;
  const prisma: any = {
    incidente: { findMany: jest.fn(), findUnique: jest.fn() },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    service = new IncidentesService(prisma);
  });

  it('findAll incluye evidencias sin claveArchivo en la consulta', async () => {
    prisma.incidente.findMany.mockResolvedValue([{ id: 'inc-1', evidencias: [evidencia] }]);

    const res = await service.findAll('user-1');

    const args = prisma.incidente.findMany.mock.calls[0][0];
    expect(args.include.evidencias.select).not.toHaveProperty('claveArchivo');
    expect(args.include.evidencias.select).toMatchObject({ id: true, tipoMime: true });
    expect(res[0].evidencias).toEqual([{ ...evidencia, url: '/evidencias/evi-1/archivo' }]);
  });

  it('findAll responde evidencias vacio para incidentes sin evidencias', async () => {
    prisma.incidente.findMany.mockResolvedValue([{ id: 'inc-2', evidencias: [] }]);
    const res = await service.findAll();
    expect(res[0].evidencias).toEqual([]);
  });

  it('findOne incluye evidencias asociadas', async () => {
    prisma.incidente.findUnique.mockResolvedValue({ id: 'inc-1', evidencias: [evidencia] });
    const res = await service.findOne('inc-1');
    expect(res.evidencias).toHaveLength(1);
    expect(res.evidencias[0].incidenteId).toBe('inc-1');
  });

  it('findOne mantiene el 404 cuando no existe', async () => {
    prisma.incidente.findUnique.mockResolvedValue(null);
    await expect(service.findOne('x')).rejects.toThrow(NotFoundException);
  });
});