# Guía de Convenciones y Arquitectura - Ojo Ciudadano

**Responsable:** Martín Maier  
**Tarea:** MM-02 — Definir la estructura modular y las convenciones de carpetas  
**Proyecto:** Ojo Ciudadano  
**Versión:** 1.2.0 — Sprint 1  

---

## 1. Visión y Enfoque Arquitectónico Global

El repositorio de **Ojo Ciudadano** está diseñado como un **monorepo integral** que agrupa todas las plataformas de la solución, facilitando la integración transversal entre el equipo web y el equipo móvil:

```text
Ojo_Ciudadano_Proyecto/           # Raíz del repositorio
├── docs/                         # Documentación transversal de arquitectura y decisiones
├── mobile/                       # Solución y aplicación móvil (Flutter / React Native)
└── web/                          # Solución Web y Backend de Microservicios
    ├── frontend/                 # Aplicación Web React (Bárbara Sáez - BSa-01 a BSa-07)
    └── backend/                  # Monorepo NestJS con Microservicios y API Gateway
```

### Capas del Sistema:
1. **Capa 1: Clientes**:
   * Aplicación Web (`web/frontend/`): React + Vite con gestión de sesión y alertas.
   * Aplicación Móvil (`mobile/`): App móvil para reporte ciudadano en terreno.
2. **Capa 2: API Gateway (`web/backend/apps/api-gateway`)**: Punto único de entrada HTTP (puerto `3000`), encargado de enrutamiento proxy hacia microservicios, manejo unificado de excepciones y validación base.
3. **Capa 3: Servicios de Dominio / Microservicios (`web/backend/apps/`)**:
   * **`auth-usuarios`** (puerto `3001`): Autenticación, JWT, roles y gestión de usuarios.
   * **`incidentes`** (puerto `3002`): Registro, categorías, geolocalización, estados y evidencias de incidentes.
   * **`notificaciones`** (puerto `3003`): Envíos de notificaciones y alertas ciudadanas.
4. **Capa 4: Persistencia y Almacenamiento (Base de Datos por Servicio)**:
   * **Patrón Database-per-Service**: Cada microservicio cuenta con su propia base de datos PostgreSQL independiente para garantizar aislamiento y bajo acoplamiento.
   * **Entorno de Infraestructura Local**: Orquestado mediante **Docker Compose** con tres instancias PostgreSQL 16 aisladas.
   * **ORM**: **Prisma 7** con `@prisma/adapter-pg` y clientes dedicados generados por cada microservicio.

---

## 2. Estructura de Directorios del Backend (`web/backend/`)

```text
web/
├── frontend/                     # Plataforma Web React (Vite)
└── backend/                      # Monorepo NestJS
    ├── docker-compose.yml        # Contenedores PostgreSQL independientes por microservicio
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
        │   │   ├── evidencias/   # Manejo de imágenes y archivos adjuntos
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
  \"statusCode\": 400,
  \"timestamp\": \"2026-09-11T10:00:00.000Z\",
  \"path\": \"/api/v1/auth/login\",
  \"error\": \"Bad Request\",
  \"message\": \"El correo electrónico no tiene un formato válido\"
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
