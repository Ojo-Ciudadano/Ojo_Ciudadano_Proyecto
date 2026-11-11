import { Module } from '@nestjs/common';
import { RolesController } from '../presentation/controllers/roles.controller';
import { RolesService } from '../application/services/roles.service';

@Module({
  controllers: [RolesController],
  providers: [RolesService],
  exports: [RolesService],
})
export class RolesModule {}
