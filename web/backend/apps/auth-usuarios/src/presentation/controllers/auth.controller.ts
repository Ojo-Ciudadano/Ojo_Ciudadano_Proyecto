import {
  Controller,
  Post,
  Get,
  Body,
  HttpCode,
  HttpStatus,
  UseGuards,
  Req,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import {
  ApiExcludeEndpoint,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { AuthService } from '../../application/services/auth.service';
import { LoginDto } from '../../application/dto/login.dto';
import { CreateUsuarioDto } from '../../application/dto/create-usuario.dto';
import { RolesGuard } from '../../infrastructure/security/roles.guard';
import { Roles, Role } from '../../infrastructure/security/roles.decorator';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({
    summary: 'Registrar una nueva cuenta de ciudadano',
    description:
      'Crea un nuevo usuario asignándole por defecto el rol CIUDADANO y devuelve sus datos públicos sin passwordHash.',
  })
  @ApiResponse({
    status: 201,
    description: 'Usuario registrado exitosamente.',
    schema: {
      type: 'object',
      properties: {
        id: {
          type: 'string',
          format: 'uuid',
          example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        },
        nombre: { type: 'string', example: 'Camila Soto Riquelme' },
        email: { type: 'string', example: 'camila.soto@ojociudadano.cl' },
        telefono: {
          type: 'string',
          nullable: true,
          example: '+56987654321',
        },
        fechaRegistro: {
          type: 'string',
          format: 'date-time',
          example: '2026-10-30T15:00:00.000Z',
        },
      },
    },
  })
  @ApiResponse({
    status: 400,
    description: 'Datos de entrada inválidos (correo mal formado o contraseña corta).',
  })
  @ApiResponse({
    status: 409,
    description: 'Conflicto: ya existe una cuenta registrada con ese correo electrónico.',
  })
  async register(@Body() dto: CreateUsuarioDto) {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({
    summary: 'Iniciar sesión y obtener token Bearer JWT',
    description:
      'Autentica las credenciales del usuario y retorna un access_token JWT firmado junto con los datos básicos y roles del usuario.',
  })
  @ApiResponse({
    status: 200,
    description: 'Autenticación exitosa.',
    schema: {
      type: 'object',
      properties: {
        access_token: {
          type: 'string',
          description: 'Token JWT de ejemplo (ficticio, sin secreto real)',
          example:
            'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJiMGVlYmM5OS05YzBiLTRlZjgtYmI2ZC02YmI5YmQzODBhMTIiLCJlbWFpbCI6ImNpdWRhZGFuby5lamVtcGxvQG9qb2NpdWRhZGFuby5jbCIsInJvbGVzIjpbIkNJVURBREOTyJdfQ.fictitious_signature_example',
        },
        token_type: {
          type: 'string',
          example: 'Bearer',
        },
        usuario: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid',
              example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
            },
            nombre: { type: 'string', example: 'Camila Soto Riquelme' },
            email: {
              type: 'string',
              example: 'ciudadano.ejemplo@ojociudadano.cl',
            },
            roles: {
              type: 'array',
              items: { type: 'string', enum: ['ADMIN', 'CIUDADANO'] },
              example: ['CIUDADANO'],
            },
          },
        },
      },
    },
  })
  @ApiResponse({
    status: 400,
    description: 'Formato de credenciales inválido.',
  })
  @ApiResponse({
    status: 401,
    description: 'Credenciales inválidas.',
  })
  async login(@Body() loginDto: LoginDto) {
    return this.authService.login(loginDto);
  }

  @Get('perfil')
  @UseGuards(AuthGuard('jwt'))
  @ApiExcludeEndpoint()
  getPerfil(@Req() req: any) {
    return req.user;
  }

  @Get('admin-only')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles(Role.ADMIN)
  @ApiExcludeEndpoint()
  getAdminData() {
    return { message: 'Bienvenido al panel de administración' };
  }
}
