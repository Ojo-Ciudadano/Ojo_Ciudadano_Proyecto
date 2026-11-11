# Guía de Convenciones y Arquitectura - Ojo Ciudadano

**Responsable:** Martín Maier  
**Tarea:** MM-02 — Definir la estructura modular y las convenciones de carpetas  
**Proyecto:** Ojo Ciudadano  
**Versión:** 1.5.0 — Sprint 3 (Clean Architecture)  

---

## 1. Visión y Enfoque Arquitectónico Global

El sistema **Ojo Ciudadano** sigue una arquitectura en capas distribuida, coordinada entre la plataforma web (2° Año) y la aplicación móvil (4° Año):

```text
Ojo_Ciudadano_Proyecto/           # Raíz del repositorio
├── docs/                         # Documentación transversal de arquitectura y contratos
├── mobile/                       # Aplicación Móvil Android (4° Año - Kotlin / GPS + Cámara)
└── web/                          # Plataforma Web y Backend de Microservicios
    ├── frontend/                 # Plataforma Web de Gestión (2° Año - React + Vite + Leaflet)
    └── backend/                  # Monorepo NestJS con API Gateway y Microservicios
```

### Capas del Sistema (según Diagrama Oficial de Arquitectura):

1. **Capa 1: Capa de Presentación (Clientes)**:
   * **Aplicación Móvil Android (4° Año)**: Desarrollada en Kotlin con captura de GPS nativo y cámara para reportes en terreno. Consume el API Gateway mediante HTTPS / REST / JSON con autenticación JWT.
   * **Plataforma Web de Gestión (2° Año)**: Desarrollada en React + TypeScript con Vite, React Router, interfaz en MUI o Tailwind y visualización cartográfica con **React-Leaflet**.
2. **Capa 2: Capa de Integración / API Común (`api-gateway`)**:
   * **API Gateway**: Desarrollado en NestJS + TypeScript.
   * Punto único de entrada para Web y Móvil.
   * Responsabilidades: Enrutamiento proxy hacia microservicios de negocio, validación de JWT + Roles, Documentación interactiva Swagger / OpenAPI y formateo uniforme de errores.
3. **Capa 3: Servicios de Negocio (Backend)**:
   * **Microservicio de Auth/Usuarios** (Puerto `3001`): Registro, autenticación, hash de contraseñas, roles y permisos.
   * **Microservicio de Incidentes** (Puerto `3002`): Gestión del ciclo de vida de incidentes, categorías, georreferenciación e integración con mapas OpenStreetMap.
   * **Microservicio de Notificaciones** (Puerto `3003`): Envíos de notificaciones, alertas y mensajería de eventos.
4. **Capa 4: Datos / Persistencia**:
   * **Patrón Database-per-Service**: Cada microservicio cuenta con su propia base de datos PostgreSQL independiente (PostgreSQL 16 en Docker Compose para desarrollo local).
   * **ORM**: **Prisma 7** con `@prisma/adapter-pg` en cada microservicio.
   * **Almacenamiento de Archivos (Evidencias/Fotos)**: Supabase Storage para almacenamiento de archivos multimedia y fotografías de incidentes.
5. **Capa 5: Servicios Externos**:
   * **OpenStreetMap Foundation**: Servicio de mapas base y datos cartográficos consumido por la plataforma Web (vía React-Leaflet) y el microservicio de incidentes.
   * **GPS del Dispositivo**: Ubicación capturada directamente desde el hardware Android.

---

## 2. Estructura de Directorios del Backend (`web/backend/` — Clean Architecture)

Cada microservicio dentro de `web/backend/apps/` organiza su carpeta `src/` en las 4 capas de **Arquitectura Limpia (Clean Architecture)**:

* **`domain/`**: Reglas de negocio puras, constantes de dominio, matrices de transición de estados y escalas operativas sin dependencias de transporte ni base de datos.
* **`application/`**: Casos de uso (`services/`), contratos de transferencia (`dto/`) y transformadores de modelos (`mappers/`).
* **`infrastructure/`**: Adaptadores técnicos hacia el exterior: persistencia con Prisma 7 (`persistence/prisma/`), almacenamiento en Supabase (`storage/`), clientes HTTP entre microservicios (`clients/`), seguridad/JWT/guards (`security/`) y configuración técnica (`config/`).
* **`presentation/`**: Puntos de entrada HTTP (`controllers/`) y filtros globales de excepciones (`filters/`).

