import { Test, TestingModule } from '@nestjs/testing';
import {
  INestApplication,
  UnauthorizedException,
  ValidationPipe,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import request from 'supertest';
import { App } from 'supertest/types';
import { ApiGatewayModule } from './../src/api-gateway.module';
import { HttpExceptionFilter } from '../src/filters/http-exception.filter';
import { AuthService } from '../src/auth/auth-service';
import { buildCorsOptions } from '../src/config/cors.config';

const JWT_SECRET = 'test-jwt-secret';
const ALLOWED_WEB_ORIGIN = 'https://ojociudadano.dev.censei.cl';
const UNAUTHORIZED_WEB_ORIGIN = 'https://sitio-no-autorizado.evil.cl';

const CREDENCIALES_VALIDAS = {
  email: 'admin@ojociudadano.cl',
  password: 'password-seguro',
};
const INCIDENTE = {
  titulo: 'Luminaria apagada',
  descripcion: 'La luminaria de la esquina no funciona',
  categoriaId: '550e8400-e29b-41d4-a716-446655440000',
  usuarioReportaId: '550e8400-e29b-41d4-a716-446655440001',
  ubicacion: {
    latitud: -33.45,
    longitud: -70.66,
    direccionReferencia: 'Av. Central 123',
    comuna: 'Santiago',
    region: 'Metropolitana',
    sector: 'Centro',
  },
};

describe('ApiGatewayController (e2e)', () => {
  let app: INestApplication<App>;
  let token: string;

  beforeEach(async () => {
    process.env.JWT_SECRET = JWT_SECRET;
    process.env.CORS_ORIGINS = `${ALLOWED_WEB_ORIGIN},http://localhost:5173`;
    token = await new JwtService({ secret: JWT_SECRET }).signAsync({
      sub: '550e8400-e29b-41d4-a716-446655440001',
      email: CREDENCIALES_VALIDAS.email,
      roles: ['CIUDADANO'],
    });

    const authService = {
      register: jest
        .fn()
        .mockResolvedValue({ id: 'usuario-1', email: CREDENCIALES_VALIDAS.email }),
      login: jest.fn(async (dto: typeof CREDENCIALES_VALIDAS) => {
        if (
          dto.email !== CREDENCIALES_VALIDAS.email ||
          dto.password !== CREDENCIALES_VALIDAS.password
        ) {
          throw new UnauthorizedException('Credenciales invalidas');
        }
        return { access_token: token, token_type: 'Bearer' };
      }),
    };

    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [ApiGatewayModule],
    })
      .overrideProvider(AuthService)
      .useValue(authService)
      .compile();

    app = moduleFixture.createNestApplication();
    const configService = app.get(ConfigService);
    app.enableCors(buildCorsOptions(configService));
    app.setGlobalPrefix('api/v1');
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

  it('registra un usuario por el gateway', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({ nombre: 'Admin', ...CREDENCIALES_VALIDAS })
      .expect(201)
      .expect(({ body }) => expect(body.email).toBe(CREDENCIALES_VALIDAS.email));
  });

  it('inicia sesion y entrega un JWT', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/login')
      .send(CREDENCIALES_VALIDAS)
      .expect(200)
      .expect(({ body }) => {
        expect(body.access_token).toBe(token);
        expect(body.token_type).toBe('Bearer');
      });
  });

  it('rechaza credenciales invalidas con error estructurado', () => {
    return request(app.getHttpServer())
      .post('/api/v1/auth/login')
      .send({ email: CREDENCIALES_VALIDAS.email, password: 'incorrecta' })
      .expect(401)
      .expect(({ body }) => {
        expect(body).toEqual(
          expect.objectContaining({
            statusCode: 401,
            error: 'Unauthorized',
            path: '/api/v1/auth/login',
          }),
        );
        expect(body.access_token).toBeUndefined();
      });
  });

  it('deniega la creacion de incidentes sin JWT', () => {
    return request(app.getHttpServer())
      .post('/api/v1/incidentes')
      .send(INCIDENTE)
      .expect(401)
      .expect(({ body }) => expect(body.statusCode).toBe(401));
  });

  it('acepta la creacion de incidentes con JWT', async () => {
    const incidenteCreado = { id: 'incidente-1', ...INCIDENTE };
    const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue({
      status: 201,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => incidenteCreado,
    } as Response);

    await request(app.getHttpServer())
      .post('/api/v1/incidentes')
      .set('Authorization', `Bearer ${token}`)
      .send(INCIDENTE)
      .expect(201)
      .expect(({ body }) => expect(body.id).toBe('incidente-1'));

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:3002/incidentes',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(INCIDENTE),
      }),
    );
    fetchMock.mockRestore();
  });

  describe('Evidencias a través del API Gateway (#86)', () => {
    it('reenvía subida multipart/form-data de evidencia en POST /api/v1/incidentes/:id/evidencias', async () => {
      const evidenciaCreada = {
        id: 'ev-1',
        incidenteId: 'inc-1',
        tipoMime: 'image/jpeg',
        tamanoArchivo: 8,
        fechaCreacion: '2026-10-23T15:00:00.000Z',
        url: '/evidencias/ev-1/archivo',
      };
      const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue({
        status: 201,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => evidenciaCreada,
      } as Response);

      const response = await request(app.getHttpServer())
        .post('/api/v1/incidentes/inc-1/evidencias')
        .set('Authorization', `Bearer ${token}`)
        .attach('archivo', Buffer.from('foto-jpg'), {
          filename: 'foto.jpg',
          contentType: 'image/jpeg',
        })
        .expect(201);

      expect(response.body).toEqual(evidenciaCreada);
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:3002/incidentes/inc-1/evidencias',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'content-type': expect.stringContaining('multipart/form-data'),
          }),
          body: expect.any(Uint8Array),
        }),
      );
      fetchMock.mockRestore();
    });

    it('consulta las evidencias de un incidente en GET /api/v1/incidentes/:id/evidencias', async () => {
      const lista = [
        {
          id: 'ev-1',
          incidenteId: 'inc-1',
          tipoMime: 'image/jpeg',
          tamanoArchivo: 8,
          fechaCreacion: '2026-10-23T15:00:00.000Z',
          url: '/evidencias/ev-1/archivo',
        },
      ];
      const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue({
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => lista,
      } as Response);

      const response = await request(app.getHttpServer())
        .get('/api/v1/incidentes/inc-1/evidencias')
        .set('Authorization', `Bearer ${token}`)
        .expect(200);

      expect(response.body).toEqual(lista);
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:3002/incidentes/inc-1/evidencias',
        expect.objectContaining({ method: 'GET' }),
      );
      fetchMock.mockRestore();
    });

    it('descarga el archivo binario de una evidencia en GET /api/v1/evidencias/:id/archivo', async () => {
      const imageBytes = Buffer.from([0xff, 0xd8, 0xff, 0xe0]);
      const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue({
        status: 200,
        headers: new Headers({
          'content-type': 'image/jpeg',
          'content-length': imageBytes.length.toString(),
        }),
        arrayBuffer: async () =>
          imageBytes.buffer.slice(
            imageBytes.byteOffset,
            imageBytes.byteOffset + imageBytes.byteLength,
          ),
      } as Response);

      const response = await request(app.getHttpServer())
        .get('/api/v1/evidencias/ev-1/archivo')
        .set('Authorization', `Bearer ${token}`)
        .buffer(true)
        .parse((res, cb) => {
          const chunks: Buffer[] = [];
          res.on('data', (chunk: Buffer) => chunks.push(chunk));
          res.on('end', () => cb(null, Buffer.concat(chunks)));
        })
        .expect(200);

      expect(response.headers['content-type']).toContain('image/jpeg');
      expect(Buffer.compare(response.body as Buffer, imageBytes)).toBe(0);
      fetchMock.mockRestore();
    });
  });

  describe('CORS y Orígenes por Entorno (#127)', () => {
    it('autoriza solicitud preflight OPTIONS con Authorization desde un origen permitido', async () => {
      const response = await request(app.getHttpServer())
        .options('/api/v1/incidentes')
        .set('Origin', ALLOWED_WEB_ORIGIN)
        .set('Access-Control-Request-Method', 'PATCH')
        .set('Access-Control-Request-Headers', 'Authorization,Content-Type')
        .expect(204);

      expect(response.headers['access-control-allow-origin']).toBe(
        ALLOWED_WEB_ORIGIN,
      );
      expect(response.headers['access-control-allow-methods']).toContain('PATCH');
      expect(response.headers['access-control-allow-headers']).toContain(
        'Authorization',
      );
    });

    it('autoriza solicitud GET/POST desde un origen permitido devolviendo Access-Control-Allow-Origin', async () => {
      const response = await request(app.getHttpServer())
        .get('/api/v1')
        .set('Origin', ALLOWED_WEB_ORIGIN)
        .expect(200);

      expect(response.headers['access-control-allow-origin']).toBe(
        ALLOWED_WEB_ORIGIN,
      );
    });

    it('rechaza CORS para un origen no autorizado omitiendo Access-Control-Allow-Origin', async () => {
      const response = await request(app.getHttpServer())
        .get('/api/v1')
        .set('Origin', UNAUTHORIZED_WEB_ORIGIN)
        .expect(200);

      expect(response.headers['access-control-allow-origin']).toBeUndefined();
    });
  });

  afterEach(async () => {
    await app.close();
  });
});
