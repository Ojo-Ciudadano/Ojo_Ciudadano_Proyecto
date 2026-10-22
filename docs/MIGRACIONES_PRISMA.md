# Migraciones Prisma — Ojo Ciudadano

**Proyecto:** Ojo Ciudadano · **Ámbito:** Backend (`web/backend`) · **Issue relacionada:** #80 (modelo persistente de evidencias)

Cada microservicio del monorepo mantiene su propio esquema Prisma y su propia base de datos PostgreSQL (*Database-per-Service*). Las migraciones se ejecutan de forma manual: **el pipeline CI/CD no aplica migraciones automáticamente** (`.github/workflows/backend-ci.yml` solo construye la imagen y actualiza el deployment).

---

## 1. Comandos base

Todos los comandos se ejecutan desde la carpeta de la app dueña del esquema, porque `prisma.config.ts` resuelve el esquema y las migraciones de forma relativa a esa carpeta.

```bash
cd web/backend/apps/incidentes   # o apps/auth-usuarios, apps/notificaciones
```

| Comando | Propósito |
| :--- | :--- |
| `npx prisma validate` | Valida el `schema.prisma` sin tocar la base de datos. |
| `npx prisma generate` | Regenera el cliente Prisma en `src/generated/prisma` (carpeta ignorada por Git). |
| `npx prisma migrate dev --name <nombre>` | Crea y aplica una migración en **desarrollo** (requiere base de datos local). |
| `npx prisma migrate deploy` | Aplica las migraciones pendientes **sin regenerar el cliente**. Es el comando para los entornos remotos. |
| `npx prisma migrate status` | Muestra el historial aplicado y las migraciones pendientes. |
| `npx prisma migrate diff --from-config-datasource --to-schema ./prisma/schema.prisma --script` | Verifica que la base de datos y el esquema no diverjan (drift). |

### Variable de entorno requerida

`prisma.config.ts` lee la URL de conexión desde el `.env` de la raíz del backend (`web/backend/.env`) mediante la variable específica de cada microservicio:

| Microservicio | Variable | Base de datos local |
| :--- | :--- | :--- |
| Auth/Usuarios | `DATABASE_URL_AUTH` | `postgresql://auth_user:auth_password@localhost:5432/auth_usuarios` |
| Incidentes | `DATABASE_URL_INCIDENTES` | `postgresql://incidentes_user:incidentes_password@localhost:5433/incidentes` |
| Notificaciones | `DATABASE_URL_NOTIFICACIONES` | `postgresql://notificaciones_user:notificaciones_password@localhost:5434/notificaciones` |

```bash
cd web/backend
cp .env.example .env   # y completar los valores
```

---

## 2. Aplicación en la base de datos del clúster UCT (Kubernetes)

El servicio `postgres-incidentes-svc` es de tipo `ClusterIP`, por lo que no es alcanzable desde fuera del clúster. La vía soportada es `kubectl port-forward` contra el Service.

### 2.1 Contexto de Kubernetes

El clúster UCT es `https://k8s-estudiantes.dev.censei.cl` y los workloads del equipo viven en el namespace `student-daalvarez`. Las credenciales (`K8S_CA`, `K8S_TOKEN`) se almacenan como **secrets del repositorio** y se inyectan en el runner de GitHub Actions:

```bash
# Reutilizar el mismo cluster/usuario que el pipeline (los secretos se obtienen del repositorio)
kubectl config set-cluster uct \
  --server=https://k8s-estudiantes.dev.censei.cl \
  --certificate-authority=ca.crt
kubectl config set-credentials <usuario> --token=<K8S_TOKEN>
kubectl config set-context uct --cluster=uct --user=<usuario> --namespace=student-daalvarez
kubectl config use-context uct
```

### 2.2 Exponer PostgreSQL de Incidentes

```bash
# Terminal 1: mantener el túnel abierto mientras se aplican las migraciones
kubectl -n student-daalvarez port-forward svc/postgres-incidentes-svc 5433:5432
```

### 2.3 Aplicar las migraciones

```bash
# Terminal 2
cd web/backend/apps/incidentes

# La contraseña vive en el Secret del clúster (no se escribe en el repositorio)
PG_PASS=$(kubectl -n student-daalvarez get secret backend-secrets -o jsonpath="{.data.postgres-incidentes}" | base64 -d)

DATABASE_URL_INCIDENTES="postgresql://incidentes_user:${PG_PASS}@localhost:5433/incidentes" npx prisma migrate deploy

# Verificación del estado final
DATABASE_URL_INCIDENTES="postgresql://incidentes_user:${PG_PASS}@localhost:5433/incidentes" npx prisma migrate status
```

Salida esperada:

```text
2 migrations found in prisma/migrations
Applying migration `20260921171228_init`
Applying migration `20261021205043_evidencia_metadata_storage`
All migrations have been successfully applied.
```

> `migrate deploy` aplica las migraciones pendientes en orden y registra cada una en la tabla `_prisma_migrations` de la base de datos de Incidentes. No vuelve a ejecutar migraciones ya aplicadas.

