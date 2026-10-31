# Despliegue de la Web en Kubernetes UCT (GHCR)

**Issue:** #129 (CRI-07) — Crear la imagen y desplegar la Web en Kubernetes UCT  
**Estrategia de referencia:** [ESTRATEGIA_DESPLIEGUE_WEB_K8S.md](ESTRATEGIA_DESPLIEGUE_WEB_K8S.md) (#128)  
**Namespace:** `student-daalvarez`  
**Host institucional:** `ojociudadano.dev.censei.cl`

---

## 1. Arquitectura del despliegue

La Web se publica como una imagen multi-etapa: React se compila con Vite y el
resultado estático se sirve con Nginx. El Ingress usa **Single-Host Path
Routing** para que la Web y el API Gateway compartan el mismo origen:

| Ruta | Backend | Puerto |
| :--- | :--- | :---: |
| `/api` | `api-gateway-svc` | `3000` |
| `/` | `web-frontend-svc` | `80` |

Esto evita CORS, permite reutilizar el certificado TLS autofirmado del Ingress y
habilita el contexto seguro (`https`) que exige la geolocalización del navegador.

### Archivos involucrados

| Archivo | Función |
| :--- | :--- |
| `web/frontend/Dockerfile` | Build multi-etapa: `node:20-alpine` → `nginx:stable-alpine`. |
| `web/frontend/nginx.conf` | SPA fallback (`try_files $uri $uri/ /index.html`), gzip, caché de `/assets` y `/healthz`. |
| `web/frontend/.dockerignore` | Excluye `node_modules`, `dist` y archivos `.env` del contexto de build. |
| `infrastructure/k8s/config/web-frontend-config.yaml` | ConfigMap `web-frontend-config` con `VITE_API_URL`. |
| `infrastructure/k8s/services/web-frontend.yaml` | Deployment `web-frontend` + Service `web-frontend-svc`. |
| `infrastructure/k8s/services/api-gateway-ingress.yaml` | Ingress Single-Host (`/api` y `/`). |

> **Importante:** `VITE_API_URL` se incrusta en el bundle **en tiempo de
> compilación**. El ConfigMap mantiene la configuración declarativa en el
> clúster, pero cambiar su valor no altera una imagen ya construida: hay que
> reconstruir y publicar la imagen con el nuevo `--build-arg`.

---

## 2. Requisitos previos

- Docker 20+ con acceso a `ghcr.io`.
- Un Personal Access Token (PAT) de GitHub con permisos `read:packages` y
  `write:packages` (y `delete:packages` si se requiere limpiar versiones).
- `kubectl` configurado contra el clúster UCT y permisos en el namespace
  `student-daalvarez`.
- Certificado TLS `uct-ingress-tls` ya aprovisionado (ver comentarios en
  `api-gateway-ingress.yaml`).

Variables usadas en los ejemplos:

```bash
export REGISTRY=ghcr.io
export REPO=ojo-ciudadano/ojo_ciudadano_proyecto
export IMAGE=${REGISTRY}/${REPO}/web
export NAMESPACE=student-daalvarez
export VITE_API_URL=https://ojociudadano.dev.censei.cl
export GIT_SHA=$(git rev-parse --short HEAD)
```

---

## 3. Construir y publicar la imagen en GHCR

### 3.1 Autenticarse en GHCR

```bash
echo "$CR_PAT" | docker login ghcr.io -u <usuario-github> --password-stdin
```

### 3.2 Construir la imagen

Se etiqueta con una versión legible (`v1.0.0`), con el SHA corto del commit
(trazabilidad y rollback determinista) y con `latest`.

```bash
docker build \
  --build-arg VITE_API_URL="${VITE_API_URL}" \
  -t "${IMAGE}:v1.0.0" \
  -t "${IMAGE}:${GIT_SHA}" \
  -t "${IMAGE}:latest" \
  ./web/frontend
```

### 3.3 Publicar en GHCR

```bash
docker push "${IMAGE}:v1.0.0"
docker push "${IMAGE}:${GIT_SHA}"
docker push "${IMAGE}:latest"
```

### 3.4 Hacer público el paquete (obligatorio tras la primera publicación)

La primera vez que se publica un paquete en GHCR queda como **Private**. Los
nodos del clúster UCT no tienen credenciales de pull, por lo que un paquete
privado provoca `ImagePullBackOff` con `401 Unauthorized`.

1. Ir a GitHub → organización **Ojo-Ciudadano** → pestaña **Packages**.
2. Abrir el paquete **`web`**.
3. **Package settings** → sección **Danger Zone** → **Change visibility**.
4. Seleccionar **Public** y confirmar escribiendo el nombre del paquete.

Verificación rápida de que el paquete es público:

```bash
docker logout ghcr.io
docker pull "${IMAGE}:v1.0.0"
```

Si la descarga funciona sin autenticación, el clúster podrá obtener la imagen.

---

## 4. Aplicar los manifiestos

```bash
kubectl apply -f infrastructure/k8s/config/web-frontend-config.yaml
kubectl apply -f infrastructure/k8s/services/web-frontend.yaml
kubectl apply -f infrastructure/k8s/services/api-gateway-ingress.yaml
```

Esperar a que el rollout termine:

```bash
kubectl rollout status deployment/web-frontend -n "${NAMESPACE}" --timeout=180s
```

Verificar los recursos creados:

```bash
kubectl get deployment,service,ingress,pods -n "${NAMESPACE}" -l app=web-frontend
kubectl get ingress api-gateway-ingress -n "${NAMESPACE}"
```

### Desplegar una versión distinta a la fijada en el manifiesto

El `deployment.yaml` referencia `web:v1.0.0`. Para publicar otra versión sin
editar el archivo:

```bash
kubectl set image deployment/web-frontend \
  web-frontend="${IMAGE}:${GIT_SHA}" \
  -n "${NAMESPACE}"

kubectl rollout status deployment/web-frontend -n "${NAMESPACE}" --timeout=180s
```

---

## 5. Verificación del despliegue

### 5.1 Estado del pod y eventos

```bash
kubectl get pods -n "${NAMESPACE}" -l app=web-frontend
kubectl describe pod -n "${NAMESPACE}" -l app=web-frontend
kubectl logs -n "${NAMESPACE}" -l app=web-frontend --tail=50
```

### 5.2 Acceso HTTP

```bash
# Raíz de la SPA
curl -I -k https://ojociudadano.dev.censei.cl/

# Ruta de React Router accedida directamente: debe responder 200 con index.html
curl -I -k https://ojociudadano.dev.censei.cl/login

# API Gateway bajo el mismo host
curl -k https://ojociudadano.dev.censei.cl/api/v1
```

Resultados esperados:

- `/` y `/login` → `HTTP/2 200` con `Content-Type: text/html`.
- `/api/v1` → `Hello World!` (o el mensaje de bienvenida del gateway).
- `/assets/*.js` y `/assets/*.css` → `200` con `Cache-Control: public, immutable`.

### 5.3 Verificación en navegador

1. Abrir `https://ojociudadano.dev.censei.cl` y aceptar el certificado
   autofirmado (`uct-ingress-tls`).
2. Navegar a `/login` y recargar con `F5`: no debe aparecer un 404 de Nginx.
3. Iniciar sesión con un usuario válido: la petición debe ir a
   `https://ojociudadano.dev.censei.cl/api/v1/auth/login` sin errores CORS.
4. En DevTools → Network, confirmar que los activos se cargan desde `/assets/`
   y que las llamadas al API usan el mismo host.

---

## 6. Rollback a una versión anterior

### 6.1 Revertir a la revisión previa del Deployment

```bash
kubectl rollout history deployment/web-frontend -n "${NAMESPACE}"
kubectl rollout undo deployment/web-frontend -n "${NAMESPACE}"
kubectl rollout status deployment/web-frontend -n "${NAMESPACE}" --timeout=180s
```

### 6.2 Fijar explícitamente una imagen anterior conocida

```bash
kubectl set image deployment/web-frontend \
  web-frontend="${IMAGE}:<SHA_ANTERIOR>" \
  -n "${NAMESPACE}"

kubectl rollout status deployment/web-frontend -n "${NAMESPACE}" --timeout=180s
```

### 6.3 Revertir a una revisión específica

```bash
kubectl rollout undo deployment/web-frontend \
  --to-revision=<NUMERO_DE_REVISION> \
  -n "${NAMESPACE}"
```

Tras cualquier rollback, repetir las verificaciones de la sección 5.

---

## 7. Diagnóstico de problemas frecuentes

| Síntoma | Causa probable | Solución |
| :--- | :--- | :--- |
| `ImagePullBackOff` / `401 Unauthorized` | Paquete GHCR privado | Cambiar la visibilidad de `web` a **Public** (sección 3.4). |
| `ImagePullBackOff` / `manifest unknown` | Etiqueta inexistente en GHCR | Verificar el tag publicado con `docker pull "${IMAGE}:<tag>"`. |
| `404` al recargar `/login` | Falta el SPA fallback en Nginx | Confirmar `try_files $uri $uri/ /index.html` en `nginx.conf` y reconstruir la imagen. |
| Errores CORS en el navegador | La Web apunta a otro origen | Verificar el `--build-arg VITE_API_URL` y que el Ingress enrute `/api` al gateway. |
| `Pending` por cuota | Recursos mayores a la `ResourceQuota` | Mantener `requests 25m/64Mi` y `limits 50m/128Mi`; usar `maxSurge: 0`. |
| `CrashLoopBackOff` | Configuración de Nginx inválida | Revisar `kubectl logs`; validar con `docker run --rm <imagen> nginx -t`. |
| La Web muestra datos de localhost | Imagen construida sin el `--build-arg` | Reconstruir con `VITE_API_URL=https://ojociudadano.dev.censei.cl`. |

Comandos útiles de diagnóstico:

```bash
kubectl get events -n "${NAMESPACE}" --sort-by=.lastTimestamp | tail -20
kubectl get pod -n "${NAMESPACE}" -l app=web-frontend -o jsonpath='{.items[0].status.containerStatuses[0].state}'
```

---

## 8. Seguridad

- La imagen contiene únicamente activos estáticos públicos; **no** incluye
  secretos ni archivos `.env` (excluidos por `.dockerignore`).
- Ninguna variable `VITE_*` debe contener credenciales: todo el bundle es
  visible para el navegador.
- El Deployment `web-frontend` no referencia `Secret` alguno; solo el ConfigMap
  público `web-frontend-config`.
- El Secret TLS `uct-ingress-tls` se gestiona fuera del repositorio (ver
  comentarios en `api-gateway-ingress.yaml`).

---

## 9. Referencias

- [ESTRATEGIA_DESPLIEGUE_WEB_K8S.md](ESTRATEGIA_DESPLIEGUE_WEB_K8S.md) — estrategia de #128.
- [CONFIGURACION_ENTORNOS_CORS.md](CONFIGURACION_ENTORNOS_CORS.md) — variables por entorno (#127).
- Issue #78 — infraestructura backend en Kubernetes UCT.
- Issue #129 — construcción de la imagen y despliegue Web.
