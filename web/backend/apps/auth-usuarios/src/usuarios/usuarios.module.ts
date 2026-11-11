import { Module } from '@nestjs/common';
import { UsuariosController } from '../presentation/controllers/usuarios.controller';
import { UsuariosService } from '../application/services/usuarios.service';
import { PrismaModule } from '../infrastructure/persistence/prisma/prisma.module';
import { RolesGuard } from '../infrastructure/security/roles.guard';

@Module({
  imports: [PrismaModule],
  controllers: [UsuariosController],
  providers: [UsuariosService, RolesGuard],
  exports: [UsuariosService],
})
export class UsuariosModule {}
