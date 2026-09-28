# Informe de Pruebas de Extremo a Extremo (E2E) - Sprint 1

**Fecha de ejecución:** 28 de septiembre de 2026  
**Responsable:** Bernardo Sandoval  
**Entorno:** Local; Gateway ejecutado con `JWT_SECRET` temporal de proceso. Auth, Incidentes y PostgreSQL no estaban disponibles.

## 1. Objetivo

Verificar la integración correcta entre la capa de enrutamiento (API Gateway) y los microservicios subyacentes, comprobando que el flujo de autenticación y protección de rutas funciona según los contratos definidos.

## 2. Resultados de Ejecución

> **Resultado general: REPROBADO / INCOMPLETO.** Se ejecutaron peticiones HTTP reales contra el Gateway. El rechazo sin token funcionó; el proxy de Auth respondió `502` y no se obtuvo JWT. Por tanto, el acceso autenticado no pudo probarse.

### Flujo 1: Registro y Emisión de Token (Casos 01 y 02)

* **Estado:** REPROBADO
* **Registro:** `POST http://localhost:3000/api/v1/auth/register` respondió `502 Bad Gateway` con `Auth service no disponible`.
* **Login:** `POST http://localhost:3000/api/v1/auth/login` respondió `502 Bad Gateway` con `Auth service no disponible`. No se obtuvo token JWT.
* **Evidencia:** Las peticiones llegaron al Gateway, pero no había Auth escuchando en `localhost:3001`. Además, el código de `auth-usuarios` revisado no expone `/auth/register`: contiene dos declaraciones `login` y el registro visible está en `/usuarios/registro` como endpoint de documentación sin lógica de persistencia.
* **Captura:** No se generó una captura de Postman/Swagger; se registraron los status y cuerpos HTTP en la terminal.

### Flujo 2: Protección de Recursos (Caso 03)

* **Estado:** APROBADO (solo para el rechazo del Gateway)
* **Observación:** `POST http://localhost:3000/api/v1/incidentes`, con un JSON válido y sin token, respondió `401 Unauthorized`.
* **Respuesta recibida:** `{"statusCode":401,"error":"Unauthorized","message":"Se requiere un token Bearer valido"}`.
* **Captura:** No se generó una captura de Postman/Swagger; se registró la respuesta HTTP.

### Flujo 3: Consumo Autenticado (Caso 04)

* **Estado:** NO EJECUTADO (BLOQUEADO)
* **Observación:** No se pudo repetir la petición con `Authorization: Bearer <token>` porque el login no entregó un token. No se obtuvo respuesta `201 Created` y no se afirma que el incidente se haya creado.
* **Captura:** No disponible; no se envió una petición autenticada.

## 3. Conclusión

La protección de la ruta de incidentes rechazó correctamente la petición sin credenciales. Sin embargo, el flujo E2E completo **no está operativo en esta ejecución**: Auth no respondió al Gateway, no se emitió un JWT y el caso autenticado quedó bloqueado. No hay evidencia de creación de incidentes (`201`).

Para completar la prueba, se requiere habilitar Docker, iniciar las bases de datos y servicios, corregir la ruta/lógica de registro de Auth, generar el cliente Prisma de Incidentes y confirmar que el Gateway escucha en el puerto `3000`. Después se debe repetir el flujo y capturar las respuestas reales `401 Unauthorized` y `201 Created`.

**Bloqueos observados en esta ejecución:**

* El comando `docker` no está instalado o no está disponible en el PATH; tampoco se encontró PostgreSQL local.
* No había servicios escuchando en los puertos `3001` (Auth), `3002` (Incidentes) ni `5432`-`5434` (bases de datos).
* `web/backend/docker-compose.yml` define contenedores PostgreSQL; no define los tres microservicios.
* `npx nest build api-gateway` compiló correctamente; Auth también compiló. `npx nest build incidentes` falló porque falta `apps/incidentes/src/generated/prisma/client`.
* El Gateway requiere `JWT_SECRET` para arrancar. Se usó una clave temporal solo en el proceso de prueba, sin guardarla en archivos.
