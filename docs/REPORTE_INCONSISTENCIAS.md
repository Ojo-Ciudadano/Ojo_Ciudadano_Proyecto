# Reporte de Inconsistencias de la API (Sprint 1)

Auditoria ejecutada el 2026-09-28 contra el API Gateway local en `http://localhost:3000`. Los resultados describen el estado del entorno durante la prueba; no se consideran respuestas esperadas las que no pudieron verificarse por falta de disponibilidad de un microservicio.

| ID | Endpoint | Modulo | Comportamiento Esperado | Comportamiento Real | Severidad | Accion Requerida |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 01 | `POST /api/v1/auth/register` | Auth | Retorna 201 y crea usuario | **HTTP 404**: `Cannot POST /auth/register`. El Gateway recibe la ruta, pero el microservicio `auth-usuarios` no expone esa ruta de registro. | Alta | Implementar el endpoint de registro en `auth-usuarios` y verificar que el Gateway reenvie la solicitud correctamente. |
| 02 | `POST /api/v1/incidentes` | Incidentes | Valida el DTO y retorna 400 si faltan datos | **HTTP 503**: `Microservicio incidentes no disponible en http://localhost:3002`. El cuerpo vacio no alcanzo la validacion del DTO porque el microservicio no inicio. | Media | Generar el cliente Prisma de Incidentes y levantar el servicio; repetir la prueba para verificar el `400` y el formato del filtro global. |
| 03 | `GET /api/v1/incidentes` | Incidentes | Exige token JWT | **HTTP 503** sin token: el microservicio no estaba disponible, por lo que no se pudo observar la respuesta de autenticacion. En el codigo revisado no se encontro una guarda JWT en el controlador de incidentes ni en la ruta proxy del Gateway. | Critica | Verificar el comportamiento en ejecucion y aplicar una guarda JWT si la ruta debe ser privada. |

## Condiciones de la prueba

- Gateway (`3000`) y `auth-usuarios` (`3001`) iniciaron correctamente. El microservicio de Incidentes no compilo: falta `apps/incidentes/src/generated/prisma/client`.
- Las bases de datos no estaban en ejecucion; los puertos `5432`, `5433` y `5434` no tenian listeners. Docker no esta disponible en el entorno de prueba y no existe `web/backend/.env`.
- El registro se probo con `email` y `password` validos; el Gateway respondio `404`.
- El POST vacio y el GET se enviaron sin token. Ambos recibieron `503`, no un `400` ni una respuesta de autorizacion.

## Comandos para repetir la auditoria

Desde la raiz del repositorio, con Docker, variables de entorno y el cliente Prisma de Incidentes preparados:

```bash
cd web/backend
docker compose up -d
npm run start:dev api-gateway
npm run start:dev auth-usuarios
npm run start:dev incidentes
```

Ejecutar cada comando `npm run start:dev` en una terminal separada. Cuando los tres servicios esten disponibles, repetir las solicitudes de la tabla y actualizar los resultados de los casos 02 y 03.