import { NestFactory } from '@nestjs/core';
import { Logger, RequestMethod, ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { NestExpressApplication } from '@nestjs/platform-express';
import { ApiGatewayModule } from './api-gateway.module';
import { HttpExceptionFilter } from './presentation/filters/http-exception.filter';
import { buildCorsOptions, parseCorsOrigins } from './infrastructure/config/cors.config';
import { setupSwagger, SWAGGER_DOCS_PATH } from './infrastructure/config/swagger.config';

export const GATEWAY_BODY_PARSER_LIMIT = '12mb';

async function bootstrap() {
  const logger = new Logger('ApiGateway');
  const app = await NestFactory.create<NestExpressApplication>(ApiGatewayModule);
  const configService = app.get(ConfigService);

  // Configurar límite de carga útil de 12 MB para soportar transferencia de evidencias (#86, #127)
  app.useBodyParser('json', { limit: GATEWAY_BODY_PARSER_LIMIT });
  app.useBodyParser('urlencoded', {
    limit: GATEWAY_BODY_PARSER_LIMIT,
    extended: true,
  });

  // Habilitar CORS restringido a la lista explícita de orígenes permitidos por entorno (#127)
  app.enableCors(buildCorsOptions(configService));

  // Prefijo global de la API pública v1 (MM-06 / ADR #126)
  app.setGlobalPrefix('api/v1', {
    exclude: [{ path: 'health', method: RequestMethod.GET }],
  });

  // Publicar documentación OpenAPI en /api/docs (#156)
  setupSwagger(app);

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

  const port = configService.get<number>('PORT') ?? process.env.PORT ?? 3000;
  const publicApiUrl =
    configService.get<string>('PUBLIC_API_URL') ??
    `http://localhost:${port}`;
  const allowedOrigins = parseCorsOrigins(
    configService.get<string>('CORS_ORIGINS'),
  );

  await app.listen(port);
  logger.log(
    `API Gateway corriendo en ${publicApiUrl}/api/v1 | Swagger en ${publicApiUrl}/${SWAGGER_DOCS_PATH} (CORS permitido para: ${allowedOrigins.join(', ')})`,
  );
}
bootstrap();
