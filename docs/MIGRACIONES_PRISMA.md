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

---

## 5. Seed de ciudadanos demo (Issue #151)

El seed de Auth/Usuarios vive en `web/backend/apps/auth-usuarios/prisma/seed.ts` y está registrado en `prisma.config.ts`. Además del administrador y de los roles `ADMIN`/`CIUDADANO`, crea un conjunto acotado de ciudadanos ficticios con **UUID fijos y reproducibles**.

### 5.1 Comando de ejecución local

```bash
cd web/backend/apps/auth-usuarios
npx prisma db seed
```

El comando carga `web/backend/.env` (vía `prisma.config.ts`) y requiere `DATABASE_URL_AUTH`.

### 5.2 Usuarios demo

| Nombre | Correo | UUID fijo |
| :--- | :--- | :--- |
| Ciudadano Demo Uno | `ciudadano.demo1@ojociudadano.cl` | `a1b2c3d4-0001-4000-8000-000000000001` |
| Ciudadana Demo Dos | `ciudadano.demo2@ojociudadano.cl` | `a1b2c3d4-0002-4000-8000-000000000002` |
| Ciudadano Demo Tres | `ciudadano.demo3@ojociudadano.cl` | `a1b2c3d4-0003-4000-8000-000000000003` |

Todos reciben el rol `CIUDADANO` mediante `UsuarioRol`. Los hashes se generan con `bcryptjs` y salt `10`, el mismo algoritmo usado por `auth.service.ts`.

### 5.3 Contraseña de los usuarios demo

La contraseña compartida se lee de la variable `DEMO_CITIZEN_PASSWORD` (definida en `web/backend/.env`):

```bash
# web/backend/.env
DEMO_CITIZEN_PASSWORD=<contraseña-demo>
```

Si la variable no está definida o está vacía, el seed usa el fallback exclusivo de desarrollo local `CiudadanoDemo123!` y emite una advertencia. En entornos compartidos o remotos se debe definir un valor propio; no se versionan credenciales reales.

### 5.4 Idempotencia

El seed se puede ejecutar cualquier cantidad de veces:

* Crea o actualiza los ciudadanos demo por su UUID fijo.
* Reutiliza el hash existente cuando la contraseña configurada ya es válida, evitando escrituras innecesarias.
* Asegura la relación `UsuarioRol` con `upsert`.
* Si un correo demo ya existe asociado a **otro** UUID, no lo modifica: lo informa por consola y continúa. Así nunca sobrescribe usuarios ajenos al conjunto demo.

Una segunda ejecución debe reportar `sin-cambios` para los tres ciudadanos y no debe aumentar el número de filas.

### 5.5 Poblar la base remota en UCT

El Service `postgres-auth-svc` es `ClusterIP`; para alcanzarlo desde fuera del clúster se abre un túnel:

```bash
# Terminal 1: mantener el túnel abierto
kubectl port-forward svc/postgres-auth-svc 5432:5432 -n student-daalvarez
```

```bash
# Terminal 2: ejecutar el seed contra la base remota
PG_PASS=$(kubectl -n student-daalvarez get secret backend-secrets -o jsonpath="{.data.postgres-auth}" | base64 -d)

cd web/backend/apps/auth-usuarios
DATABASE_URL_AUTH="postgresql://auth_user:${PG_PASS}@localhost:5432/auth_usuarios?schema=public" \
DEMO_CITIZEN_PASSWORD="<contraseña-demo>" \
npx prisma db seed
```

> Si el puerto `5432` local ya está ocupado por otra instancia de PostgreSQL, se puede mapear otro puerto (por ejemplo `55432:5432`) y ajustar la URL a `localhost:55432`. Las migraciones pendientes se aplican antes del seed con `npx prisma migrate deploy` usando la misma `DATABASE_URL_AUTH`.

---

## 6. Seed de incidentes demo (Issue #152)

El seed de Incidentes vive en `web/backend/apps/incidentes/prisma/seed.ts` y está registrado en `web/backend/apps/incidentes/prisma.config.ts` (`seed: "npx tsx prisma/seed.ts"`). Crea un conjunto acotado de incidentes georreferenciados en La Araucanía usando los ciudadanos demo de la Issue #151.

> **Importante (entorno remoto/UCE/UCT):** La ejecución del seed contra el entorno remoto de producción/Kubernetes (UCT) **DEBE realizarse desde la máquina local** abriendo túneles mediante `kubectl port-forward` (`postgres-incidentes-svc` y `postgres-auth-svc`), debido a la **ausencia intencional de herramientas de desarrollo** (`prisma`, `node`, `ts-node`) dentro del contenedor de producción. **No** se ejecuta desde dentro del contenedor de la aplicación.

### 6.1 Requisitos previos

1. La base de Incidentes debe tener las migraciones aplicadas.
2. Los **catálogos son SOLO LECTURA**: deben existir previamente los **5 estados canónicos** (`Nuevo`, `En revisión`, `En atención`, `Resuelto`, `Cerrado`) y las **8 categorías requeridas** con su `prioridadBase` válido (1-5). Si faltan, el seed **abortará inmediatamente con código de salida 1** y **sin escribir ni alterar ningún registro** en la base de incidentes.
3. La base de Auth/Usuarios debe tener el seed de #151 ejecutado: los incidentes demo referencian los UUID fijos de `ciudadano.demo1`, `ciudadano.demo2` y `ciudadano.demo3`.
4. `web/backend/.env` debe definir `DATABASE_URL_INCIDENTES` y `DATABASE_URL_AUTH`. El seed valida los ciudadanos en Auth/Usuarios antes de cualquier escritura en Incidentes; si no existen o no tienen el rol `CIUDADANO`, aborta con un mensaje claro y no modifica la base de Incidentes.

