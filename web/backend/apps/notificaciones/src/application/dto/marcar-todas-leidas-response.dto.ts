import { ApiProperty } from '@nestjs/swagger';

export class MarcarTodasLeidasResponseDto {
  @ApiProperty({
    description:
      'Cantidad de notificaciones pendientes del usuario que pasaron a leídas. Es 0 si ya estaban todas leídas.',
    example: 2,
    minimum: 0,
  })
  marcadas!: number;
}
