import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { IsEnum, IsInt, IsOptional, Max, Min } from 'class-validator';
import { Role } from '../../infrastructure/security/roles.decorator';
import { PerfilUsuarioDto } from './perfil-usuario.dto';

export class ListarUsuariosDto {
  @ApiPropertyOptional({
    description: 'Número de página a consultar (base 1)',
    default: 1,
    minimum: 1,
    example: 1,
  })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @IsOptional()
  page: number = 1;

  @ApiPropertyOptional({
    description: 'Cantidad de usuarios por página (entre 1 y 100)',
    default: 10,
    minimum: 1,
    maximum: 100,
    example: 10,
  })
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  @IsOptional()
  limit: number = 10;

  @ApiPropertyOptional({
    description: 'Filtrar usuarios por rol vigente',
    enum: Role,
    example: Role.CIUDADANO,
  })
  @IsEnum(Role)
  @IsOptional()
  rol?: Role;
}

export class PaginacionMetaDto {
  @ApiProperty({ description: 'Página actual', example: 1 })
  page!: number;

  @ApiProperty({ description: 'Límite de registros por página', example: 10 })
  limit!: number;

  @ApiProperty({ description: 'Total de usuarios que cumplen el filtro', example: 25 })
  total!: number;

  @ApiProperty({ description: 'Total de páginas disponibles', example: 3 })
  totalPages!: number;
}

export class UsuariosPaginadosResponseDto {
  @ApiProperty({
    description: 'Arreglo de usuarios de la página solicitada (sin passwordHash)',
    type: () => [PerfilUsuarioDto],
  })
  data!: PerfilUsuarioDto[];

  @ApiProperty({
    description: 'Metadatos de paginación',
    type: () => PaginacionMetaDto,
  })
  meta!: PaginacionMetaDto;
}
