import { ApiProperty } from '@nestjs/swagger';

export class EvidenciaResponseDto {
  @ApiProperty({
    description: 'Identificador único de la evidencia fotográfica',
    format: 'uuid',
    example: '8f14e45f-ceea-467a-9a38-7b23d28f9102',
  })
  id!: string;

  @ApiProperty({
    description: 'UUID del incidente al que pertenece la evidencia',
    format: 'uuid',
    example: 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13',
  })
  incidenteId!: string;

  @ApiProperty({
    description: 'Tipo MIME validado del archivo de imagen',
    enum: ['image/jpeg', 'image/png', 'image/webp'],
    example: 'image/jpeg',
  })
  tipoMime!: string;

  @ApiProperty({
    description: 'Tamaño del archivo en bytes (máximo funcional: 10 MB)',
    example: 245812,
  })
  tamanoArchivo!: number;

  @ApiProperty({
    description: 'Fecha y hora de creación del registro de evidencia (ISO 8601)',
    example: '2026-10-23T18:30:00.000Z',
  })
  fechaCreacion!: Date;

  @ApiProperty({
    description:
      'Ruta relativa (respecto a /api/v1) para descargar el archivo binario sin exponer claves internas de Storage',
    example: '/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo',
  })
  url!: string;
}
