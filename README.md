# 👁️ Ojo Ciudadano - Plataforma de Reportes e Incidentes

Sistema colaborativo para el reporte, seguimiento y gestión de incidentes urbanos, desarrollado como proyecto conjunto entre estudiantes de 2° y 4° año.

---

## 🏗️ Estructura del Repositorio (Monorepo)

```text
Ojo_Ciudadano_Proyecto/
├── docs/                      # Documentación transversal de arquitectura y convenciones
│   └── CONVENCIONES_ARQUITECTURA.md
├── mobile/                    # App Móvil Android (4° Año) - Kotlin nativo, GPS, Cámara
├── web/                       # Plataforma Web y Microservicios (2° Año)
│   ├── frontend/              # Frontend Web (React + Vite + Tailwind/MUI + React-Leaflet)
│   └── backend/               # Microservicios NestJS + Prisma ORM + PostgreSQL
│       ├── apps/
│       │   ├── api-gateway/       # Gateway centralizado (Port 3000, Proxy & Auth Routing)
│       │   ├── auth-usuarios/     # Microservicio de Autenticación y Usuarios (Port 3001)
│       │   ├── incidentes/        # Microservicio de Incidentes y Evidencias (Port 3002)
│       │   └── notificaciones/    # Microservicio de Notificaciones (Port 3003)
│       ├── docker-compose.yml     # Orquestación de bases de datos PostgreSQL independientes
│       ├── package.json           # Dependencias globales del backend monorepo
│       └── tsconfig.json          # Configuración TypeScript
└── README.md                  # Este archivo
```

---

## 🚀 Inicio Rápido (Backend Web)

### 1. Requisitos Previos
* **Node.js**: v20 o superior
* **Docker Desktop** (para los contenedores PostgreSQL)
* **npm** v10 o superior

### 2. Configuración del Backend
```bash
# Navegar a la carpeta backend
cd web/backend

# Instalar dependencias
npm install

# Levantar las bases de datos PostgreSQL en Docker
docker compose up -d

# Ejecutar las pruebas unitarias
npm test

# Iniciar el API Gateway en modo desarrollo
npm run start:dev api-gateway
```

Para más detalles sobre la arquitectura de 5 capas, convención de nombres y estándares de código, consulta [CONVENCIONES_ARQUITECTURA.md](docs/CONVENCIONES_ARQUITECTURA.md).
