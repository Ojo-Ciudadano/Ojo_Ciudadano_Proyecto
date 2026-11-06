# Configuración de Orígenes CORS y URLs de Servicios por Entorno

**Responsable:** Martín Maier  
**Issue:** #127 — Configurar los orígenes CORS y las URLs de servicios por entorno  
**Proyecto:** Ojo Ciudadano  
**Versión:** 1.0.0 — Sprint 2  

---

## 1. Objetivo

Establecer una configuración centralizada por entorno para:
1. Los **orígenes Web permitidos (`CORS_ORIGINS`)** en el API Gateway (`api-gateway`).
2. Las **direcciones internas de comunicación (`*_SERVICE_URL`)** desde el API Gateway hacia los microservicios (`auth-usuarios`, `incidentes`, `notificaciones`), manteniendo dichos microservicios sin exposición pública directa.
3. La **URL pública del API Gateway (`VITE_API_URL` / `PUBLIC_API_URL`)** consumida por el cliente Web React.
4. El ajuste del límite de tamaño de cuerpo en el Ingress institucional (`nginx.ingress.kubernetes.io/proxy-body-size: "12m"`) para permitir la transferencia de fotografías de evidencia de hasta 10 MB.

---

## 2. Catálogo de Variables por Componente

### 2.1 Backend — API Gateway y Microservicios (`web/backend`)

