import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { App } from 'supertest/types';
import { IncidentesModule } from './../src/incidentes.module';
import { PrismaService } from '../src/prisma/prisma.service';
import {
  StorageObjectNotFoundError,
  StorageService,
} from '../src/storage/storage.service';
import { HttpExceptionFilter } from '../src/filters/http-exception.filter';
import { MAX_EVIDENCIA_BYTES } from '../src/evidencias/evidencias.service';

describe('Incidentes y Evidencias (e2e)', () => {
  let app: INestApplication<App>;

  const mockPrisma = {
    incidente: {
      findUnique: jest.fn(),
      findMany: jest.fn(),
      create: jest.fn(),
    },
    evidencia: {
      create: jest.fn(),
      findMany: jest.fn(),
      findUnique: jest.fn(),
    },
    categoria: {
      findUnique: jest.fn(),
      findMany: jest.fn(),
    },
    estado: {
      findFirst: jest.fn(),
    },
  };

  const mockStorage = {
    upload: jest.fn(),
    download: jest.fn(),
    remove: jest.fn(),
  };

  beforeEach(async () => {
    jest.resetAllMocks();
    process.env.JWT_SECRET = 'test-jwt-secret';
    process.env.SUPABASE_URL = 'https://example.supabase.co/rest/v1/';
    process.env.SUPABASE_SERVICE_ROLE_KEY = 'test-service-role-key';
    process.env.SUPABASE_BUCKET_EVIDENCES = 'incident-evidences';

    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [IncidentesModule],
    })
      .overrideProvider(PrismaService)
      .useValue(mockPrisma)
      .overrideProvider(StorageService)
      .useValue(mockStorage)
      .compile();

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

  it('POST /incidentes/:incidenteId/evidencias sube una fotografía y retorna metadata sin exponer claves internas', async () => {
    const fecha = new Date('2026-10-23T15:00:00.000Z');
    mockPrisma.incidente.findUnique.mockResolvedValue({ id: 'inc-100' });
    mockStorage.upload.mockResolvedValue(undefined);
    mockPrisma.evidencia.create.mockImplementation(async ({ data }: any) => ({
      ...data,
      fechaCreacion: fecha,
    }));

    const fotoBuffer = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]);

    const response = await request(app.getHttpServer())
      .post('/incidentes/inc-100/evidencias')
      .attach('archivo', fotoBuffer, {
        filename: 'evidencia.jpg',
        contentType: 'image/jpeg',
      })
      .expect(201);

    expect(response.body).toEqual(
      expect.objectContaining({
        incidenteId: 'inc-100',
        tipoMime: 'image/jpeg',
        tamanoArchivo: fotoBuffer.length,
        url: expect.stringMatching(/^\/evidencias\/[0-9a-f-]{36}\/archivo$/),
      }),
    );
    expect(response.body.claveArchivo).toBeUndefined();
    expect(mockStorage.upload).toHaveBeenCalledTimes(1);
  });

  it('POST /incidentes/:incidenteId/evidencias retorna 404 si el incidente no existe', async () => {
    mockPrisma.incidente.findUnique.mockResolvedValue(null);

    await request(app.getHttpServer())
      .post('/incidentes/no-existe/evidencias')
      .attach('archivo', Buffer.from('foto'), {
        filename: 'evidencia.jpg',
        contentType: 'image/jpeg',
      })
      .expect(404)
      .expect(({ body }) => {
        expect(body.statusCode).toBe(404);
      });

    expect(mockStorage.upload).not.toHaveBeenCalled();
  });

  it('POST /incidentes/:incidenteId/evidencias acepta cargas de hasta 12 MB en el parser pero rechaza con 413 archivos mayores a 10 MB', async () => {
    mockPrisma.incidente.findUnique.mockResolvedValue({ id: 'inc-100' });
    // Archivo de ~10.5 MB (menor a 12 MB del parser Multer, mayor al límite funcional de 10 MB)
    const buffer10_5MB = Buffer.alloc(MAX_EVIDENCIA_BYTES + 512 * 1024, 0x41);

    await request(app.getHttpServer())
      .post('/incidentes/inc-100/evidencias')
      .attach('archivo', buffer10_5MB, {
        filename: 'grande.jpg',
        contentType: 'image/jpeg',
      })
      .expect(413)
      .expect(({ body }) => {
        expect(body.statusCode).toBe(413);
        expect(body.message).toContain('10 MB');
      });

    expect(mockStorage.upload).not.toHaveBeenCalled();
  });

  it('GET /incidentes/:incidenteId/evidencias retorna la lista de evidencias del incidente', async () => {
    const fecha = new Date('2026-10-23T15:00:00.000Z');
    mockPrisma.incidente.findUnique.mockResolvedValue({ id: 'inc-100' });
    mockPrisma.evidencia.findMany.mockResolvedValue([
      {
        id: 'ev-100',
        incidenteId: 'inc-100',
        claveArchivo: 'incidentes/inc-100/ev-100.png',
        tipoMime: 'image/png',
        tamanoArchivo: 256,
        fechaCreacion: fecha,
      },
    ]);

    const response = await request(app.getHttpServer())
      .get('/incidentes/inc-100/evidencias')
      .expect(200);

    expect(response.body).toEqual([
      {
        id: 'ev-100',
        incidenteId: 'inc-100',
        tipoMime: 'image/png',
        tamanoArchivo: 256,
        fechaCreacion: fecha.toISOString(),
        url: '/evidencias/ev-100/archivo',
      },
    ]);
  });

  it('GET /evidencias/:id/archivo descarga el binario almacenado con su Content-Type original y retorna 404 si no existe', async () => {
    const bytes = Buffer.from([0x89, 0x50, 0x4e, 0x47]);
    mockPrisma.evidencia.findUnique.mockResolvedValueOnce({
      id: 'ev-100',
      incidenteId: 'inc-100',
      claveArchivo: 'incidentes/inc-100/ev-100.png',
      tipoMime: 'image/png',
      tamanoArchivo: bytes.length,
    });
    mockStorage.download.mockResolvedValueOnce({
      buffer: bytes,
      contentType: 'image/png',
    });

    const okRes = await request(app.getHttpServer())
      .get('/evidencias/ev-100/archivo')
      .buffer(true)
      .parse((res, cb) => {
        const chunks: Buffer[] = [];
        res.on('data', (chunk: Buffer) => chunks.push(chunk));
        res.on('end', () => cb(null, Buffer.concat(chunks)));
      })
      .expect(200);

    expect(okRes.headers['content-type']).toContain('image/png');
    expect(Buffer.compare(okRes.body as Buffer, bytes)).toBe(0);

    mockPrisma.evidencia.findUnique.mockResolvedValueOnce({
      id: 'ev-huérfana',
      incidenteId: 'inc-100',
      claveArchivo: 'incidentes/inc-100/missing.png',
      tipoMime: 'image/png',
      tamanoArchivo: 10,
    });
    mockStorage.download.mockRejectedValueOnce(
      new StorageObjectNotFoundError('incidentes/inc-100/missing.png'),
    );

    await request(app.getHttpServer())
      .get('/evidencias/ev-huérfana/archivo')
      .expect(404);
  });
});