```text
web/
├── frontend/                     # Plataforma Web React (Vite + React-Leaflet)
└── backend/                      # Monorepo NestJS
    ├── docker-compose.yml        # 3 Contenedores PostgreSQL independientes
    ├── package.json              # Dependencias compartidas y scripts del monorepo
    ├── tsconfig.json             # Configuración global de TypeScript
    │
    └── apps/
        ├── api-gateway/          # Gateway y orquestación de rutas (Puerto 3000)
        │   ├── src/
        │   │   ├── application/
        │   │   │   ├── dto/              # login.dto.ts, register.dto.ts
        │   │   │   └── services/         # api-gateway.service.ts, auth.service.ts, proxy.service.ts
        │   │   ├── infrastructure/
        │   │   │   ├── config/           # cors.config.ts
        │   │   │   └── security/         # jwt-auth.guard.ts, roles.guard.ts, roles.decorator.ts
        │   │   ├── presentation/
        │   │   │   ├── controllers/      # api-gateway.controller.ts, auth.controller.ts, proxy.controller.ts
        │   │   │   └── filters/          # http-exception.filter.ts
        │   │   ├── auth/                 # auth.module.ts
        │   │   ├── proxy/                # proxy.module.ts
        │   │   ├── api-gateway.module.ts
        │   │   └── main.ts
        │   ├── test/             # Pruebas e2e del gateway
        │   └── tsconfig.app.json
        │
        ├── auth-usuarios/        # Microservicio de Autenticación y Usuarios (Puerto 3001)
        │   ├── prisma.config.ts  # Configuración Prisma 7 (DATABASE_URL_AUTH)
        │   ├── prisma/
        │   │   ├── schema.prisma # Modelos: Usuario, Rol, UsuarioRol
        │   │   └── migrations/   # Historial de migraciones SQL
        │   ├── src/
        │   │   ├── application/
        │   │   │   ├── dto/              # create-usuario.dto.ts, login.dto.ts, listar-usuarios.dto.ts, etc.
        │   │   │   └── services/         # auth.service.ts, usuarios.service.ts, roles.service.ts
        │   │   ├── infrastructure/
        │   │   │   ├── persistence/prisma/ # prisma.service.ts, prisma.module.ts
        │   │   │   └── security/         # jwt.strategy.ts, roles.guard.ts, roles.decorator.ts
        │   │   ├── presentation/
        │   │   │   ├── controllers/      # auth.controller.ts, usuarios.controller.ts, roles.controller.ts
        │   │   │   └── filters/          # http-exception.filter.ts
        │   │   ├── auth/                 # auth.module.ts
        │   │   ├── roles/                # roles.module.ts
        │   │   ├── usuarios/             # usuarios.module.ts
        │   │   ├── auth-usuarios.module.ts
        │   │   └── main.ts
        │   ├── test/
        │   └── tsconfig.app.json
        │
        ├── incidentes/           # Microservicio de Incidentes (Puerto 3002)
        │   ├── prisma.config.ts  # Configuración Prisma 7 (DATABASE_URL_INCIDENTES)
        │   ├── prisma/
        │   │   ├── schema.prisma # Modelos: Incidente, Categoria, Evidencia, Ubicacion
        │   │   └── migrations/
        │   ├── src/
        │   │   ├── domain/
        │   │   │   ├── estado/           # transiciones-incidente.constants.ts
        │   │   │   └── prioridad/        # prioridad.constants.ts
        │   │   ├── application/
        │   │   │   ├── dto/              # create-incidente.dto.ts, update-estado.dto.ts, etc.
        │   │   │   ├── mappers/          # incidente.mapper.ts, evidencia.mapper.ts, etc.
        │   │   │   └── services/         # incidentes.service.ts, categorias.service.ts, evidencias.service.ts
        │   │   ├── infrastructure/
        │   │   │   ├── clients/          # auth-usuarios.client.ts, incidentes-notificaciones.client.ts
        │   │   │   ├── persistence/prisma/ # prisma.service.ts, prisma.module.ts
        │   │   │   ├── security/         # jwt-auth.guard.ts, roles.guard.ts, current-user.decorator.ts
        │   │   │   └── storage/          # storage.service.ts, storage.module.ts
        │   │   ├── presentation/
        │   │   │   ├── controllers/      # incidentes.controller.ts, categorias.controller.ts, evidencias.controller.ts
        │   │   │   └── filters/          # http-exception.filter.ts
        │   │   ├── categorias/           # categorias.module.ts
        │   │   ├── evidencias/           # evidencias.module.ts
        │   │   ├── incidentes.module.ts
        │   │   └── main.ts
        │   ├── test/
        │   └── tsconfig.app.json
        │
        └── notificaciones/       # Microservicio de Notificaciones (Puerto 3003)
            ├── prisma.config.ts  # Configuración Prisma 7 (DATABASE_URL_NOTIFICACIONES)
            ├── prisma/
            │   ├── schema.prisma # Modelo: Notificacion
            │   └── migrations/
            ├── src/
            │   ├── application/
            │   │   ├── dto/              # create-notificacion.dto.ts, notificacion-response.dto.ts
            │   │   └── services/         # notificaciones.service.ts
            │   ├── infrastructure/
            │   │   ├── persistence/prisma/ # prisma.service.ts, prisma.module.ts
            │   │   └── security/         # jwt-auth.guard.ts, current-user.decorator.ts
            │   ├── presentation/
            │   │   ├── controllers/      # notificaciones.controller.ts
            │   │   └── filters/          # http-exception.filter.ts
            │   ├── notificaciones.module.ts
            │   └── main.ts
            ├── test/
            └── tsconfig.app.json
```

