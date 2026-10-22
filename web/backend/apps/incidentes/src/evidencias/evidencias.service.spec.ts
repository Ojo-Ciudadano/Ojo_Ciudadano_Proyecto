import { BadGatewayException, BadRequestException, NotFoundException, PayloadTooLargeException } from '@nestjs/common';
import { EvidenciasService, MAX_EVIDENCIA_BYTES } from './evidencias.service';
import { StorageObjectNotFoundError, StorageOperationError } from '../storage/storage.service';

describe('EvidenciasService', () => {
  const prisma = {
    incidente: { findUnique: jest.fn() },
    evidencia: { create: jest.fn(), findUnique: jest.fn() },
  };
  const storage = { upload: jest.fn(), download: jest.fn(), remove: jest.fn() };
  let service: EvidenciasService;

  const entrada = {
    incidenteId: 'inc-1',
    archivo: Buffer.from('foto'),
    tipoMime: 'image/jpeg',
  };

  beforeEach(() => {
    jest.resetAllMocks();
    prisma.incidente.findUnique.mockResolvedValue({ id: 'inc-1' });
    service = new EvidenciasService(prisma as any, storage as any);
  });

  it('sube el objeto y luego persiste la metadata asociada al incidente', async () => {
    prisma.evidencia.create.mockImplementation(async ({ data }: { data: unknown }) => data);

    const result = await service.guardar(entrada);

    expect(storage.upload).toHaveBeenCalledWith(result.claveArchivo, entrada.archivo, 'image/jpeg');
    expect(result.claveArchivo).toMatch(/^incidentes\/inc-1\/[0-9a-f-]{36}\.jpg$/);
    expect(result).toMatchObject({ incidenteId: 'inc-1', tipoMime: 'image/jpeg', tamanoArchivo: 4 });
    expect(storage.upload.mock.invocationCallOrder[0]).toBeLessThan(
      prisma.evidencia.create.mock.invocationCallOrder[0],
    );
  });

  it('no crea metadata si falla el almacenamiento', async () => {
    storage.upload.mockRejectedValue(new StorageOperationError('upload', 'caído'));

    await expect(service.guardar(entrada)).rejects.toBeInstanceOf(BadGatewayException);
    expect(prisma.evidencia.create).not.toHaveBeenCalled();
  });

  it('borra el objeto subido si falla el insert en Prisma', async () => {
    prisma.evidencia.create.mockRejectedValue(new Error('db caída'));
    storage.remove.mockResolvedValue(undefined);

    await expect(service.guardar(entrada)).rejects.toThrow('No fue posible registrar la evidencia');
    const clave = storage.upload.mock.calls[0][0];
    expect(storage.remove).toHaveBeenCalledWith(clave);
  });

  it('valida tipo, tamaño e incidente antes de tocar el storage', async () => {
    await expect(service.guardar({ ...entrada, tipoMime: 'application/pdf' })).rejects.toBeInstanceOf(BadRequestException);
    await expect(
      service.guardar({ ...entrada, archivo: Buffer.alloc(MAX_EVIDENCIA_BYTES + 1) }),
    ).rejects.toBeInstanceOf(PayloadTooLargeException);
    prisma.incidente.findUnique.mockResolvedValue(null);
    await expect(service.guardar(entrada)).rejects.toBeInstanceOf(NotFoundException);
    expect(storage.upload).not.toHaveBeenCalled();
  });

  it('recupera el archivo con la clave persistida', async () => {
    prisma.evidencia.findUnique.mockResolvedValue({ id: 'e1', claveArchivo: 'k', tipoMime: 'image/png' });
    storage.download.mockResolvedValue({ buffer: Buffer.from('x') });

    const result = await service.obtenerArchivo('e1');

    expect(storage.download).toHaveBeenCalledWith('k');
    expect(result.tipoMime).toBe('image/png');
  });

  it('responde 404 si la metadata existe pero el objeto no', async () => {
    prisma.evidencia.findUnique.mockResolvedValue({ id: 'e1', claveArchivo: 'k', tipoMime: 'image/png' });
    storage.download.mockRejectedValue(new StorageObjectNotFoundError('k'));

    await expect(service.obtenerArchivo('e1')).rejects.toBeInstanceOf(NotFoundException);
  });
});