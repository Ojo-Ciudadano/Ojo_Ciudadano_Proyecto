import { ApiProperty } from '@nestjs/swagger';

export class CreateIncidenteDto {
  @ApiProperty({ 
    example: 'Bache peligroso en la vía', 
    description: 'Título breve y descriptivo del incidente' 
  })
  titulo!: string;

  @ApiProperty({ 
    example: 'Hay un hoyo profundo en la pista derecha que daña los vehículos.', 
    description: 'Descripción detallada de la situación' 
  })
  descripcion!: string;

  @ApiProperty({ 
    example: 1, 
    description: 'ID de la categoría a la que pertenece el incidente' 
  })
  categoriaId!: number;
}