---

## 3. Convenciones de Nombres y Archivos

Para garantizar coherencia y legibilidad transversal en el equipo:

### 3.1 Nomenclatura de Archivos
* Todo en **kebab-case**, con sufijo descriptivo de su rol en NestJS:
  * Controladores: `<nombre>.controller.ts` (ej. `usuarios.controller.ts`)
  * Servicios: `<nombre>.service.ts` (ej. `usuarios.service.ts`)
  * Módulos: `<nombre>.module.ts` (ej. `usuarios.module.ts`)
  * DTOs: `<accion>-<nombre>.dto.ts` (ej. `create-usuario.dto.ts`, `login.dto.ts`)
  * Mappers: `<nombre>.mapper.ts` (ej. `incidente.mapper.ts`)
  * Filtros: `<nombre>.filter.ts` (ej. `http-exception.filter.ts`)
  * Guards: `<nombre>.guard.ts` (ej. `jwt-auth.guard.ts`, `roles.guard.ts`)
  * Decoradores: `<nombre>.decorator.ts` (ej. `roles.decorator.ts`, `current-user.decorator.ts`)
  * Pruebas: `<nombre>.<tipo>.spec.ts` (ej. `usuarios.controller.spec.ts`)

### 3.2 Nomenclatura de Código
* **Clases**: `PascalCase` con sufijo de tipo: `UsuariosController`, `RolesService`, `CreateIncidenteDto`.
* **Métodos y Funciones**: `camelCase` con verbos de acción: `findById()`, `create()`, `validateUser()`.
* **Variables y Propiedades**: `camelCase`: `userId`, `createdAt`, `passwordHash`.
* **Constantes**: `UPPER_SNAKE_CASE`: `JWT_SECRET`, `DEFAULT_PAGE_LIMIT`.
* **Interfaces y Tipos**: `PascalCase` descriptivos sin prefijo `I`: `UserPayload`, `IncidentFilter`.

---

## 4. Estructura Interna por Capas (Clean Architecture)

Cada microservicio debe respetar la siguiente división estricta de responsabilidades entre sus capas:

1. **Capa de Presentación (`presentation/controllers/*.controller.ts`, `presentation/filters/*.filter.ts`)**:
   * Responsabilidad exclusiva de manejar peticiones HTTP, recibir y responder JSON, definir códigos de estado (`@HttpCode`) y delegar la ejecución a la capa de Aplicación (`application/services/`).
   * **No contiene lógica de negocio ni consultas directas a base de datos.**

2. **Capa de Aplicación (`application/services/*.service.ts`, `application/dto/*.dto.ts`, `application/mappers/*.mapper.ts`)**:
   * Contiene los casos de uso del sistema, validaciones de aplicación, contratos de entrada/salida (`DTOs` decorados con `class-validator`) y mapeo de respuestas (`mappers`).
   * No interactúa directamente con objetos de transporte HTTP (`Request`, `Response`).

3. **Capa de Dominio (`domain/`)**:
   * Contiene las reglas e invariantes de negocio puras (por ejemplo, la matriz de transiciones válidas de estado de un incidente y las cotas de la escala de prioridad).

4. **Capa de Infraestructura (`infrastructure/`)**:
   * Implementa el acceso a bases de datos (`persistence/prisma/`), almacenamiento de objetos (`storage/`), clientes HTTP externos (`clients/`), seguridad JWT (`security/`) y configuración técnica (`config/`).

---

## 5. Estándar de Respuestas y Errores (MM-03 / MM-04)

Todos los servicios y el API Gateway deben responder los errores con una estructura JSON homogénea:

```json
{
  "statusCode": 400,
  "timestamp": "2026-09-11T10:00:00.000Z",
  "path": "/api/v1/auth/login",
  "error": "Bad Request",
  "message": "El correo electrónico no tiene un formato válido"
}
```