### 2.4 Vía soportada: workflow manual en GitHub Actions

El service account `ci-deployer` del clúster **no tiene permisos de `pods/portforward`**, por lo que el pipeline no puede abrir el túnel desde el runner. Además, el namespace aplica la `ResourceQuota` `student-provisioner-quota` (`requests.memory=1G`, `limits.memory=2G`, ya casi agotada por los workloads del equipo) y un `LimitRange` por contenedor (mínimo 25m CPU / 64Mi de memoria, máximo 500m CPU / 512Mi de memoria), por lo que **no se pueden crear pods nuevos** para ejecutar la migración.

Por eso el job `migrate-prisma` de `.github/workflows/backend-ci.yml` aplica la migración **como `initContainer` del deployment ya desplegado** del microservicio:

- No se crea ningún pod: se parchea `deployment/<microservicio>` agregando un `initContainer` con la imagen `migrate-prisma:<sha>` (`web/backend/Dockerfile.migrate`), que ejecuta `npx prisma migrate deploy` con la contraseña del Secret `backend-secrets`.
- Los `initContainers` **no suman** al request efectivo del pod (se aplica `max(sum(containers), max(initContainers))`), por lo que la cuota del namespace no se ve afectada.
- El resultado se toma del *exit code* del `initContainer`, sin depender de la salud del contenedor de la aplicación.
- Al finalizar se retira el `initContainer` y el deployment queda en su estado original.

El job **solo se ejecuta de forma manual** (evento `workflow_dispatch`) y realiza las siguientes acciones:

1. Construye y publica la imagen `migrate-prisma:<sha>` en el registro del proyecto.
2. Parchea el deployment del microservicio con el `initContainer` de migración y espera su terminación.
3. Muestra el log de la migración (salida de `prisma migrate deploy`) y restaura el deployment.

Para ejecutarlo desde GitHub Actions → *Backend CI CD* → *Run workflow*:

| Input | Valores | Descripción |
| :--- | :--- | :--- |
| `microservicio` | `incidentes`, `auth-usuarios`, `notificaciones` | Base de datos destino de la migración. |
| `desplegar` | `true` / `false` | Si es `true`, además reconstruye y despliega las imágenes (por defecto `false`). |

El pipeline por `push` a `integration` conserva su comportamiento original (construcción de imagen y despliegue); las migraciones nunca se aplican de forma automática.

> Si en el futuro la plataforma libera cuota o agrega `pods/portforward` al Role `ci-deployer` (`infrastructure/k8s/ci-deployer.yaml`), el túnel local de las secciones 2.2 y 2.3 vuelve a ser una alternativa válida para aplicar migraciones a mano.

---

## 3. Modelo de evidencias (Issue #80)

El microservicio de Incidentes persiste la **metadata** de las evidencias fotográficas; el archivo binario vive en Supabase Storage (bucket dedicado). La tabla `evidencia` se relaciona con `incidente` mediante clave foránea `evidencia_incidente_id_fkey`.

| Campo Prisma | Columna | Tipo | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | `id` | `uuid` | Identificador de la evidencia (`uuid()` por defecto). |
| `incidenteId` | `incidente_id` | `uuid` | Incidente al que pertenece la evidencia (FK → `incidente.id`). |
| `claveArchivo` | `clave_archivo` | `text` | Clave/ruta del objeto almacenado en Supabase Storage (por ejemplo `incidentes/<id>/foto.jpg`). Permite recuperar el objeto sin guardar URLs absolutas. |
| `tipoMime` | `tipo_mime` | `text` | Tipo MIME del archivo (`image/jpeg`, `image/png`, ...). |
| `tamanoArchivo` | `tamano_archivo` | `integer` | Tamaño del archivo en bytes. |
| `fechaCreacion` | `fecha_creacion` | `timestamp(3)` | Fecha de creación del registro (`now()` por defecto). |

Notas de diseño:

* Se agrega el índice `evidencia_incidente_id_idx` sobre `incidente_id` para las consultas de evidencias por incidente.
* La clave foránea usa `ON DELETE RESTRICT`, en línea con el resto de relaciones del microservicio: no se puede eliminar un incidente que tiene evidencias asociadas.
* La migración `20261021205043_evidencia_metadata_storage` **renombra** las columnas preexistentes (`url_archivo` → `clave_archivo`, `tipo` → `tipo_mime`, `fecha_subida` → `fecha_creacion`) en lugar de eliminarlas y recrearlas, de modo que la metadata ya persistida no se pierde.
* No se almacena binarios en PostgreSQL ni se versionan credenciales, URLs de Supabase o nombres de bucket dentro del esquema.

---

## 4. Comandos de verificación

```bash
# Estado de las migraciones
cd web/backend/apps/incidentes && npx prisma migrate status

# Detección de drift entre la base de datos y el esquema (salida esperada: "This is an empty migration.")
npx prisma migrate diff --from-config-datasource --to-schema ./prisma/schema.prisma --script --exit-code

# Regeneración del cliente Prisma
npx prisma generate
```
