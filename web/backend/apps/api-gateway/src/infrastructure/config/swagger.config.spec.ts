import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { ApiGatewayController } from '../../presentation/controllers/api-gateway.controller';
import { ApiGatewayService } from '../../application/services/api-gateway.service';
import { setupSwagger } from './swagger.config';

describe('Swagger OpenAPI Contracts (#156)', () => {
  let app: INestApplication<App>;

  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [ApiGatewayController],
      providers: [ApiGatewayService],
    }).compile();

    app = moduleRef.createNestApplication();
    app.setGlobalPrefix('api/v1');
    setupSwagger(app);
    await app.init();
  });

  afterEach(async () => {
    await app.close();
  });

  it('publica la interfaz Swagger UI en /api/docs y el JSON en /api/docs-json', async () => {
    const uiRes = await request(app.getHttpServer()).get('/api/docs/');
    expect(uiRes.status).toBe(200);
    expect(uiRes.text).toContain('swagger-ui');

    const jsonRes = await request(app.getHttpServer()).get('/api/docs-json');
    expect(jsonRes.status).toBe(200);
    expect(jsonRes.body.openapi).toMatch(/^3\./);
  });

  it('documenta todas las operaciones del alcance de Sprint 2 bajo /api/v1 con esquema Bearer JWT', async () => {
    const { body } = await request(app.getHttpServer()).get('/api/docs-json');

    expect(body.components.securitySchemes).toHaveProperty('bearer');
    expect(body.components.securitySchemes.bearer).toMatchObject({
      type: 'http',
      scheme: 'bearer',
      bearerFormat: 'JWT',
    });

    const paths = body.paths;
    expect(paths['/api/v1/auth/register']).toHaveProperty('post');
    expect(paths['/api/v1/auth/login']).toHaveProperty('post');
    expect(paths['/api/v1/usuarios/me']).toHaveProperty('get');
    expect(paths['/api/v1/usuarios']).toHaveProperty('get');
    expect(paths['/api/v1/usuarios/{id}/rol']).toHaveProperty('patch');

    expect(paths['/api/v1/incidentes']).toHaveProperty('get');
    expect(paths['/api/v1/incidentes']).toHaveProperty('post');
    expect(paths['/api/v1/incidentes/{id}']).toHaveProperty('get');
    expect(paths['/api/v1/incidentes/{id}/estado']).toHaveProperty('patch');
    expect(paths['/api/v1/incidentes/{id}/historial']).toHaveProperty('get');
    expect(paths['/api/v1/incidentes/{id}/prioridad']).toHaveProperty('patch');
    expect(paths['/api/v1/incidentes/{id}/asignacion']).toHaveProperty('patch');

    expect(paths['/api/v1/incidentes/{incidenteId}/evidencias']).toHaveProperty('post');
    expect(paths['/api/v1/incidentes/{incidenteId}/evidencias']).toHaveProperty('get');
    expect(paths['/api/v1/evidencias/{id}']).toHaveProperty('get');
    expect(paths['/api/v1/evidencias/{id}/archivo']).toHaveProperty('get');

    expect(paths['/api/v1/notificaciones']).toHaveProperty('get');

    // Verificar que no expone operaciones pendientes como /usuarios/registro
    expect(paths['/api/v1/usuarios/registro']).toBeUndefined();
  });

  it('incluye filtros de incidentes, paginación de usuarios y errores 401, 403 y 409', async () => {
    const { body } = await request(app.getHttpServer()).get('/api/docs-json');

    const filtrosIncidentes = body.paths['/api/v1/incidentes'].get.parameters.map(
      (p: { name: string }) => p.name,
    );
    expect(filtrosIncidentes).toEqual(
      expect.arrayContaining(['estadoId', 'categoriaId', 'comuna', 'usuarioReportaId']),
    );

    const filtrosUsuarios = body.paths['/api/v1/usuarios'].get.parameters.map(
      (p: { name: string }) => p.name,
    );
    expect(filtrosUsuarios).toEqual(
      expect.arrayContaining(['page', 'limit', 'rol']),
    );

    const respuestasEstado = body.paths['/api/v1/incidentes/{id}/estado'].patch.responses;
    expect(respuestasEstado).toHaveProperty('401');
    expect(respuestasEstado).toHaveProperty('403');
    expect(respuestasEstado).toHaveProperty('409');
  });
});