| Variable | Propósito | Valor en Desarrollo Local (`.env`) | Valor en Kubernetes UCT (`backend-config` ConfigMap) | Sensible |
| :--- | :--- | :--- | :--- | :---: |
| `PORT` | Puerto de escucha HTTP del API Gateway. | `3000` | `3000` (definido en `containerPort`) | No |
| `PUBLIC_API_URL` | URL base pública donde se expone el API Gateway (sin `/api/v1`). | `http://localhost:3000` | `https://ojociudadano.dev.censei.cl` | No |
| `CORS_ORIGINS` | Lista separada por comas de orígenes Web (`Origin`) autorizados para consumir el API Gateway desde el navegador. | `http://localhost:5173,http://127.0.0.1:5173` | `https://ojociudadano.dev.censei.cl` | No |
| `AUTH_SERVICE_URL` | Dirección interna HTTP del microservicio `auth-usuarios`. | `http://localhost:3001` | `http://auth-svc:3001` | No |
| `INCIDENTES_SERVICE_URL` | Dirección interna HTTP del microservicio `incidentes`. | `http://localhost:3002` | `http://incidentes-svc:3002` | No |
| `NOTIFICACIONES_SERVICE_URL` | Dirección interna HTTP del microservicio `notificaciones`. | `http://localhost:3003` | `http://notificaciones-svc:3003` | No |
| `JWT_SECRET` | Clave simétrica para firma y validación de tokens Bearer JWT. | Definida en `.env` local | `Secret`: `backend-secrets` (`key: jwt-secret`) | **Sí** |
| `SUPABASE_URL` | Endpoint REST/Storage de Supabase para evidencias (#84). | Definida en `.env` local | `ConfigMap`: `supabase-config` | No |
| `SUPABASE_BUCKET_EVIDENCES` | Nombre del bucket de almacenamiento de fotografías (#84). | `incident-evidences` | `ConfigMap`: `supabase-config` | No |
| `SUPABASE_SERVICE_ROLE_KEY` | Llave administrativa de Supabase Storage (#84). | Definida en `.env` local | `Secret`: `supabase-credentials` | **Sí** |

### 2.2 Frontend Web (`web/frontend`)

| Variable | Propósito | Valor en Desarrollo Local (`web/frontend/.env`) | Valor en Kubernetes UCT (Compilación / `ConfigMap`) | Sensible |
| :--- | :--- | :--- | :--- | :---: |
| `VITE_API_URL` | URL pública del API Gateway (el cliente `src/services/api.ts` normaliza barras finales y concatena `/api/v1`). | `http://localhost:3000` | `https://ojociudadano.dev.censei.cl` | No |
| `PORT` | Puerto del servidor de desarrollo de Vite (`npm run dev`). | `5173` | N/A (servido por Nginx en puerto `80`) | No |

---

## 3. Comportamiento de CORS en el API Gateway

La configuración se encuentra centralizada en [`web/backend/apps/api-gateway/src/config/cors.config.ts`](../web/backend/apps/api-gateway/src/config/cors.config.ts) e inyectada en [`main.ts`](../web/backend/apps/api-gateway/src/main.ts):

1. **Orígenes Web Permitidos:**
   * Se lee `CORS_ORIGINS` mediante `ConfigService`.
   * Cuando un navegador envía una petición con cabecera `Origin`, se normaliza y compara contra la lista explícita de `CORS_ORIGINS`.
   * Si coincide, el API Gateway devuelve `Access-Control-Allow-Origin: <Origin>` y `Access-Control-Allow-Credentials: true`.
2. **Rechazo de Orígenes No Autorizados:**
   * Si el valor de `Origin` no figura en `CORS_ORIGINS`, el callback de CORS devuelve `false`, omitiendo la cabecera `Access-Control-Allow-Origin`. El navegador bloquea la lectura de la respuesta conforme al estándar CORS.
3. **Soporte para Clientes Nativos (Android) y Herramientas CLI:**
   * Las solicitudes que no incluyen cabecera `Origin` (como la aplicación móvil Android con Retrofit/OkHttp, `curl` o probes de salud de Kubernetes) son aceptadas sin restricción de origen Web.
4. **Solicitudes Preflight (`OPTIONS`) y Encabezados:**
   * Métodos habilitados: `GET, HEAD, PUT, PATCH, POST, DELETE, OPTIONS`.
   * Encabezados permitidos: `Content-Type, Authorization, Accept, Origin, X-Requested-With`.
   * Código de respuesta preflight: `204 No Content`.

---

## 4. Configuración del Ingress en Kubernetes UCT (`proxy-body-size`)

En [`infrastructure/k8s/services/api-gateway-ingress.yaml`](../infrastructure/k8s/services/api-gateway-ingress.yaml) se incorporó la anotación:

```yaml
nginx.ingress.kubernetes.io/proxy-body-size: "12m"
```

* **Motivo técnico:** Por defecto, el controlador Ingress Nginx rechaza peticiones con cuerpos superiores a `1m` devolviendo `413 Request Entity Too Large`. Dado que el límite funcional de las fotografías de evidencia en Sprint 2 es de **10 MB**, el límite de `12m` en el Ingress garantiza un margen seguro para las cabeceras y metadatos `multipart/form-data`.

---

## 5. Cómo Aplicar Cambios de Configuración

### 5.1 En Desarrollo Local
1. Copiar las plantillas sin secretos si aún no existen:
   ```bash
   cp web/backend/.env.example web/backend/.env
   cp web/frontend/.env.example web/frontend/.env
   ```
2. Editar `CORS_ORIGINS`, `AUTH_SERVICE_URL`, `INCIDENTES_SERVICE_URL`, `NOTIFICACIONES_SERVICE_URL` o `VITE_API_URL` en los archivos `.env` locales.
3. Reiniciar el proceso de `api-gateway` (`npm run start:dev api-gateway`) o el servidor de Vite (`npm run dev`).

### 5.2 En Kubernetes UCT (`student-daalvarez`)
1. **Cambios en `CORS_ORIGINS` o `*_SERVICE_URL` (Backend):**
   * Editar [`infrastructure/k8s/config/backend-config.yaml`](../infrastructure/k8s/config/backend-config.yaml).
   * Aplicar el `ConfigMap` y reiniciar el `Deployment` del API Gateway para que recargue las variables de entorno:
     ```bash
     kubectl apply -f infrastructure/k8s/config/backend-config.yaml -n student-daalvarez
     kubectl rollout restart deployment/api-gateway -n student-daalvarez
     kubectl rollout status deployment/api-gateway -n student-daalvarez --timeout=180s
     ```
2. **Cambios en anotaciones del Ingress (`api-gateway-ingress.yaml`):**
   * Editar [`infrastructure/k8s/services/api-gateway-ingress.yaml`](../infrastructure/k8s/services/api-gateway-ingress.yaml) y aplicar:
     ```bash
     kubectl apply -f infrastructure/k8s/services/api-gateway-ingress.yaml -n student-daalvarez
     ```
3. **Cambios en `VITE_API_URL` (Frontend Web):**
   * Dado que Vite incrusta las variables `VITE_*` durante `npm run build`, cualquier cambio en la URL pública requiere reconstruir la imagen Docker de la Web pasando `--build-arg VITE_API_URL=<nueva-url>`, publicarla en GHCR y ejecutar `kubectl rollout restart deployment/web-frontend -n student-daalvarez` (según lo especificado en `#128` y `#129`).
