import type { INestApplication } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule, type OpenAPIObject } from '@nestjs/swagger';

export const SWAGGER_DOCS_PATH = 'api/docs';

const ERROR_SCHEMA_REF = { $ref: '#/components/schemas/ErrorResponseFormat' };

function errorResponse(statusCode: number, error: string, message: string | string[], path: string) {
  return {
    content: {
      'application/json': {
        schema: ERROR_SCHEMA_REF,
        example: {
          statusCode,
          timestamp: '2026-10-30T14:00:00.000Z',
          path,
          error,
          message,
        },
      },
    },
  };
}

export function buildOpenApiDocument(app: INestApplication): OpenAPIObject {
  const config = new DocumentBuilder()
    .setTitle('Ojo Ciudadano — API Gateway (Sprint 2)')
    .setDescription(
      'Documentación OpenAPI de los contratos backend expuestos bajo `/api/v1` (ADR-001 #126) para los módulos de Auth/Usuarios, Incidentes, Evidencias y Notificaciones.',
    )
    .setVersion('1.0.0')
    .addBearerAuth(
      {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
        description:
          'Ingrese el token JWT obtenido en POST /api/v1/auth/login (sin el prefijo Bearer).',
      },
      'bearer',
    )
    .addTag('Auth', 'Registro de ciudadanos e inicio de sesión con emisión de JWT')
    .addTag('Usuarios', 'Perfil propio (/usuarios/me), listado paginado y gestión de roles (#118, #121, #122)')
    .addTag('Incidentes', 'Reporte, filtros, detalle ampliado, estados, historial, prioridad y asignación (#117, #119, #120, #123, #124, #125)')
    .addTag('Evidencias', 'Subida multipart/form-data, consulta de metadata y descarga binaria de fotografías (#86, #106)')
    .addTag('Categorías', 'Catálogo de categorías de incidentes y prioridad base')
    .addTag('Notificaciones', 'Consulta de notificaciones del usuario autenticado (#114)')
    .build();

  const document = SwaggerModule.createDocument(app, config);

  document.components = document.components ?? {};
  document.components.schemas = {
    ...document.components.schemas,
    ErrorResponseFormat: {
      type: 'object',
      required: ['statusCode', 'timestamp', 'path', 'error', 'message'],
      properties: {
        statusCode: { type: 'integer', example: 400 },
        timestamp: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-30T14:00:00.000Z',
        },
        path: { type: 'string', example: '/api/v1/incidentes' },
        error: { type: 'string', example: 'Bad Request' },
        message: {
          oneOf: [
            { type: 'string', example: 'Datos de entrada inválidos' },
            {
              type: 'array',
              items: { type: 'string' },
              example: ['El titulo es obligatorio'],
            },
          ],
        },
      },
    },
    RoleEnum: {
      type: 'string',
      enum: ['ADMIN', 'CIUDADANO'],
      example: 'CIUDADANO',
    },
    EstadoIncidenteEnum: {
      type: 'string',
      enum: ['Nuevo', 'En revisión', 'En atención', 'Resuelto', 'Cerrado'],
      example: 'En revisión',
    },
    LoginRequestDto: {
      type: 'object',
      required: ['email', 'password'],
      properties: {
        email: {
          type: 'string',
          format: 'email',
          example: 'ciudadano.ejemplo@ojociudadano.cl',
        },
        password: {
          type: 'string',
          minLength: 6,
          example: 'ClaveFicticiaSegura123!',
        },
      },
    },
    LoginResponseDto: {
      type: 'object',
      required: ['access_token', 'token_type', 'usuario'],
      properties: {
        access_token: {
          type: 'string',
          description: 'Token JWT de ejemplo (ficticio, sin secreto real)',
          example:
            'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJiMGVlYmM5OS05YzBiLTRlZjgtYmI2ZC02YmI5YmQzODBhMTIiLCJlbWFpbCI6ImNpdWRhZGFuby5lamVtcGxvQG9qb2NpdWRhZGFuby5jbCIsInJvbGVzIjpbIkNJVURBREOTyJdfQ.fictitious_signature_example',
        },
        token_type: { type: 'string', example: 'Bearer' },
        usuario: {
          type: 'object',
          required: ['id', 'nombre', 'email', 'roles'],
          properties: {
            id: {
              type: 'string',
              format: 'uuid',
              example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            },
            nombre: { type: 'string', example: 'Camila Soto Riquelme' },
            email: {
              type: 'string',
              format: 'email',
              example: 'ciudadano.ejemplo@ojociudadano.cl',
            },
            roles: {
              type: 'array',
              items: { $ref: '#/components/schemas/RoleEnum' },
              example: ['CIUDADANO'],
            },
          },
        },
      },
    },
    RegisterUsuarioRequestDto: {
      type: 'object',
      required: ['nombre', 'email', 'password'],
      properties: {
        nombre: { type: 'string', example: 'Camila Soto Riquelme' },
        email: {
          type: 'string',
          format: 'email',
          example: 'camila.soto@ojociudadano.cl',
        },
        password: {
          type: 'string',
          minLength: 6,
          example: 'ClaveFicticia123!',
        },
        telefono: { type: 'string', example: '+56987654321' },
      },
    },
    PerfilUsuarioDto: {
      type: 'object',
      required: ['id', 'nombre', 'email', 'telefono', 'fechaRegistro', 'roles'],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        nombre: { type: 'string', example: 'Camila Soto Riquelme' },
        email: {
          type: 'string',
          format: 'email',
          example: 'camila.soto@ojociudadano.cl',
        },
        telefono: {
          type: 'string',
          nullable: true,
          example: '+56987654321',
        },
        fechaRegistro: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-15T12:00:00.000Z',
        },
        roles: {
          type: 'array',
          items: { $ref: '#/components/schemas/RoleEnum' },
          example: ['CIUDADANO'],
        },
      },
    },
    UsuarioConRolesDto: {
      type: 'object',
      required: ['id', 'nombre', 'email', 'roles'],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        nombre: { type: 'string', example: 'Camila Soto Riquelme' },
        email: {
          type: 'string',
          format: 'email',
          example: 'camila.soto@ojociudadano.cl',
        },
        telefono: {
          type: 'string',
          nullable: true,
          example: '+56987654321',
        },
        roles: {
          type: 'array',
          items: { $ref: '#/components/schemas/RoleEnum' },
          example: ['ADMIN'],
        },
      },
    },
    UsuariosPaginadosResponseDto: {
      type: 'object',
      required: ['data', 'meta'],
      properties: {
        data: {
          type: 'array',
          items: { $ref: '#/components/schemas/PerfilUsuarioDto' },
        },
        meta: {
          type: 'object',
          required: ['page', 'limit', 'total', 'totalPages'],
          properties: {
            page: { type: 'integer', example: 1 },
            limit: { type: 'integer', example: 10 },
            total: { type: 'integer', example: 25 },
            totalPages: { type: 'integer', example: 3 },
          },
        },
      },
    },
    UpdateUsuarioRolDto: {
      type: 'object',
      required: ['rol'],
      properties: {
        rol: { $ref: '#/components/schemas/RoleEnum' },
      },
    },
    UpdateUsuarioRolResponseDto: {
      type: 'object',
      required: ['id', 'roles'],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        roles: {
          type: 'array',
          items: { $ref: '#/components/schemas/RoleEnum' },
          example: ['ADMIN'],
        },
      },
    },
    CreateUbicacionDto: {
      type: 'object',
      required: [
        'latitud',
        'longitud',
        'direccionReferencia',
        'comuna',
        'region',
        'sector',
      ],
      properties: {
        latitud: { type: 'number', minimum: -90, maximum: 90, example: -38.7359 },
        longitud: {
          type: 'number',
          minimum: -180,
          maximum: 180,
          example: -72.5904,
        },
        direccionReferencia: {
          type: 'string',
          example: 'Av. Alemania 0422, frente al campus universitario',
        },
        comuna: { type: 'string', example: 'Temuco' },
        region: { type: 'string', example: 'La Araucanía' },
        sector: { type: 'string', example: 'Poniente' },
      },
    },
    CreateIncidenteDto: {
      type: 'object',
      required: [
        'titulo',
        'descripcion',
        'categoriaId',
        'usuarioReportaId',
        'ubicacion',
      ],
      properties: {
        titulo: {
          type: 'string',
          example: 'Luminaria apagada en Av. Alemania',
        },
        descripcion: {
          type: 'string',
          example:
            'El poste de alumbrado público ubicado en la esquina se encuentra apagado desde hace dos noches.',
        },
        categoriaId: {
          type: 'string',
          format: 'uuid',
          example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        },
        usuarioReportaId: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        ubicacion: { $ref: '#/components/schemas/CreateUbicacionDto' },
      },
    },
    CategoriaDetalleResponseDto: {
      type: 'object',
      required: [
        'id',
        'nombre',
        'descripcion',
        'prioridadBase',
        'esEmergenciaCritica',
        'canalOficialContacto',
      ],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        },
        nombre: { type: 'string', example: 'Alumbrado público' },
        descripcion: {
          type: 'string',
          example: 'Fallas o desperfectos en luminarias públicas',
        },
        prioridadBase: { type: 'integer', minimum: 1, maximum: 5, example: 3 },
        esEmergenciaCritica: { type: 'boolean', example: false },
        canalOficialContacto: {
          type: 'string',
          nullable: true,
          example: '1409 Seguridad Municipal',
        },
      },
    },
    EstadoDetalleResponseDto: {
      type: 'object',
      required: ['id', 'nombre', 'orden'],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        },
        nombre: { type: 'string', example: 'Nuevo' },
        orden: { type: 'integer', example: 1 },
      },
    },
    UbicacionDetalleResponseDto: {
      type: 'object',
      required: [
        'id',
        'latitud',
        'longitud',
        'direccionReferencia',
        'comuna',
        'region',
        'sector',
      ],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: '99eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
        },
        latitud: { type: 'number', example: -38.7359 },
        longitud: { type: 'number', example: -72.5904 },
        direccionReferencia: {
          type: 'string',
          example: 'Av. Alemania 0422, frente al campus universitario',
        },
        comuna: { type: 'string', example: 'Temuco' },
        region: { type: 'string', example: 'La Araucanía' },
        sector: { type: 'string', example: 'Poniente' },
      },
    },
    EvidenciaResponseDto: {
      type: 'object',
      required: [
        'id',
        'incidenteId',
        'tipoMime',
        'tamanoArchivo',
        'fechaCreacion',
        'url',
      ],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
        },
        incidenteId: {
          type: 'string',
          format: 'uuid',
          example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        },
        tipoMime: {
          type: 'string',
          enum: ['image/jpeg', 'image/png', 'image/webp'],
          example: 'image/jpeg',
        },
        tamanoArchivo: { type: 'integer', example: 245812 },
        fechaCreacion: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-23T18:30:00.000Z',
        },
        url: {
          type: 'string',
          description:
            'Ruta relativa a la base /api/v1 para descargar el archivo binario',
          example: '/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
        },
      },
    },
    HistorialEstadoResponseDto: {
      type: 'object',
      required: [
        'id',
        'incidenteId',
        'estado',
        'usuarioId',
        'observacion',
        'fecha',
      ],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02',
        },
        incidenteId: {
          type: 'string',
          format: 'uuid',
          example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        },
        estado: { $ref: '#/components/schemas/EstadoDetalleResponseDto' },
        usuarioId: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        observacion: {
          type: 'string',
          nullable: true,
          example: 'Reporte creado',
        },
        fecha: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-29T10:00:00.000Z',
        },
      },
    },
    AsignacionResponseDto: {
      type: 'object',
      required: [
        'id',
        'incidenteId',
        'usuarioAsignadoId',
        'asignadoPorId',
        'fechaAsignacion',
        'activa',
      ],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: '77eebc99-9c0b-4ef8-bb6d-6bb9bd380a55',
        },
        incidenteId: {
          type: 'string',
          format: 'uuid',
          example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        },
        usuarioAsignadoId: {
          type: 'string',
          format: 'uuid',
          example: 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a99',
        },
        asignadoPorId: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        fechaAsignacion: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-29T14:20:00.000Z',
        },
        activa: { type: 'boolean', example: true },
      },
    },
    IncidenteResumenResponseDto: {
      type: 'object',
      required: [
        'id',
        'titulo',
        'descripcion',
        'categoriaId',
        'estadoId',
        'ubicacionId',
        'usuarioReportaId',
        'nivelPrioridad',
        'fechaCreacion',
        'fechaActualizacion',
        'categoria',
        'estado',
        'ubicacion',
        'evidencias',
      ],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        },
        titulo: {
          type: 'string',
          example: 'Luminaria apagada en Av. Alemania',
        },
        descripcion: {
          type: 'string',
          example: 'El poste de alumbrado público no enciende.',
        },
        categoriaId: {
          type: 'string',
          format: 'uuid',
          example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        },
        estadoId: {
          type: 'string',
          format: 'uuid',
          example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
        },
        ubicacionId: {
          type: 'string',
          format: 'uuid',
          example: '99eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
        },
        usuarioReportaId: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        nivelPrioridad: {
          type: 'integer',
          nullable: true,
          minimum: 1,
          maximum: 5,
          example: 3,
        },
        fechaCreacion: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-29T10:00:00.000Z',
        },
        fechaActualizacion: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-29T10:00:00.000Z',
        },
        categoria: { $ref: '#/components/schemas/CategoriaDetalleResponseDto' },
        estado: { $ref: '#/components/schemas/EstadoDetalleResponseDto' },
        ubicacion: { $ref: '#/components/schemas/UbicacionDetalleResponseDto' },
        evidencias: {
          type: 'array',
          items: { $ref: '#/components/schemas/EvidenciaResponseDto' },
        },
      },
    },
    IncidenteDetalleResponseDto: {
      allOf: [
        { $ref: '#/components/schemas/IncidenteResumenResponseDto' },
        {
          type: 'object',
          required: ['historial', 'asignacionActiva'],
          properties: {
            historial: {
              type: 'array',
              description:
                'Historial cronológico ascendente de cambios de estado y asignaciones (#120, #125)',
              items: { $ref: '#/components/schemas/HistorialEstadoResponseDto' },
            },
            asignacionActiva: {
              nullable: true,
              description:
                'Asignación activa vigente del incidente o null si no tiene responsable asignado (#123, #125)',
              allOf: [{ $ref: '#/components/schemas/AsignacionResponseDto' }],
            },
          },
        },
      ],
    },
    UpdateEstadoDto: {
      type: 'object',
      required: ['estado', 'observacion'],
      properties: {
        estado: { $ref: '#/components/schemas/EstadoIncidenteEnum' },
        observacion: {
          type: 'string',
          example: 'Se inicia la evaluación técnica del reporte',
        },
      },
    },
    UpdatePrioridadDto: {
      type: 'object',
      required: ['nivelPrioridad'],
      properties: {
        nivelPrioridad: {
          type: 'integer',
          minimum: 1,
          maximum: 5,
          example: 4,
        },
      },
    },
    AsignarResponsableDto: {
      type: 'object',
      required: ['usuarioAsignadoId'],
      properties: {
        usuarioAsignadoId: {
          type: 'string',
          format: 'uuid',
          example: 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a99',
        },
      },
    },
    NotificacionResponseDto: {
      type: 'object',
      required: ['id', 'usuarioId', 'incidenteId', 'mensaje', 'leido', 'fecha'],
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: '11eebc99-9c0b-4ef8-bb6d-6bb9bd380a77',
        },
        usuarioId: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        incidenteId: {
          type: 'string',
          format: 'uuid',
          example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
        },
        mensaje: {
          type: 'string',
          example:
            'El incidente "Luminaria apagada en Av. Alemania" cambió de estado "Nuevo" a "En revisión".',
        },
        leido: { type: 'boolean', example: false },
        fecha: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-29T10:05:00.000Z',
        },
      },
    },
  };

  document.paths = {
    ...document.paths,
    '/api/v1/auth/register': {
      post: {
        tags: ['Auth'],
        summary: 'Registrar una nueva cuenta de ciudadano',
        description:
          'Crea un nuevo usuario asignándole el rol CIUDADANO por defecto y retorna sus datos sin passwordHash.',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/RegisterUsuarioRequestDto' },
            },
          },
        },
        responses: {
          '201': {
            description: 'Usuario registrado exitosamente.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/PerfilUsuarioDto' },
              },
            },
          },
          '400': {
            description: 'Error de validación en los datos de entrada.',
            ...errorResponse(
              400,
              'Bad Request',
              ['El formato de correo electronico es invalido'],
              '/api/v1/auth/register',
            ),
          },
          '409': {
            description: 'El correo electrónico ya se encuentra registrado.',
            ...errorResponse(
              409,
              'Conflict',
              'Ya existe una cuenta con ese correo',
              '/api/v1/auth/register',
            ),
          },
        },
      },
    },
    '/api/v1/auth/login': {
      post: {
        tags: ['Auth'],
        summary: 'Iniciar sesión y obtener token Bearer JWT',
        description:
          'Valida las credenciales del usuario y retorna un access_token JWT firmado junto con los datos básicos y roles vigentes.',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/LoginRequestDto' },
            },
          },
        },
        responses: {
          '200': {
            description: 'Autenticación exitosa.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/LoginResponseDto' },
              },
            },
          },
          '400': {
            description: 'Error de validación en los datos enviados.',
            ...errorResponse(
              400,
              'Bad Request',
              ['La contrasena debe tener al menos 6 caracteres'],
              '/api/v1/auth/login',
            ),
          },
          '401': {
            description: 'Credenciales inválidas.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Credenciales inválidas',
              '/api/v1/auth/login',
            ),
          },
        },
      },
    },
    '/api/v1/usuarios/me': {
      get: {
        tags: ['Usuarios'],
        security: [{ bearer: [] }],
        summary: 'Consultar el perfil y rol del usuario autenticado (#118)',
        description:
          'Obtiene el perfil y los roles vigentes en PostgreSQL del usuario autenticado a partir del claim sub del token Bearer JWT. Acceso permitido para ADMIN y CIUDADANO.',
        responses: {
          '200': {
            description: 'Perfil del usuario autenticado obtenido correctamente.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/PerfilUsuarioDto' },
              },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/usuarios/me',
            ),
          },
          '404': {
            description: 'Usuario no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Usuario no encontrado.',
              '/api/v1/usuarios/me',
            ),
          },
        },
      },
    },
    '/api/v1/usuarios': {
      get: {
        tags: ['Usuarios'],
        security: [{ bearer: [] }],
        summary: 'Listar usuarios paginados con filtro opcional por rol (#121)',
        description:
          'Operación exclusiva para el rol ADMIN. Retorna usuarios paginados ordenados por fechaRegistro descendente y permite filtrar por rol (ADMIN o CIUDADANO).',
        parameters: [
          {
            name: 'page',
            in: 'query',
            required: false,
            description: 'Número de página (por defecto 1, mínimo 1)',
            schema: { type: 'integer', minimum: 1, default: 1, example: 1 },
          },
          {
            name: 'limit',
            in: 'query',
            required: false,
            description: 'Cantidad de registros por página (por defecto 10, entre 1 y 100)',
            schema: {
              type: 'integer',
              minimum: 1,
              maximum: 100,
              default: 10,
              example: 10,
            },
          },
          {
            name: 'rol',
            in: 'query',
            required: false,
            description: 'Filtrar usuarios por rol vigente',
            schema: { $ref: '#/components/schemas/RoleEnum' },
          },
        ],
        responses: {
          '200': {
            description: 'Listado paginado de usuarios obtenido correctamente.',
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/UsuariosPaginadosResponseDto',
                },
              },
            },
          },
          '400': {
            description: 'Parámetros de paginación o filtro de rol inválidos.',
            ...errorResponse(
              400,
              'Bad Request',
              ['page must not be less than 1'],
              '/api/v1/usuarios',
            ),
          },
          '401': {
            description: 'Usuario no autenticado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/usuarios',
            ),
          },
          '403': {
            description: 'Acceso restringido al rol ADMIN.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso.',
              '/api/v1/usuarios',
            ),
          },
        },
      },
    },
    '/api/v1/usuarios/{id}': {
      get: {
        tags: ['Usuarios'],
        security: [{ bearer: [] }],
        summary: 'Obtener usuario por ID con sus roles vigentes (#123)',
        description:
          'Operación restringida al rol ADMIN. Consulta datos básicos y roles vigentes de un usuario por su UUID.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del usuario',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Usuario obtenido correctamente.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/UsuarioConRolesDto' },
              },
            },
          },
          '401': {
            description: 'Usuario no autenticado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            ),
          },
          '403': {
            description: 'Acceso restringido al rol ADMIN.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso.',
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            ),
          },
          '404': {
            description: 'Usuario no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Usuario no encontrado.',
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            ),
          },
        },
      },
    },
    '/api/v1/usuarios/{id}/rol': {
      patch: {
        tags: ['Usuarios'],
        security: [{ bearer: [] }],
        summary: 'Cambiar el rol de un usuario restringido al Administrador (#122)',
        description:
          'Permite a un usuario con rol ADMIN cambiar el rol de un usuario a ADMIN o CIUDADANO. Rechaza con 403 Forbidden si un administrador intenta degradar su propio rol ADMIN.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del usuario objetivo',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            },
          },
        ],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/UpdateUsuarioRolDto' },
            },
          },
        },
        responses: {
          '200': {
            description: 'Rol del usuario actualizado correctamente.',
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/UpdateUsuarioRolResponseDto',
                },
              },
            },
          },
          '400': {
            description: 'UUID inválido o rol fuera de [ADMIN, CIUDADANO].',
            ...errorResponse(
              400,
              'Bad Request',
              ['El rol debe ser ADMIN o CIUDADANO.'],
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12/rol',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12/rol',
            ),
          },
          '403': {
            description:
              'Permisos insuficientes (requiere ADMIN) o intento de autodegradación del propio rol ADMIN.',
            ...errorResponse(
              403,
              'Forbidden',
              'No puedes quitarte tu propio rol de administrador.',
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12/rol',
            ),
          },
          '404': {
            description: 'Usuario no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Usuario no encontrado.',
              '/api/v1/usuarios/b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12/rol',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes': {
      get: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Listar incidentes con filtros por estado, comuna, categoría y autor (#119, #106)',
        description:
          'Retorna la lista de incidentes ordenada por fechaCreacion descendente, incluyendo sus evidencias disponibles ([] si no posee). Todos los filtros son opcionales y se combinan con operador lógico AND. Acceso permitido para ADMIN y CIUDADANO.',
        parameters: [
          {
            name: 'estadoId',
            in: 'query',
            required: false,
            description: 'UUID del estado actual del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14',
            },
          },
          {
            name: 'categoriaId',
            in: 'query',
            required: false,
            description: 'UUID de la categoría del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
            },
          },
          {
            name: 'comuna',
            in: 'query',
            required: false,
            description:
              'Nombre exacto de la comuna (insensible a mayúsculas/minúsculas y espacios extremos)',
            schema: { type: 'string', example: 'Temuco' },
          },
          {
            name: 'usuarioReportaId',
            in: 'query',
            required: false,
            description: 'UUID del usuario que reportó el incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Listado de incidentes obtenido correctamente.',
            content: {
              'application/json': {
                schema: {
                  type: 'array',
                  items: {
                    $ref: '#/components/schemas/IncidenteResumenResponseDto',
                  },
                },
              },
            },
          },
          '400': {
            description: 'Alguno de los filtros enviados no es un UUID válido o comuna está vacía.',
            ...errorResponse(
              400,
              'Bad Request',
              ['El estadoId debe ser un UUID valido'],
              '/api/v1/incidentes',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes',
            ),
          },
          '403': {
            description: 'Rol no autorizado.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes',
            ),
          },
        },
      },
      post: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Crear un nuevo incidente con prioridad inicial por categoría (#124, #106)',
        description:
          'Crea un incidente en estado "Nuevo", asigna automáticamente nivelPrioridad según prioridadBase de la categoría seleccionada y genera el registro inicial en historial_estado. Acceso permitido para ADMIN y CIUDADANO.',
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/CreateIncidenteDto' },
            },
          },
        },
        responses: {
          '201': {
            description: 'Incidente creado exitosamente.',
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/IncidenteResumenResponseDto',
                },
              },
            },
          },
          '400': {
            description: 'Datos inválidos o la categoría indicada no existe.',
            ...errorResponse(
              400,
              'Bad Request',
              'La categoria indicada no existe',
              '/api/v1/incidentes',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes',
            ),
          },
          '403': {
            description: 'Rol no autorizado.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{id}': {
      get: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Consultar el detalle ampliado de un incidente con historial, asignación activa y evidencias (#125, #106)',
        description:
          'Retorna el incidente con su categoría, estado, ubicación, evidencias fotográficas disponibles, historial cronológico ascendente y asignación activa vigente (o null). Acceso permitido para ADMIN y CIUDADANO.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Detalle ampliado del incidente obtenido correctamente.',
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/IncidenteDetalleResponseDto',
                },
              },
            },
          },
          '400': {
            description: 'El id del incidente no es un UUID válido.',
            ...errorResponse(
              400,
              'Bad Request',
              'El id del incidente debe ser un UUID valido',
              '/api/v1/incidentes/id-invalido',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            ),
          },
          '403': {
            description: 'Rol no autorizado.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{id}/historial': {
      get: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Consultar el historial de estados de un incidente (#120)',
        description:
          'Devuelve los registros de historial del incidente en orden cronológico ascendente (fecha asc, id asc). Si el incidente no existe responde 404 Not Found. Acceso permitido para ADMIN y CIUDADANO.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Historial del incidente obtenido correctamente.',
            content: {
              'application/json': {
                schema: {
                  type: 'array',
                  items: {
                    $ref: '#/components/schemas/HistorialEstadoResponseDto',
                  },
                },
              },
            },
          },
          '400': {
            description: 'El id del incidente no es un UUID válido.',
            ...errorResponse(
              400,
              'Bad Request',
              'El id del incidente debe ser un UUID valido',
              '/api/v1/incidentes/id-invalido/historial',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/historial',
            ),
          },
          '403': {
            description: 'Rol no autorizado.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/historial',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/historial',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{id}/estado': {
      patch: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Cambiar el estado de un incidente con validación de transiciones y registro en historial (#117)',
        description:
          'Exclusivo para el rol ADMIN. Valida la transición solicitada contra la matriz de #116 (Nuevo -> En revisión -> En atención -> Resuelto -> Cerrado / Reapertura Resuelto -> En atención), actualiza el incidente, registra en historial_estado y emite notificación al ciudadano reportante.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/UpdateEstadoDto' },
            },
          },
        },
        responses: {
          '200': {
            description: 'Estado del incidente actualizado correctamente.',
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/IncidenteResumenResponseDto',
                },
              },
            },
          },
          '400': {
            description: 'UUID inválido, nombre de estado no canónico u observación vacía.',
            ...errorResponse(
              400,
              'Bad Request',
              [
                'El estado debe ser uno de: Nuevo, En revisión, En atención, Resuelto, Cerrado',
              ],
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/estado',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/estado',
            ),
          },
          '403': {
            description: 'Acceso permitido únicamente a usuarios con rol ADMIN.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/estado',
            ),
          },
          '404': {
            description: 'Incidente o estado destino no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/estado',
            ),
          },
          '409': {
            description:
              'Conflicto de transición: la transición solicitada está prohibida por la matriz #116 (incluye transición al mismo estado actual).',
            ...errorResponse(
              409,
              'Conflict',
              'Transicion no permitida de "Nuevo" a "Resuelto"',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/estado',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{id}/prioridad': {
      patch: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Modificar el nivel de prioridad de un incidente (#124)',
        description:
          'Exclusivo para el rol ADMIN. Permite modificar nivelPrioridad a un valor entero entre 1 y 5 sin alterar categoría, estado ni asignaciones.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/UpdatePrioridadDto' },
            },
          },
        },
        responses: {
          '200': {
            description: 'Prioridad del incidente actualizada correctamente.',
            content: {
              'application/json': {
                schema: {
                  $ref: '#/components/schemas/IncidenteResumenResponseDto',
                },
              },
            },
          },
          '400': {
            description: 'UUID inválido o prioridad fuera del rango entero 1..5.',
            ...errorResponse(
              400,
              'Bad Request',
              ['El nivel de prioridad maximo es 5'],
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/prioridad',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/prioridad',
            ),
          },
          '403': {
            description: 'Acceso permitido únicamente al rol ADMIN.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/prioridad',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/prioridad',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{id}/asignacion': {
      patch: {
        tags: ['Incidentes'],
        security: [{ bearer: [] }],
        summary: 'Asignar o reasignar un responsable a un incidente (#123)',
        description:
          'Exclusivo para el rol ADMIN. Verifica que el usuarioAsignadoId exista y tenga rol ADMIN vigente, desactiva la asignación previa (si existía), crea la nueva asignación activa, registra la trazabilidad en historial_estado y emite notificación.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        requestBody: {
          required: true,
          content: {
            'application/json': {
              schema: { $ref: '#/components/schemas/AsignarResponsableDto' },
            },
          },
        },
        responses: {
          '200': {
            description: 'Asignación activa registrada o confirmada correctamente.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/AsignacionResponseDto' },
              },
            },
          },
          '400': {
            description:
              'UUID inválido o el usuario propuesto no tiene rol ADMIN vigente.',
            ...errorResponse(
              400,
              'Bad Request',
              'El usuario propuesto debe tener rol ADMIN vigente',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/asignacion',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/asignacion',
            ),
          },
          '403': {
            description: 'Acceso permitido únicamente al rol ADMIN.',
            ...errorResponse(
              403,
              'Forbidden',
              'No tienes permisos suficientes para acceder a este recurso',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/asignacion',
            ),
          },
          '404': {
            description: 'Incidente o usuario propuesto no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/asignacion',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{incidenteId}/evidencias': {
      post: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Subir una evidencia fotográfica asociada a un incidente (#86)',
        description:
          'Recibe un único archivo en formato multipart/form-data (image/jpeg, image/png o image/webp) de hasta 10 MB (límite técnico de transporte: 12 MB). Almacena el archivo en Supabase Storage de forma encapsulada y retorna la metadata pública con url relativa.',
        parameters: [
          {
            name: 'incidenteId',
            in: 'path',
            required: true,
            description: 'UUID del incidente al que pertenece la fotografía',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        requestBody: {
          required: true,
          content: {
            'multipart/form-data': {
              schema: {
                type: 'object',
                required: ['archivo'],
                properties: {
                  archivo: {
                    type: 'string',
                    format: 'binary',
                    description:
                      'Fotografía de evidencia (image/jpeg, image/png o image/webp, máx. 10 MB)',
                  },
                },
              },
            },
          },
        },
        responses: {
          '201': {
            description: 'Evidencia subida y registrada exitosamente.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/EvidenciaResponseDto' },
              },
            },
          },
          '400': {
            description:
              'Archivo no adjuntado, múltiples archivos enviados, archivo vacío o tipo MIME no permitido.',
            ...errorResponse(
              400,
              'Bad Request',
              'Tipo de archivo no permitido. Use JPEG, PNG o WebP',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
          '413': {
            description:
              'La fotografía supera el límite funcional de 10 MB o el límite de payload de 12 MB.',
            ...errorResponse(
              413,
              'Payload Too Large',
              'La evidencia supera el tamaño máximo de 10 MB',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
          '502': {
            description: 'Fallo de comunicación con el servicio de almacenamiento de objetos.',
            ...errorResponse(
              502,
              'Bad Gateway',
              'No fue posible almacenar el archivo de evidencia',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
        },
      },
      get: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Listar las evidencias asociadas a un incidente (#86)',
        description:
          'Devuelve un arreglo ordenado por fechaCreacion ascendente con la metadata pública y referencia relativa (/evidencias/:id/archivo) de cada evidencia del incidente.',
        parameters: [
          {
            name: 'incidenteId',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Colección de evidencias del incidente.',
            content: {
              'application/json': {
                schema: {
                  type: 'array',
                  items: { $ref: '#/components/schemas/EvidenciaResponseDto' },
                },
              },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13 no encontrado',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias',
            ),
          },
        },
      },
    },
    '/api/v1/evidencias': {
      post: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Subir evidencia fotográfica indicando incidenteId en multipart o query (#86)',
        description:
          'Ruta alternativa compatible para subir una fotografía en formato multipart/form-data especificando incidenteId en el formulario o en el query string.',
        parameters: [
          {
            name: 'incidenteId',
            in: 'query',
            required: false,
            description: 'UUID del incidente (si no se envía en el cuerpo multipart)',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        requestBody: {
          required: true,
          content: {
            'multipart/form-data': {
              schema: {
                type: 'object',
                required: ['archivo'],
                properties: {
                  incidenteId: {
                    type: 'string',
                    format: 'uuid',
                    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
                  },
                  archivo: {
                    type: 'string',
                    format: 'binary',
                  },
                },
              },
            },
          },
        },
        responses: {
          '201': {
            description: 'Evidencia subida exitosamente.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/EvidenciaResponseDto' },
              },
            },
          },
          '400': {
            description: 'Archivo o incidenteId inválido.',
            ...errorResponse(
              400,
              'Bad Request',
              'Debe adjuntar un archivo de evidencia',
              '/api/v1/evidencias',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/evidencias',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente no encontrado',
              '/api/v1/evidencias',
            ),
          },
          '413': {
            description: 'Archivo supera 10 MB.',
            ...errorResponse(
              413,
              'Payload Too Large',
              'La evidencia supera el tamaño máximo de 10 MB',
              '/api/v1/evidencias',
            ),
          },
        },
      },
      get: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Listar evidencias de un incidente mediante parámetro query incidenteId (#86)',
        parameters: [
          {
            name: 'incidenteId',
            in: 'query',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Listado de evidencias del incidente.',
            content: {
              'application/json': {
                schema: {
                  type: 'array',
                  items: { $ref: '#/components/schemas/EvidenciaResponseDto' },
                },
              },
            },
          },
          '400': {
            description: 'Falta el parámetro incidenteId.',
            ...errorResponse(
              400,
              'Bad Request',
              'El identificador del incidente es obligatorio',
              '/api/v1/evidencias',
            ),
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/evidencias',
            ),
          },
          '404': {
            description: 'Incidente no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Incidente no encontrado',
              '/api/v1/evidencias',
            ),
          },
        },
      },
    },
    '/api/v1/evidencias/{id}': {
      get: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Consultar la metadata pública de una evidencia individual (#86)',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID de la evidencia',
            schema: {
              type: 'string',
              format: 'uuid',
              example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Metadata de la evidencia.',
            content: {
              'application/json': {
                schema: { $ref: '#/components/schemas/EvidenciaResponseDto' },
              },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102',
            ),
          },
          '404': {
            description: 'Evidencia no encontrada.',
            ...errorResponse(
              404,
              'Not Found',
              'Evidencia 8f14e45f-ceea-467a-9a38-7b23d28f9102 no encontrada',
              '/api/v1/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102',
            ),
          },
        },
      },
    },
    '/api/v1/evidencias/{id}/archivo': {
      get: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Descargar o recuperar el archivo binario de una evidencia (#86)',
        description:
          'Devuelve el stream binario de la fotografía con su Content-Type original (image/jpeg, image/png o image/webp), Content-Length y Cache-Control: private, max-age=3600.',
        parameters: [
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID de la evidencia',
            schema: {
              type: 'string',
              format: 'uuid',
              example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Archivo binario de la fotografía.',
            content: {
              'image/jpeg': { schema: { type: 'string', format: 'binary' } },
              'image/png': { schema: { type: 'string', format: 'binary' } },
              'image/webp': { schema: { type: 'string', format: 'binary' } },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
            ),
          },
          '404': {
            description: 'Evidencia o archivo binario no encontrado.',
            ...errorResponse(
              404,
              'Not Found',
              'Evidencia 8f14e45f-ceea-467a-9a38-7b23d28f9102 no encontrada',
              '/api/v1/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
            ),
          },
          '502': {
            description: 'Fallo al recuperar el objeto desde Supabase Storage.',
            ...errorResponse(
              502,
              'Bad Gateway',
              'No fue posible recuperar el archivo de evidencia',
              '/api/v1/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
            ),
          },
        },
      },
    },
    '/api/v1/incidentes/{incidenteId}/evidencias/{id}/archivo': {
      get: {
        tags: ['Evidencias'],
        security: [{ bearer: [] }],
        summary: 'Descargar el archivo binario de una evidencia validando su pertenencia al incidente (#86)',
        parameters: [
          {
            name: 'incidenteId',
            in: 'path',
            required: true,
            description: 'UUID del incidente',
            schema: {
              type: 'string',
              format: 'uuid',
              example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
            },
          },
          {
            name: 'id',
            in: 'path',
            required: true,
            description: 'UUID de la evidencia',
            schema: {
              type: 'string',
              format: 'uuid',
              example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
            },
          },
        ],
        responses: {
          '200': {
            description: 'Archivo binario de la fotografía.',
            content: {
              'image/jpeg': { schema: { type: 'string', format: 'binary' } },
              'image/png': { schema: { type: 'string', format: 'binary' } },
              'image/webp': { schema: { type: 'string', format: 'binary' } },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
            ),
          },
          '404': {
            description:
              'La evidencia no existe o no pertenece al incidente indicado.',
            ...errorResponse(
              404,
              'Not Found',
              'Evidencia 8f14e45f-ceea-467a-9a38-7b23d28f9102 no encontrada para el incidente c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
              '/api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
            ),
          },
        },
      },
    },
    '/api/v1/categorias': {
      get: {
        tags: ['Categorías'],
        security: [{ bearer: [] }],
        summary: 'Listar categorías de incidentes',
        description:
          'Devuelve las categorías ordenadas alfabéticamente junto con su prioridadBase.',
        responses: {
          '200': {
            description: 'Listado de categorías.',
            content: {
              'application/json': {
                schema: {
                  type: 'array',
                  items: {
                    $ref: '#/components/schemas/CategoriaDetalleResponseDto',
                  },
                },
              },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/categorias',
            ),
          },
        },
      },
    },
    '/api/v1/notificaciones': {
      get: {
        tags: ['Notificaciones'],
        security: [{ bearer: [] }],
        summary: 'Consultar las notificaciones del usuario autenticado (#114)',
        description:
          'Retorna exclusivamente las notificaciones asociadas al usuario autenticado (extraído del claim sub del JWT) ordenadas por fecha descendente. Acceso permitido para ADMIN y CIUDADANO.',
        responses: {
          '200': {
            description: 'Listado de notificaciones propias.',
            content: {
              'application/json': {
                schema: {
                  type: 'array',
                  items: {
                    $ref: '#/components/schemas/NotificacionResponseDto',
                  },
                },
              },
            },
          },
          '401': {
            description: 'Token Bearer JWT ausente, inválido o expirado.',
            ...errorResponse(
              401,
              'Unauthorized',
              'Se requiere un token Bearer valido',
              '/api/v1/notificaciones',
            ),
          },
        },
      },
    },
  };

  return document;
}

export function setupSwagger(app: INestApplication): OpenAPIObject {
  const document = buildOpenApiDocument(app);

  // Se publica en /api/docs respetando el enrutamiento del Ingress (/api -> api-gateway-svc)
  SwaggerModule.setup(SWAGGER_DOCS_PATH, app, document, {
    useGlobalPrefix: false,
    swaggerOptions: {
      persistAuthorization: true,
    },
  });

  return document;
}
