import { Module } from '@nestjs/common';
import { CategoriasController } from '../presentation/controllers/categorias.controller';
import { CategoriasService } from '../application/services/categorias.service';
import { PrismaModule } from '../infrastructure/persistence/prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [CategoriasController],
  providers: [CategoriasService]
})
export class CategoriasModule {}
