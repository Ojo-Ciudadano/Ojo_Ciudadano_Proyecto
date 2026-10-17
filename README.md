# Ojo Ciudadano - Plataforma de Reportes e Incidentes

Sistema colaborativo para el reporte, seguimiento y gestión de incidentes urbanos, desarrollado como proyecto conjunto entre estudiantes de 2° y 4° año.

---

## Estructura del Repositorio (Monorepo)

```text
Ojo_Ciudadano_Proyecto/
├── docs/                      # Documentación transversal de arquitectura y contratos
│   ├── CONVENCIONES_ARQUITECTURA.md
│   └── ESTRATEGIA_DESPLIEGUE_WEB_K8S.md
├── mobile/                    # Aplicación Móvil Android (4° Año) - Kotlin nativo, GPS, Cámara
├── web/                       # Plataforma Web y Backend de Microservicios (2° Año)
│   ├── frontend/              # Plataforma Web de Gestión (React + TypeScript + Vite + Leaflet)
│   └── backend/               # Monorepo NestJS + Prisma ORM + PostgreSQL
│       ├── apps/
│       │   ├── api-gateway/       # API Gateway centralizado (Proxy, JWT, Swagger)
│       │   ├── auth-usuarios/     # Microservicio de Autenticación y Usuarios
│       │   ├── incidentes/        # Microservicio de Incidentes y Evidencias
│       │   └── notificaciones/    # Microservicio de Notificaciones
│       ├── docker-compose.yml     # Orquestación de bases de datos PostgreSQL
│       ├── package.json           # Dependencias y scripts globales del backend
│       └── tsconfig.json          # Configuración TypeScript del monorepo
└── README.md                  # Guía general del proyecto
```

---

## Mapeo de Puertos y Servicios

### Microservicios (HTTP)
| Servicio | Directorio | Puerto | Descripción |
| :--- | :--- | :---: | :--- |
| **API Gateway** | `apps/api-gateway` | `3000` | Punto de entrada único, routing proxy y documentación Swagger |
| **Auth & Usuarios** | `apps/auth-usuarios` | `3001` | Gestión de usuarios, roles y autenticación |
| **Incidentes** | `apps/incidentes` | `3002` | Registro de incidentes, categorías y evidencias georreferenciadas |
| **Notificaciones** | `apps/notificaciones` | `3003` | Despacho de notificaciones y alertas |

### Bases de Datos (PostgreSQL en Docker)
| Contenedor | Puerto Local | Base de Datos | Usuario |
| :--- | :---: | :--- | :--- |
| `postgres-auth-usuarios` | `5432` | `auth_usuarios` | `auth_user` |
| `postgres-incidentes` | `5433` | `incidentes` | `incidentes_user` |
| `postgres-notificaciones` | `5434` | `notificaciones` | `notificaciones_user` |

---

## Inicio Rápido (Backend Web)

### 1. Requisitos Previos
* **Node.js**: v20 o superior
* **npm**: v10 o superior
* **Docker Desktop** (en ejecución para las bases de datos)

### 2. Instalación y Dependencias
```bash
# Navegar al directorio de backend
cd web/backend

# Instalar dependencias del monorepo
npm install
```

### 3. Configuración de Variables de Entorno
```bash
# Copiar plantilla de variables de entorno base
cp .env.example .env
```

### 4. Levantar Bases de Datos (Docker)
```bash
# Iniciar los tres contenedores PostgreSQL independientes
docker compose up -d
```

### 5. Generar Clientes Prisma
```bash
# Generar los clientes Prisma correspondientes a cada esquema
npx prisma generate --schema=apps/auth-usuarios/prisma/schema.prisma
npx prisma generate --schema=apps/incidentes/prisma/schema.prisma
npx prisma generate --schema=apps/notificaciones/prisma/schema.prisma
```

### 6. Ejecución en Modo Desarrollo
Puedes iniciar cada servicio de forma individual según se requiera:

```bash
# API Gateway
npm run start:dev api-gateway

# Microservicio de Autenticación y Usuarios
npm run start:dev auth-usuarios

# Microservicio de Incidentes
npm run start:dev incidentes

# Microservicio de Notificaciones
npm run start:dev notificaciones
```

### 7. Pruebas y Calidad de Código
```bash
# Ejecutar suite de pruebas unitarias
npm test

# Ejecutar análisis estático (Linter)
npm run lint
```

---

## Documentación de Arquitectura

* **Convenciones y Arquitectura de Backend:** [docs/CONVENCIONES_ARQUITECTURA.md](docs/CONVENCIONES_ARQUITECTURA.md)
* **Estrategia de Despliegue Web en Kubernetes UCT (#128):** [docs/ESTRATEGIA_DESPLIEGUE_WEB_K8S.md](docs/ESTRATEGIA_DESPLIEGUE_WEB_K8S.md)
