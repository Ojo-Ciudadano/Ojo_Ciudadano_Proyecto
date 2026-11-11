import { Body, Controller, HttpCode, Post } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { AuthService } from '../../application/services/auth.service';
import { RegisterDto } from '../../application/dto/register.dto';
import { LoginDto } from '../../application/dto/login.dto';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  @HttpCode(201)
  @ApiOperation({
    summary: 'Registrar una nueva cuenta de ciudadano',
    description:
      'Registra un nuevo usuario con rol CIUDADANO por defecto y devuelve sus datos públicos sin passwordHash.',
  })
  @ApiResponse({
    status: 201,
    description: 'Usuario registrado exitosamente.',
  })
  @ApiResponse({
    status: 400,
    description: 'Error de validación en los datos de entrada.',
  })
  @ApiResponse({
    status: 409,
    description: 'Ya existe una cuenta registrada con ese correo electrónico.',
  })
  register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(200)
  @ApiOperation({
    summary: 'Iniciar sesión y obtener token Bearer JWT',
    description:
      'Autentica las credenciales del usuario y retorna un access_token JWT junto con los datos básicos y roles del usuario.',
  })
  @ApiResponse({
    status: 200,
    description: 'Inicio de sesión exitoso y emisión de token Bearer JWT.',
  })
  @ApiResponse({
    status: 400,
    description: 'Error de validación en los datos de entrada.',
  })
  @ApiResponse({
    status: 401,
    description: 'Credenciales inválidas.',
  })
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }
}
