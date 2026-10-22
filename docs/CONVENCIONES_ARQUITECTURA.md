# Guía de Convenciones y Arquitectura - Ojo Ciudadano

**Responsable:** Martín Maier  
**Tarea:** MM-02 — Definir la estructura modular y las convenciones de carpetas  
**Proyecto:** Ojo Ciudadano  
**Versión:** 1.3.0 — Sprint 1  

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

## 2. Estructura de Directorios del Backend (`web/backend/`)

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
        │   │   ├── filters/      # Filtros globales (HttpExceptionFilter)
        │   │   ├── proxy/        # Lógica de reenvío proxy hacia microservicios
        │   │   ├── api-gateway.controller.ts
        │   │   ├── api-gateway.module.ts
        │   │   ├── api-gateway.service.ts
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
        │   │   ├── filters/      # Manejo homogéneo de excepciones (MM-04)
        │   │   ├── roles/        # Dominio de Roles y Permisos
        │   │   │   ├── dto/      # DTOs de entrada y salida
        │   │   │   ├── roles.controller.ts
        │   │   │   ├── roles.module.ts
        │   │   │   └── roles.service.ts
        │   │   ├── usuarios/     # Dominio de Usuarios
        │   │   │   ├── dto/
        │   │   │   ├── usuarios.controller.ts
        │   │   │   ├── usuarios.module.ts
        │   │   │   └── usuarios.service.ts
        │   │   ├── auth/         # Estrategias JWT, Guards, Login y Register
        │   │   ├── prisma/       # PrismaService y PrismaModule (acceso a BD)
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
        │   │   ├── filters/      # Manejo homogéneo de excepciones (MM-04)
        │   │   ├── categorias/   # Categorías de incidentes
        │   │   ├── evidencias/   # Manejo de imágenes (Supabase Storage)
        │   │   ├── incidentes/   # CRUD y ciclo de vida de incidentes (dominio principal)
        │   │   │   ├── dto/      # create-incidente.dto.ts
        │   │   │   ├── incidentes.controller.ts
        │   │   │   └── incidentes.service.ts
        │   │   ├── prisma/       # PrismaService y PrismaModule
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
            │   ├── filters/      # Manejo homogéneo de excepciones (MM-04)
            │   ├── prisma/       # PrismaService y PrismaModule
            │   ├── notificaciones.controller.ts
            │   ├── notificaciones.module.ts
            │   ├── notificaciones.service.ts
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

## 4. Estructura Interna por Dominio (Clean Architecture)

Cada carpeta de dominio (por ejemplo `usuarios/` o `incidentes/`) debe respetar la siguiente división de responsabilidades:

1. **Controlador (`*.controller.ts`)**:
   * Responsabilidad exclusiva de manejar peticiones HTTP, recibir y responder JSON, definir códigos de estado (`@HttpCode`) y delegar la lógica de negocio al Servicio.
   * **No contiene lógica de negocio ni consultas directas a base de datos.**

2. **Servicio (`*.service.ts`)**:
   * Contiene la lógica de negocio pura, validaciones de dominio y orquestación con la capa de datos (`PrismaService`).
   * No interactúa directamente con objetos de transporte HTTP (`Request`, `Response`).

3. **DTOs (`dto/*.dto.ts`)**:
   * Definen la forma de los datos de entrada y salida mediante clases TypeScript decoradas con `class-validator` y `class-transformer`.
   * Los controladores deben tipar estrictamente el `@Body()`, `@Query()` y `@Param()` con estos DTOs.

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
