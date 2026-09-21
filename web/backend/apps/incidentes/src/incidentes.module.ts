import { Module } from '@nestjs/common';
import { IncidentesController } from './incidentes.controller';
import { IncidentesService } from './incidentes.service';
import { CategoriasModule } from './categorias/categorias.module';
import { EvidenciasModule } from './evidencias/evidencias.module';
import { PrismaModule } from './prisma/prisma.module';

@Module({
  imports: [PrismaModule, CategoriasModule, EvidenciasModule],
  controllers: [IncidentesController],
  providers: [IncidentesService],
})
export class IncidentesModule {}
