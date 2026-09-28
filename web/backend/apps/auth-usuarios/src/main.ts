import 'dotenv/config';
import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import { AuthUsuariosModule } from './auth-usuarios.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('AuthUsuarios');
  const app = await NestFactory.create(AuthUsuariosModule);

  // Filtro global para manejo homogeneo de excepciones (MM-04)
  app.useGlobalFilters(new HttpExceptionFilter());

  // Validacion global de DTOs (MM-05)
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  const port = process.env.PORT ?? process.env.port ?? 3001;
  await app.listen(port);
  logger.log(`Microservicio auth-usuarios corriendo en http://localhost:${port}`);
}
bootstrap();
