import { NestFactory } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import { ApiGatewayModule } from './api-gateway.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('ApiGateway');
  const app = await NestFactory.create(ApiGatewayModule);

  // Habilitar CORS para consumo desde la plataforma Web SPA (React/Vite)
  app.enableCors();

  // Prefijo global de la API pública v1 (MM-06)
  app.setGlobalPrefix('api/v1');

  // Filtro global para formato uniforme de errores (MM-03)
  app.useGlobalFilters(new HttpExceptionFilter());

  // Validacion global de DTOs (MM-05)
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  const port = process.env.PORT ?? process.env.port ?? 3000;
  await app.listen(port);
  logger.log(`API Gateway corriendo en http://localhost:${port}/api/v1`);
}
bootstrap();
