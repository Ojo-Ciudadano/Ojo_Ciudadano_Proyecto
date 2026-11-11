import { Controller, Get } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { CategoriaDetalleResponseDto } from '../../application/dto/incidente-detalle-response.dto';
import { CategoriasService } from '../../application/services/categorias.service';

@ApiTags('Categorías')
@ApiBearerAuth()
@Controller('categorias')
export class CategoriasController {
  constructor(private readonly categoriasService: CategoriasService) {}

  @Get()
  @ApiOperation({
    summary: 'Listar el catálogo de categorías de incidentes',
    description:
      'Retorna las categorías disponibles ordenadas alfabéticamente por nombre, incluyendo su prioridadBase (#124).',
  })
  @ApiResponse({
    status: 200,
    description: 'Catálogo de categorías obtenido correctamente.',
    type: [CategoriaDetalleResponseDto],
  })
  @ApiResponse({
    status: 401,
    description: 'Token Bearer JWT ausente, inválido o expirado.',
  })
  findAll() {
    return this.categoriasService.findAll();
  }
}
