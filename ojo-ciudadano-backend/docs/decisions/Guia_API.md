# Guía de documentación de la API

Estándar de documentación para la API del proyecto **Ojo Ciudadano** mediante Swagger y los decoradores de `@nestjs/swagger`.

## Objetivo

La documentación debe permitir que cualquier integrante del equipo pueda:

- Identificar rápidamente los endpoints de cada módulo.
- Entender qué hace cada operación y qué datos recibe.
- Consultar las respuestas exitosas y los errores esperados.
- Probar los endpoints protegidos desde Swagger utilizando un token JWT.

## Índice

1. [Agrupación de endpoints](#agrupación-de-endpoints)
2. [Descripción de operaciones](#descripción-de-operaciones)
3. [Modelado de datos con DTOs](#modelado-de-datos-con-dtos)
4. [Respuestas HTTP](#respuestas-http)
5. [Seguridad y control de acceso](#seguridad-y-control-de-acceso)
6. [Lista de verificación](#lista-de-verificación)

## Agrupación de endpoints

Cada controlador debe utilizar `@ApiTags()` a nivel de clase. El nombre debe coincidir con el módulo funcional al que pertenece el controlador.

Ejemplos de etiquetas válidas:

- `Usuarios`
- `Incidentes`
- `Autenticación`
- `Categorías`
- `Evidencias`

```typescript
@ApiTags('Usuarios')
@Controller('usuarios')
export class UsuariosController {}
```

## Descripción de operaciones

Cada endpoint debe incluir `@ApiOperation()` con una descripción breve y concreta de su comportamiento.

Se recomienda incluir:

- `summary`: descripción corta para identificar la operación.
- `description`: explicación de la acción, sus efectos y reglas relevantes.

```typescript
@ApiOperation({
	summary: 'Registrar ciudadano',
	description:
		'Crea un nuevo usuario con rol de ciudadano y genera el hash de su contraseña.',
})
```

## Modelado de datos con DTOs

Todas las propiedades de un Data Transfer Object (DTO) deben documentarse con `@ApiProperty()` o `@ApiPropertyOptional()` cuando correspondan.

Cada propiedad debe indicar:

- El tipo de dato esperado.
- Un ejemplo realista.
- Si es opcional.
- Las restricciones relevantes, como longitud, formato o valores permitidos.

```typescript
export class CrearUsuarioDto {
	@ApiProperty({
		example: 'Ana García',
		description: 'Nombre completo del ciudadano.',
	})
	nombre: string;

	@ApiProperty({
		example: 'ana.garcia@example.com',
		description: 'Correo electrónico utilizado para iniciar sesión.',
	})
	correo: string;

	@ApiPropertyOptional({
		example: '+56912345678',
		description: 'Número telefónico de contacto.',
	})
	telefono?: string;
}
```

## Respuestas HTTP

Cada endpoint debe documentar la respuesta exitosa y, como mínimo, los errores de validación y autorización que puedan producirse. Para ello se utiliza `@ApiResponse()`.

La descripción debe explicar el resultado de la operación, no limitarse a repetir el código HTTP.

```typescript
@ApiResponse({
	status: 201,
	description: 'Usuario registrado exitosamente.',
})
@ApiResponse({
	status: 400,
	description: 'Error de validación en los datos enviados.',
})
@ApiResponse({
	status: 401,
	description: 'Token ausente, inválido o expirado.',
})
```

Documenta también otros estados cuando formen parte del comportamiento esperado del endpoint, por ejemplo:

- `404`: recurso no encontrado.
- `409`: conflicto, como un correo ya registrado.
- `500`: error inesperado del servidor, cuando corresponda.

## Seguridad y control de acceso

Los endpoints protegidos deben incluir obligatoriamente `@ApiBearerAuth()`. Esto permite que Swagger envíe el token JWT al realizar pruebas.

```typescript
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Get('perfil')
obtenerPerfil() {}
```

Los endpoints públicos no deben incluir `@ApiBearerAuth()` si no requieren autenticación.

## Lista de verificación

Antes de considerar documentado un controlador, verifica que:

- [ ] El controlador tiene `@ApiTags()`.
- [ ] Cada endpoint tiene `@ApiOperation()`.
- [ ] Todas las propiedades de los DTOs tienen `@ApiProperty()` o `@ApiPropertyOptional()`.
- [ ] Cada propiedad incluye un ejemplo realista.
- [ ] Las propiedades opcionales están identificadas como opcionales.
- [ ] Está documentada la respuesta exitosa.
- [ ] Están documentados los errores de validación y autorización.
- [ ] Los endpoints protegidos tienen `@ApiBearerAuth()`.