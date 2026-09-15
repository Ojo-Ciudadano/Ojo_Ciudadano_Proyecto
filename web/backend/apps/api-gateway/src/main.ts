import { NestFactory } from '@nestjs/core';
import { Logger } from '@nestjs/common';
import { ApiGatewayModule } from './api-gateway.module';
import { HttpExceptionFilter } from './filters/http-exception.filter';

async function bootstrap() {
  const logger = new Logger('ApiGateway');
  const app = await NestFactory.create(ApiGatewayModule);

  // Filtro global para formato uniforme de errores (MM-03)
  app.useGlobalFilters(new HttpExceptionFilter());

  const port = process.env.PORT ?? process.env.port ?? 3000;
  await app.listen(port);
  logger.log(`API Gateway corriendo en http://localhost:${port}`);
}
bootstrap();
