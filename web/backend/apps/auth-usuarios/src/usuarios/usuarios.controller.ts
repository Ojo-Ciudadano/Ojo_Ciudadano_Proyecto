import {
  Controller,
  Post,
  Get,
  Patch,
  Body,
  Query,
  Param,
  ParseUUIDPipe,
  UseGuards,
  Req,
} from '@nestjs/common';

import type { Request } from 'express';

import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiParam,
  ApiExcludeEndpoint,
} from '@nestjs/swagger';

import { AuthGuard } from '@nestjs/passport';

import { CreateUsuarioDto } from './dto/create-usuario.dto';
import {
  ListarUsuariosDto,
  UsuariosPaginadosResponseDto,
} from './dto/listar-usuarios.dto';
import { PerfilUsuarioDto } from './dto/perfil-usuario.dto';
import {
  UpdateUsuarioRolDto,
  UpdateUsuarioRolResponseDto,
} from './dto/update-usuario-rol.dto';
import { UsuarioConRolesDto } from './dto/usuario-con-roles.dto';

import { UsuariosService } from './usuarios.service';
import { RolesGuard } from '../auth/guards/roles.guards';
import { Roles, Role } from '../auth/decorators/roles.decorator';

@ApiTags('Usuarios')
@Controller('usuarios')
export class UsuariosController {
  constructor(private readonly usuariosService: UsuariosService) {}

  // Obtener el perfil del usuario autenticado (Issue #118)
  @Get('me')
  @UseGuards(AuthGuard('jwt'))
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Obtener el perfil y roles vigentes del usuario autenticado',
    description:
      'Resuelve exclusivamente el perfil del usuario portador del token Bearer JWT (claim sub) y devuelve sus roles vigentes en PostgreSQL (#118). Acceso permitido para roles ADMIN y CIUDADANO.',
  })
  @ApiResponse({
    status: 200,
    description: 'Perfil obtenido correctamente.',
    type: PerfilUsuarioDto,
  })
  @ApiResponse({
    status: 401,
    description: 'Token inválido, expirado o ausente.',
  })
  @ApiResponse({
    status: 404,
    description: 'Usuario no encontrado.',
  })
  async obtenerMiPerfil(
    @Req() request: Request & { user: { userId: string } },
  ) {
    return this.usuariosService.obtenerMiPerfil(request.user.userId);
  }

  // Obtener usuario por ID con sus roles vigentes (Issue #123)
  @Get(':id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles(Role.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Obtener usuario por ID con sus roles vigentes (Solo ADMIN)',
    description:
      'Consulta la información y roles vigentes de un usuario específico por su UUID (#123). Restringido al rol ADMIN.',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del usuario a consultar',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  @ApiResponse({
    status: 200,
    description: 'Usuario obtenido correctamente.',
    type: UsuarioConRolesDto,
  })
  @ApiResponse({
    status: 401,
    description: 'Usuario no autenticado.',
  })
  @ApiResponse({
    status: 403,
    description: 'Acceso permitido únicamente a administradores.',
  })
  @ApiResponse({
    status: 404,
    description: 'Usuario no encontrado.',
  })
  async obtenerUsuarioPorId(@Param('id') id: string) {
    return this.usuariosService.obtenerUsuarioPorIdConRoles(id);
  }

  // Endpoint stub excluido de OpenAPI para no presentar como disponible una operación pendiente (#156)
  @Post('registro')
  @ApiExcludeEndpoint()
  registrar(@Body() _createUsuarioDto: CreateUsuarioDto) {
    // La implementación lógica real está en POST /auth/register
    return { mensaje: 'Endpoint documentado' };
  }

  // Listar usuarios con paginación y filtro por rol (Issue #121)
  @Get()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles(Role.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Listar usuarios con paginación y filtro opcional por rol (Solo ADMIN)',
    description:
      'Devuelve un listado paginado de usuarios ordenado por fechaRegistro descendente con metadatos de paginación (page, limit, total, totalPages) y filtro opcional por rol ADMIN o CIUDADANO (#121).',
  })
  @ApiResponse({
    status: 200,
    description: 'Listado paginado de usuarios obtenido correctamente.',
    type: UsuariosPaginadosResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'Parámetros de consulta inválidos (page < 1, limit > 100 o rol no válido).',
  })
  @ApiResponse({
    status: 401,
    description: 'Usuario no autenticado.',
  })
  @ApiResponse({
    status: 403,
    description: 'Acceso permitido únicamente a administradores.',
  })
  async listarUsuarios(@Query() filtros: ListarUsuariosDto) {
    return this.usuariosService.listarUsuarios(filtros);
  }

  // Cambiar el rol de un usuario (Issue #122)
  @Patch(':id/rol')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles(Role.ADMIN)
  @ApiBearerAuth()
  @ApiOperation({
    summary: 'Cambiar el rol de un usuario (Solo ADMIN)',
    description:
      'Permite a un administrador cambiar el rol de un usuario a ADMIN o CIUDADANO (#122). Impide que un administrador se quite su propio rol ADMIN (403 Forbidden).',
  })
  @ApiParam({
    name: 'id',
    description: 'UUID del usuario cuyo rol se actualizará',
    example: 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
  })
  @ApiResponse({
    status: 200,
    description: 'Rol actualizado correctamente.',
    type: UpdateUsuarioRolResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'UUID o rol inválido.',
  })
  @ApiResponse({
    status: 401,
    description: 'Usuario no autenticado.',
  })
  @ApiResponse({
    status: 403,
    description: 'Permisos insuficientes o intento de autodegradación de rol ADMIN.',
  })
  @ApiResponse({
    status: 404,
    description: 'Usuario no encontrado.',
  })
  async actualizarRolUsuario(
    @Param('id', new ParseUUIDPipe()) usuarioId: string,
    @Body() datos: UpdateUsuarioRolDto,
    @Req() request: Request & { user: { userId: string } },
  ) {
    return this.usuariosService.actualizarRolUsuario(
      usuarioId,
      datos.rol,
      request.user.userId,
    );
  }
}
