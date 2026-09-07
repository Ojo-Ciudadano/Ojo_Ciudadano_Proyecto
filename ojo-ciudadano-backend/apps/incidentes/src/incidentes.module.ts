import { Module } from '@nestjs/common';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';

@Module({
  imports: [],
  controllers: [IncidentesController],
  providers: [IncidentesService],
})
export class IncidentesModule {}
