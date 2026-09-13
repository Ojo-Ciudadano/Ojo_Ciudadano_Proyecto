import { NestFactory } from '@nestjs/core';
import { AuthUsuariosModule } from './auth-usuarios.module';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';

async function bootstrap() {
  const app = await NestFactory.create(AuthUsuariosModule);

  const config = new DocumentBuilder()
    .setTitle('Ojo Ciudadano - Servicio de Autenticación')
    .setDescription('Contratos de API para registro, inicio de sesión y gestión de roles')
    .setVersion('1.0')
    .build();
    
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  await app.listen(process.env.port ?? 3001);
}
bootstrap();