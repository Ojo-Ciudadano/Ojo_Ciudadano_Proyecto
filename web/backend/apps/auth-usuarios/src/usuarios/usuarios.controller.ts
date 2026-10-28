import { Controller, Post, Get, Body, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AuthGuard } from '@nestjs/passport';
import { CreateUsuarioDto } from './dto/create-usuario.dto';
import { ListarUsuariosDto } from './dto/listar-usuarios.dto';
import { UsuariosService } from './usuarios.service';
import { RolesGuard } from '../auth/guards/roles.guards';
import { Roles, Role } from '../auth/decorators/roles.decorator';

@ApiTags('Usuarios')
@Controller('usuarios')
export class UsuariosController {
  constructor(private readonly usuariosService: UsuariosService) {}

  @Post('registro')
  @ApiOperation({ summary: 'Registrar un nuevo ciudadano' })
  @ApiResponse({ status: 201, description: 'El usuario ha sido registrado exitosamente.' })
  @ApiResponse({ status: 400, description: 'Error de validación en los datos de entrada.' })
  registrar(@Body() _createUsuarioDto: CreateUsuarioDto) {
    // La implementación lógica la hará Franchesca (FM-03)
    return { mensaje: 'Endpoint documentado' };
  }

  // Listar usuarios con paginación y filtro por rol (Issue #121)
  @Get()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles(Role.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Listar usuarios con paginación y filtro por rol' })
  @ApiResponse({ status: 200, description: 'Listado de usuarios obtenido correctamente.' })
  @ApiResponse({ status: 400, description: 'Parámetros de consulta inválidos.' })
  @ApiResponse({ status: 401, description: 'Usuario no autenticado.' })
  @ApiResponse({ status: 403, description: 'Acceso permitido únicamente a administradores.' })
  async listarUsuarios(@Query() filtros: ListarUsuariosDto) {
    return this.usuariosService.listarUsuarios(filtros);
  }
}