import { Module } from '@nestjs/common';
import { EvidenciasController } from '../presentation/controllers/evidencias.controller';
import { EvidenciasService } from '../application/services/evidencias.service';
import { StorageModule } from '../infrastructure/storage/storage.module';

@Module({
  imports: [StorageModule],
  controllers: [EvidenciasController],
  providers: [EvidenciasService],
  exports: [EvidenciasService],
})
export class EvidenciasModule {}
