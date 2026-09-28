import { Controller, Post, Body } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { CreateUsuarioDto } from './dto/create-usuario.dto';

@ApiTags('Usuarios')
@Controller('usuarios')
export class UsuariosController {
  
  @Post('registro')
  @ApiOperation({ summary: 'Registrar un nuevo ciudadano' })
  @ApiResponse({ status: 201, description: 'El usuario ha sido registrado exitosamente.' })
  @ApiResponse({ status: 400, description: 'Error de validación en los datos de entrada.' })
  registrar(@Body() createUsuarioDto: CreateUsuarioDto) {
    // La implementación lógica la hará Franchesca (FM-03)
    return { mensaje: 'Endpoint documentado' };
  }
}