* **`statusCode`**: Código numérico HTTP (`400`, `401`, `403`, `404`, `500`).
* **`timestamp`**: Marca de tiempo ISO-8601 del momento del error.
* **`path`**: URL de la petición recibida.
* **`error`**: Nombre canónico del error HTTP.
* **`message`**: Detalle del error entendible por el frontend o cliente, o arreglo de mensajes en fallas de validación.

---

## 6. Persistencia y Bases de Datos (Docker & Prisma 7)

### 6.1 Contenedores PostgreSQL Locales (`docker-compose.yml`)
Cada microservicio opera con su propia base de datos aislada en PostgreSQL 16:
* **`postgres-auth-usuarios`**: Puerto `5432:5432`, BD: `auth_usuarios`, Usuario: `auth_user`.
* **`postgres-incidentes`**: Puerto `5433:5432`, BD: `incidentes`, Usuario: `incidentes_user`.
* **`postgres-notificaciones`**: Puerto `5434:5432`, BD: `notificaciones`, Usuario: `notificaciones_user`.

Comando para inicializar la infraestructura local:
```bash
docker compose up -d
```

### 6.2 Configuración de Prisma 7
En Prisma 7, las fuentes de datos se gestionan mediante el archivo `prisma.config.ts` en la raíz de cada app, leyendo la URL de la base de datos desde el `.env` raíz mediante `dotenv`:

```typescript
// apps/<microservicio>/prisma.config.ts
import dotenv from "dotenv";
import path from "node:path";
import { defineConfig, env } from "prisma/config";

dotenv.config({ path: path.resolve(__dirname, "../../.env") });

export default defineConfig({
  schema: "prisma/schema.prisma",
  migrations: {
    path: "prisma/migrations",
  },
  datasource: {
    url: env("DATABASE_URL_<SERVICIO>"),
  },
});
```

### 6.3 Almacenamiento de Evidencias (Supabase Storage)
* Las imágenes y evidencias de incidentes se almacenan en un bucket dedicado en **Supabase Storage**.
* La base de datos de `incidentes` almacena únicamente las URLs o identificadores del archivo generado por Supabase Storage, sin guardar binarios directamente en PostgreSQL.
* El modelo `Evidencia` persiste la metadata de cada evidencia (`clave_archivo`, `tipo_mime`, `tamano_archivo`, `fecha_creacion`) y la relaciona con su incidente mediante clave foránea. El detalle de los campos se documenta en [MIGRACIONES_PRISMA.md](MIGRACIONES_PRISMA.md).

### 6.4 Aplicación de Migraciones
* El pipeline CI/CD **no** aplica migraciones automáticamente: cada migración se ejecuta de forma manual con `npx prisma migrate deploy`.
* En el clúster UCT, PostgreSQL es un `ClusterIP`; el procedimiento completo (contexto de Kubernetes, `kubectl port-forward` hacia `postgres-incidentes-svc` y ejecución de la migración) está en [MIGRACIONES_PRISMA.md](MIGRACIONES_PRISMA.md).
* En desarrollo local se aplican las migraciones sobre los contenedores de `docker-compose.yml` (`postgres-incidentes` en el puerto `5433`).

---

## 7. Variables de Entorno Oficiales

* Cada microservicio y el monorepo deben configurarse mediante `.env` (guiados por los `.env.example`).
* **Puertos de Aplicación HTTP**:
  * `PORT=3000` ➔ `api-gateway`
  * `PORT=3001` ➔ `auth-usuarios`
  * `PORT=3002` ➔ `incidentes`
  * `PORT=3003` ➔ `notificaciones`
* **URLs de Bases de Datos Locales**:
  * `DATABASE_URL_AUTH="postgresql://auth_user:auth_password@localhost:5432/auth_usuarios"`
  * `DATABASE_URL_INCIDENTES="postgresql://incidentes_user:incidentes_password@localhost:5433/incidentes"`
  * `DATABASE_URL_NOTIFICACIONES="postgresql://notificaciones_user:notificaciones_password@localhost:5434/notificaciones"`

---

## 8. Flujo Git y Revisión Cruzada de Pull Requests

* Toda funcionalidad, corrección o tarea documental del Sprint 2 se desarrolla en una rama `feature/<numero-issue>-<slug>` creada desde `integration` y se integra mediante Pull Request hacia `integration` vinculado con `Closes #<numero>`.
* Antes de aprobar y fusionar cualquier entrega, el revisor debe aplicar el [Checklist de Revisión Cruzada de Pull Requests (CHECKLIST_REVISION_CRUZADA_PR.md)](CHECKLIST_REVISION_CRUZADA_PR.md).
