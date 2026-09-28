# Plan de Pruebas de Autenticacion y Acceso (Sprint 1)

## 1. Objetivo

Verificar el registro de usuarios, el inicio de sesion, la emision de un token JWT y el control de acceso a las rutas protegidas de incidentes a traves del API Gateway.

Este documento contiene pruebas manuales contra servicios en ejecucion. Las pruebas automatizadas disponibles se describen en la seccion [Cobertura automatizada](#8-cobertura-automatizada-y-alcance).

## 2. Alcance y rutas

El API Gateway publica las rutas bajo el prefijo `/api/v1`:

| Operacion | Metodo y ruta | Resultado esperado |
| --- | --- | --- |
| Registro | `POST /api/v1/auth/register` | `201 Created` |
| Inicio de sesion | `POST /api/v1/auth/login` | `200 OK` y JWT |
| Crear incidente | `POST /api/v1/incidentes` | `201 Created` con JWT; `401 Unauthorized` sin JWT |

La autenticacion se valida en el API Gateway para las rutas de incidentes, categorias y evidencias. El microservicio de incidentes recibe las solicitudes reenviadas por el gateway.

El login responde con un objeto de esta forma:

```json
{
  "access_token": "<jwt>",
  "token_type": "Bearer"
}
```

## 3. Precondiciones

1. Ejecutar los servicios en los puertos configurados:
   - API Gateway: `http://localhost:3000`
   - `auth-usuarios`: `http://localhost:3001`
   - `incidentes`: `http://localhost:3002`
2. Configurar `JWT_SECRET` con el mismo valor en el API Gateway y en `auth-usuarios`.
3. Configurar las URLs de los microservicios, por ejemplo `AUTH_SERVICE_URL=http://localhost:3001` e `INCIDENTES_SERVICE_URL=http://localhost:3002`.
4. Para las pruebas manuales, tener PostgreSQL disponible, aplicar las migraciones y cargar los datos iniciales requeridos por ambos servicios.
5. Tener una cuenta de prueba. Si se ejecuta el seed de `auth-usuarios`, la cuenta local inicial es `admin@ojociudadano.cl` con contrasena `Admin123!`. Esas credenciales son solo para desarrollo local.
6. Para crear incidentes, usar el UUID de una categoria existente. El `usuarioReportaId` del ejemplo debe corresponder al identificador `sub` del JWT.

> El DTO de login en `auth-usuarios` exige una contrasena de al menos 6 caracteres. El DTO del gateway tiene actualmente un minimo distinto; los casos de este plan usan contrasenas de 6 o mas caracteres para cumplir ambos contratos.

## 4. Datos de prueba

### Registro

```json
{
  "nombre": "Usuario de prueba",
  "email": "usuario.prueba@ojociudadano.cl",
  "password": "Prueba123!",
  "telefono": "+56912345678"
}
```

El telefono es opcional. El correo debe ser unico para que el registro sea exitoso.

### Login

```json
{
  "email": "admin@ojociudadano.cl",
  "password": "Admin123!"
}
```

### Creacion de incidente

Reemplazar `categoriaId` por un UUID existente y `usuarioReportaId` por el valor `sub` del token obtenido en el Caso 02.

```json
{
  "titulo": "Luminaria apagada",
  "descripcion": "La luminaria de la esquina no funciona",
  "categoriaId": "550e8400-e29b-41d4-a716-446655440000",
  "usuarioReportaId": "550e8400-e29b-41d4-a716-446655440001",
  "ubicacion": {
    "latitud": -33.45,
    "longitud": -70.66,
    "direccionReferencia": "Av. Central 123",
    "comuna": "Temuco",
    "region": "Araucania",
    "sector": "Centro"
  }
}
```

Los UUID del bloque son ejemplos de formato, no identificadores garantizados en la base de datos. La categoria debe existir; el microservicio tambien requiere que se haya cargado el estado inicial `Nuevo`.

## 5. Casos de prueba

### Caso 01: Registro exitoso

**Precondicion:** Los servicios `api-gateway` y `auth-usuarios` estan disponibles; el correo de prueba no existe.

**Accion:** Enviar el JSON de registro a `POST http://localhost:3000/api/v1/auth/register`.

```bash
curl -i -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Usuario de prueba","email":"usuario.prueba@ojociudadano.cl","password":"Prueba123!","telefono":"+56912345678"}'
```

**Resultado esperado:** `201 Created`; el cuerpo contiene los datos del usuario creado y no contiene `passwordHash`.

### Caso 02: Inicio de sesion exitoso y emision de JWT

**Precondicion:** Existe la cuenta indicada en la seccion [Datos de prueba](#4-datos-de-prueba), y ambos servicios usan el mismo `JWT_SECRET`.

**Accion:** Enviar las credenciales a `POST http://localhost:3000/api/v1/auth/login`.

```bash
curl -i -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ojociudadano.cl","password":"Admin123!"}'
```

**Resultado esperado:** `200 OK` y un cuerpo con `access_token` no vacio y `token_type` igual a `Bearer`. Guardar el token para los Casos 04 y 05.

### Caso 03: Rechazo por credenciales invalidas

**Precondicion:** Ninguna adicional.

**Accion:** Enviar una cuenta inexistente o una contrasena incorrecta a `POST http://localhost:3000/api/v1/auth/login`.

```bash
curl -i -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ojociudadano.cl","password":"ContrasenaIncorrecta"}'
```

**Resultado esperado:** `401 Unauthorized`, sin `access_token`. El cuerpo sigue el formato del filtro global e incluye `statusCode`, `timestamp`, `path`, `error` y `message`.

### Caso 04: Acceso denegado a incidentes sin token

**Precondicion:** El API Gateway esta disponible.

**Accion:** Enviar el JSON de incidente a `POST http://localhost:3000/api/v1/incidentes` sin cabecera `Authorization`.

```bash
curl -i -X POST http://localhost:3000/api/v1/incidentes \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Luminaria apagada","descripcion":"La luminaria de la esquina no funciona","categoriaId":"550e8400-e29b-41d4-a716-446655440000","usuarioReportaId":"550e8400-e29b-41d4-a716-446655440001","ubicacion":{"latitud":-33.45,"longitud":-70.66,"direccionReferencia":"Av. Central 123","comuna":"Santiago","region":"Metropolitana","sector":"Centro"}}'
```

**Resultado esperado:** `401 Unauthorized` en el API Gateway. La solicitud no debe reenviarse al microservicio de incidentes ni crear datos.

### Caso 05: Acceso autorizado a incidentes con token

**Precondicion:** El Caso 02 fue exitoso. Reemplazar `<JWT>` por `access_token` y los UUID del JSON por identificadores validos de la base de datos.

**Accion:** Enviar el JSON de incidente a `POST http://localhost:3000/api/v1/incidentes` con `Authorization: Bearer <JWT>`.

```bash
curl -i -X POST http://localhost:3000/api/v1/incidentes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"titulo":"Luminaria apagada","descripcion":"La luminaria de la esquina no funciona","categoriaId":"550e8400-e29b-41d4-a716-446655440000","usuarioReportaId":"550e8400-e29b-41d4-a716-446655440001","ubicacion":{"latitud":-33.45,"longitud":-70.66,"direccionReferencia":"Av. Central 123","comuna":"Santiago","region":"Metropolitana","sector":"Centro"}}'
```

**Resultado esperado:** `201 Created` y el incidente creado en la respuesta. Si el token es valido pero la categoria no existe o faltan datos iniciales, el microservicio respondera con un error funcional; eso no equivale a un rechazo de autenticacion.

## 6. Resultados a registrar

Para cada ejecucion, anotar:

| Caso | Estado | Codigo HTTP | Observaciones |
| --- | --- | --- | --- |
| 01 Registro | Pendiente |  |  |
| 02 Login y JWT | Pendiente |  |  |
| 03 Credenciales invalidas | Pendiente |  |  |
| 04 Incidente sin token | Pendiente |  |  |
| 05 Incidente con token | Pendiente |  |  |

## 7. Ejecucion de pruebas automatizadas

Desde `web/backend`, ejecutar:

```bash
npm run test:e2e -- --runInBand
npm test -- --runInBand --runTestsByPath apps/auth-usuarios/src/auth/auth.service.spec.ts
```

La suite e2e del gateway verifica registro/login, rechazo por credenciales y acceso a incidentes. La prueba unitaria verifica la logica del servicio de autenticacion.

## 8. Cobertura automatizada y alcance

Las pruebas automatizadas actuales no sustituyen por completo los casos manuales de este documento:

- En el e2e del gateway, el servicio de autenticacion esta simulado y la respuesta del servicio de incidentes tambien; no se usan PostgreSQL ni los microservicios reales.
- La prueba unitaria de `AuthService` simula `JwtService`; verifica que se solicite firmar el payload esperado, pero no valida una firma JWT emitida por la base de datos/servicio real.
- El guard del gateway si se prueba con tokens JWT firmados para el entorno de test, tanto en el caso sin token como en el acceso autorizado.

Por lo tanto, los Casos 01 a 05 deben ejecutarse contra los procesos reales para confirmar el flujo integrado completo, la persistencia y las respuestas del microservicio de incidentes.