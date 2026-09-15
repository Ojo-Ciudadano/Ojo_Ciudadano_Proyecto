import { NestFactory } from '@nestjs/core';
import { Logger } from '@nestjs/common';
import { AuthUsuariosModule } from './auth-usuarios.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('AuthUsuarios');
  const app = await NestFactory.create(AuthUsuariosModule);

  // Filtro global para manejo homogéneo de excepciones (MM-04)
  app.useGlobalFilters(new HttpExceptionFilter());

  const port = process.env.PORT ?? process.env.port ?? 3001;
  await app.listen(port);
  logger.log(`Microservicio auth-usuarios corriendo en http://localhost:${port}`);
}
bootstrap();
