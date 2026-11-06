import {
  BadGatewayException,
  BadRequestException,
  NotFoundException,
  PayloadTooLargeException,
} from '@nestjs/common';
import {
  EvidenciasService,
  MAX_EVIDENCIA_BYTES,
  toEvidenciaPublica,
} from './evidencias.service';
import {
  normalizeSupabaseUrl,
  StorageObjectNotFoundError,
  StorageOperationError,
} from '../storage/storage.service';

describe('EvidenciasService', () => {
  const prisma = {
    incidente: { findUnique: jest.fn() },
    evidencia: {
      create: jest.fn(),
      findUnique: jest.fn(),
      findMany: jest.fn(),
    },
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

  describe('subirEvidencia / listarPorIncidente / obtenerMetadata (#86)', () => {
    const fecha = new Date('2026-10-23T12:00:00.000Z');

    it('subirEvidencia retorna DTO público sin exponer claveArchivo', async () => {
      prisma.evidencia.create.mockResolvedValue({
        id: 'ev-1',
        incidenteId: 'inc-1',
        claveArchivo: 'incidentes/inc-1/ev-1.jpg',
        tipoMime: 'image/jpeg',
        tamanoArchivo: 4,
        fechaCreacion: fecha,
      });

      const dto = await service.subirEvidencia(entrada);

      expect(dto).toEqual({
        id: 'ev-1',
        incidenteId: 'inc-1',
        tipoMime: 'image/jpeg',
        tamanoArchivo: 4,
        fechaCreacion: fecha,
        url: '/evidencias/ev-1/archivo',
      });
      expect((dto as any).claveArchivo).toBeUndefined();
    });

    it('listarPorIncidente valida existencia del incidente y retorna evidencias mapeadas', async () => {
      prisma.evidencia.findMany.mockResolvedValue([
        {
          id: 'ev-1',
          incidenteId: 'inc-1',
          claveArchivo: 'incidentes/inc-1/ev-1.jpg',
          tipoMime: 'image/jpeg',
          tamanoArchivo: 1024,
          fechaCreacion: fecha,
        },
      ]);

      const lista = await service.listarPorIncidente('inc-1');

      expect(prisma.incidente.findUnique).toHaveBeenCalledWith({
        where: { id: 'inc-1' },
        select: { id: true },
      });
      expect(lista).toEqual([
        {
          id: 'ev-1',
          incidenteId: 'inc-1',
          tipoMime: 'image/jpeg',
          tamanoArchivo: 1024,
          fechaCreacion: fecha,
          url: '/evidencias/ev-1/archivo',
        },
      ]);
    });

    it('listarPorIncidente lanza 404 si el incidente no existe', async () => {
      prisma.incidente.findUnique.mockResolvedValue(null);

      await expect(service.listarPorIncidente('inexistente')).rejects.toBeInstanceOf(
        NotFoundException,
      );
      expect(prisma.evidencia.findMany).not.toHaveBeenCalled();
    });

    it('obtenerMetadata retorna metadata pública o 404 si no existe', async () => {
      prisma.evidencia.findUnique.mockResolvedValueOnce({
        id: 'ev-2',
        incidenteId: 'inc-1',
        claveArchivo: 'incidentes/inc-1/ev-2.png',
        tipoMime: 'image/png',
        tamanoArchivo: 2048,
        fechaCreacion: fecha,
      });

      const meta = await service.obtenerMetadata('ev-2');
      expect(meta.url).toBe('/evidencias/ev-2/archivo');
      expect((meta as any).claveArchivo).toBeUndefined();

      prisma.evidencia.findUnique.mockResolvedValueOnce(null);
      await expect(service.obtenerMetadata('no-existe')).rejects.toBeInstanceOf(
        NotFoundException,
      );
    });

    it('normalizeSupabaseUrl limpia sufijo /rest/v1/ para compatibilidad con Supabase Storage', () => {
      expect(
        normalizeSupabaseUrl('https://mnqirqjxmamzhoenntnj.supabase.co/rest/v1/'),
      ).toBe('https://mnqirqjxmamzhoenntnj.supabase.co');
      expect(
        normalizeSupabaseUrl('https://mnqirqjxmamzhoenntnj.supabase.co/'),
      ).toBe('https://mnqirqjxmamzhoenntnj.supabase.co');
    });

    it('toEvidenciaPublica omite claveArchivo e incluye url de descarga', () => {
      const pub = toEvidenciaPublica({
        id: 'abc',
        incidenteId: 'inc-9',
        tipoMime: 'image/webp',
        tamanoArchivo: 500,
        fechaCreacion: fecha,
      });
      expect(pub.url).toBe('/evidencias/abc/archivo');
    });
  });
});