### 6.2 Comando de ejecución local

```bash
cd web/backend/apps/incidentes
npx prisma db seed
```

Salida esperada en la primera ejecución (catálogos ya existentes):

```text
Seed de incidentes demo (#152) completado
  Estados vigentes (solo lectura): 5
  Categorías vigentes (solo lectura): 8
  Ubicaciones: creados=10, actualizados=0, sin cambios=0
  Incidentes:  creados=10, actualizados=0, sin cambios=0
  Ciudadanos demo validados en auth-usuarios: 3
```

En una segunda ejecución los contadores deben quedar en `sin cambios` para ubicaciones e incidentes, sin aumentar el número de filas. Los catálogos **no se crean ni modifican**.

### 6.3 Incidentes demo

| Comuna | Sector | Categoría | Estado | Prioridad |
| :--- | :--- | :--- | :--- | :---: |
| Temuco | Centro | Alumbrado público | En revisión | 2 |
| Temuco | Pedro de Valdivia | Baches y vialidad | En atención | 2 |
| Padre Las Casas | Centro | Aseo y basura | Nuevo | 1 |
| Temuco | Santa Rosa | Agua y alcantarillado | En atención | 3 |
| Padre Las Casas | San Antonio | Áreas verdes y arbolado | Resuelto | 1 |
| Temuco | Amanecer | Seguridad ciudadana | Cerrado | 4 |
| Villarrica | Centro | Accidente de tránsito | En revisión | 5 |
| Angol | El Vergel | Incendio | Resuelto | 5 |
| Nueva Imperial | Centro | Aseo y basura | Nuevo | 1 |
| Temuco | Labranza | Alumbrado público | Cerrado | 2 |

La prioridad inicial se deriva siempre de `categoria.prioridadBase`, igual que en `IncidentesService.create`, y debe estar entre 1 y 5.

### 6.4 Idempotencia y validaciones

* **Catálogos SOLO LECTURA:** No se crean ni modifican estados ni categorías. Se consultan previamente en la base de datos; deben existir los 5 estados canónicos (`Nuevo`, `En revisión`, `En atención`, `Resuelto`, `Cerrado`) y las 8 categorías requeridas con `prioridadBase` válido (1-5). Si falta alguno, el seed **aborta inmediatamente con exit code 1** sin escribir ni alterar ningún registro.
* Las 10 ubicaciones y los 10 incidentes usan UUID fijos (`d1520000-...` y `e1520000-...`). Antes de escribir se comparan campo a campo; solo se crea lo ausente y solo se actualiza lo que difiere (garantizando 0 escrituras/duplicados en ejecuciones sucesivas).
* Las definiciones locales se validan antes de conectar: UUID, estados/categorías requeridos, **coordenadas geográficamente reales y coherentes dentro de La Araucanía** (comunas válidas: Temuco, Padre Las Casas, Villarrica, Angol, Nueva Imperial), referencias a ubicaciones/ciudadanos demo.
* **Prioridad derivada:** `nivelPrioridad` se calcula leyendo directamente el `prioridadBase` de la categoría vigente existente en la BD (igual que `IncidentesService.create`).
* Los ciudadanos demo se validan en la base de Auth/Usuarios (existencia y rol `CIUDADANO`) antes de cualquier escritura en Incidentes.
* Toda la escritura de ubicaciones e incidentes ocurre en una sola transacción: si una validación o escritura falla, no queda un seed parcial.

### 6.5 Poblar la base remota en UCT

> **Obligatorio:** La ejecución del seed contra el entorno remoto de producción/Kubernetes (UCT) **DEBE realizarse desde la máquina local**, abriendo túneles mediante `kubectl port-forward` (`postgres-incidentes-svc` y `postgres-auth-svc`), debido a la **ausencia intencional de herramientas de desarrollo** (`prisma`, `node`, `ts-node`) dentro del contenedor de producción.

El seed necesita alcanzar **dos** Services `ClusterIP`: `postgres-incidentes-svc` (escritura) y `postgres-auth-svc` (validación de ciudadanos). Mantén dos túneles abiertos:

```bash
# Terminal 1: base de Incidentes
kubectl -n student-daalvarez port-forward svc/postgres-incidentes-svc 5433:5432

# Terminal 2: base de Auth/Usuarios
kubectl -n student-daalvarez port-forward svc/postgres-auth-svc 5432:5432
```

```bash
# Terminal 3: ejecutar el seed contra las bases remotas
PG_PASS_INC=$(kubectl -n student-daalvarez get secret backend-secrets -o jsonpath="{.data.postgres-incidentes}" | base64 -d)
PG_PASS_AUTH=$(kubectl -n student-daalvarez get secret backend-secrets -o jsonpath="{.data.postgres-auth}" | base64 -d)

cd web/backend/apps/incidentes
DATABASE_URL_INCIDENTES="postgresql://incidentes_user:${PG_PASS_INC}@localhost:5433/incidentes" \
DATABASE_URL_AUTH="postgresql://auth_user:${PG_PASS_AUTH}@localhost:5432/auth_usuarios" \
npx prisma db seed
```

> Si el puerto local `5432` o `5433` ya está ocupado, mapea otros puertos (por ejemplo `55432:5432` y `55433:5432`) y ajusta las URLs. Las migraciones pendientes se aplican antes del seed con `npx prisma migrate deploy` usando la misma `DATABASE_URL_INCIDENTES`. El seed de Auth/Usuarios (#151) debe haberse ejecutado previamente contra `postgres-auth-svc`.
