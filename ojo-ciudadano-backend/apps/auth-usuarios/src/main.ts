import { NestFactory } from '@nestjs/core';
import { AuthUsuariosModule } from './auth-usuarios.module';

async function bootstrap() {
  const app = await NestFactory.create(AuthUsuariosModule);
  await app.listen(process.env.port ?? 3001);
}
bootstrap();
