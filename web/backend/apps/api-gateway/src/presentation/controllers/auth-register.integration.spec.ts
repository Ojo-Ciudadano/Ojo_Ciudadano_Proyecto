/// <reference types="jest" />
import { INestApplication, ValidationPipe } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { Test, TestingModule } from '@nestjs/testing';
import { AxiosError, AxiosResponse } from 'axios';
import { of, throwError } from 'rxjs';
import request from 'supertest';
import { App } from 'supertest/types';
import { AuthService } from '../../application/services/auth.service';
import { HttpExceptionFilter } from '../filters/http-exception.filter';
import { AuthController } from './auth.controller';

describe('AuthController - registro en API Gateway (#315)', () => {
  let app: INestApplication<App>;
  let httpRequestMock: jest.Mock;

  beforeEach(async () => {
    httpRequestMock = jest.fn();

    const moduleRef: TestingModule = await Test.createTestingModule({
      controllers: [AuthController],
      providers: [
        AuthService,
        {
          provide: HttpService,
          useValue: { request: httpRequestMock },
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((_key: string, defaultValue?: string) => defaultValue),
          },
        },
      ],
    }).compile();

    app = moduleRef.createNestApplication();
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

  afterEach(async () => {
    await app?.close();
  });

  it('acepta un registro valido sin telefono y reenvia los datos normalizados a auth-usuarios', async () => {
    const usuarioCreado = {
      id: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
      nombre: 'Camila Soto',
      email: 'camila.soto@ojociudadano.cl',
      telefono: null,
      fechaRegistro: '2026-11-23T12:00:00.000Z',
    };

    httpRequestMock.mockReturnValue(
      of({ status: 201, data: usuarioCreado } as AxiosResponse),
    );

    const res = await request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({
        nombre: '  Camila Soto  ',
        email: '  Camila.Soto@OjoCiudadano.CL  ',
        password: 'ClaveFicticia123!',
      })
      .expect(201);

    expect(res.body).toEqual(usuarioCreado);
    expect(httpRequestMock).toHaveBeenCalledWith({
      method: 'POST',
      url: 'http://localhost:3001/auth/register',
      data: {
        nombre: 'Camila Soto',
        email: 'camila.soto@ojociudadano.cl',
        password: 'ClaveFicticia123!',
      },
    });
  });

  it('acepta un registro valido con telefono informado', async () => {
    const usuarioCreado = {
      id: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
      nombre: 'Camila Soto',
      email: 'camila.soto@ojociudadano.cl',
      telefono: '+56987654321',
      fechaRegistro: '2026-11-23T12:00:00.000Z',
    };

    httpRequestMock.mockReturnValue(
      of({ status: 201, data: usuarioCreado } as AxiosResponse),
    );

    const res = await request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({
        nombre: 'Camila Soto',
        email: 'camila.soto@ojociudadano.cl',
        password: 'ClaveFicticia123!',
        telefono: '  +56987654321  ',
      })
      .expect(201);

    expect(res.body).toEqual(usuarioCreado);
    expect(httpRequestMock).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({ telefono: '+56987654321' }),
      }),
    );
  });

  it.each([
    [
      'nombre solo espacios',
      {
        nombre: '   ',
        email: 'camila@ojociudadano.cl',
        password: 'ClaveFicticia123!',
      },
    ],
    [
      'correo invalido',
      {
        nombre: 'Camila Soto',
        email: 'no-es-correo',
        password: 'ClaveFicticia123!',
      },
    ],
    [
      'contrasena corta',
      {
        nombre: 'Camila Soto',
        email: 'camila@ojociudadano.cl',
        password: '12345',
      },
    ],
    [
      'telefono invalido',
      {
        nombre: 'Camila Soto',
        email: 'camila@ojociudadano.cl',
        password: 'ClaveFicticia123!',
        telefono: 'abc',
      },
    ],
    [
      'telefono vacio con espacios',
      {
        nombre: 'Camila Soto',
        email: 'camila@ojociudadano.cl',
        password: 'ClaveFicticia123!',
        telefono: '   ',
      },
    ],
    [
      'campo no permitido (roles)',
      {
        nombre: 'Camila Soto',
        email: 'camila@ojociudadano.cl',
        password: 'ClaveFicticia123!',
        roles: ['ADMIN'],
      },
    ],
  ])(
    'rechaza con 400 antes de contactar a auth-usuarios cuando falla la validacion: %s',
    async (_caso, payload) => {
      const res = await request(app.getHttpServer())
        .post('/api/v1/auth/register')
        .send(payload)
        .expect(400);

      expect(res.body).toMatchObject({
        statusCode: 400,
        error: 'Bad Request',
        path: '/api/v1/auth/register',
      });
      expect(httpRequestMock).not.toHaveBeenCalled();
    },
  );

  it('conserva la respuesta 409 Conflict cuando el correo ya esta registrado en auth-usuarios', async () => {
    const conflictPayload = {
      statusCode: 409,
      timestamp: '2026-11-23T12:00:00.000Z',
      path: '/auth/register',
      error: 'Conflict',
      message: 'Ya existe una cuenta con ese correo',
    };

    const axiosError = {
      isAxiosError: true,
      message: 'Request failed with status code 409',
      response: {
        status: 409,
        data: conflictPayload,
      },
    } as AxiosError;

    httpRequestMock.mockReturnValue(throwError(() => axiosError));

    const res = await request(app.getHttpServer())
      .post('/api/v1/auth/register')
      .send({
        nombre: 'Camila Soto',
        email: 'duplicado@ojociudadano.cl',
        password: 'ClaveFicticia123!',
      })
      .expect(409);

    expect(res.body).toMatchObject({
      statusCode: 409,
      error: 'Conflict',
      message: 'Ya existe una cuenta con ese correo',
    });
  });
});
