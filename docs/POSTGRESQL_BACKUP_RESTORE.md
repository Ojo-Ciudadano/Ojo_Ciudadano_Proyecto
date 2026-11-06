# Backup y restore de los tres PostgreSQL — Ojo Ciudadano

**Issue:** #154 · **Ámbito:** Bases de datos PostgreSQL (Auth/Usuarios, Incidentes, Notificaciones) · **Namespace UCT:** `student-daalvarez`

Este documento describe cómo **respaldar** y **restaurar** las tres bases de datos PostgreSQL del proyecto. Los respaldos se generan con `pg_dump` (formato SQL plano, comprimido con gzip), **compatibles con PostgreSQL 16** (`postgres:16-alpine`), y se automatizan en Kubernetes mediante un `CronJob`.

> **Alcance importante (Supabase Storage):** Las **imágenes/fotografías y archivos almacenados en Supabase Storage NO forman parte** de este respaldo de base de datos. Este procedimiento respalda únicamente los datos persistidos en PostgreSQL (incluida la metadata que referencie evidencias, si existe en BD). La recuperación de objetos de Storage debe gestionarse por separado según las políticas/herramientas de Supabase.

---

## 1. Inventario de bases de datos

| Servicio | Base (DB) | Usuario | Service K8s (ClusterIP) | Puerto | Secret (key en `backend-secrets`) |
|---|---|---|---:|---:|---|
| Auth/Usuarios | `auth_usuarios` | `auth_user` | `postgres-auth-svc` | 5432 | `postgres-auth` |
| Incidentes | `incidentes` | `incidentes_user` | `postgres-incidentes-svc` | 5432 | `postgres-incidentes` |
| Notificaciones | `notificaciones` | `notificaciones_user` | `postgres-notificaciones-svc` | 5432 | `postgres-notificaciones` |

Hosts/puertos/nombres no sensibles en K8s: `ConfigMap/backend-config` (`DB_HOST_*`, `DB_PORT_*`, `DB_NAME_*`). Credenciales sensibles: `Secret/backend-secrets`.

---

## 2. Artefactos del proyecto

| Artefacto | Ruta | Descripción |
|---|---|---|
| Script local | `scripts/backup-db.sh` | Respaldo manual/automatizable fuera de K8s (lee conexiones desde variables de entorno, sin secretos fijos). |
| Manifiestos K8s | `infrastructure/k8s/jobs/postgres-backup.yaml` | `PVC` (`postgres-backups-pvc`), `ConfigMap` con script, `CronJob` (`postgres-backup`) en namespace `student-daalvarez`. |

Convención de archivos generados (identificables por servicio y fecha/hora UTC):
- `backup_auth-usuarios_YYYYMMDD_HHMMSS.sql.gz`
- `backup_incidentes_YYYYMMDD_HHMMSS.sql.gz`
- `backup_notificaciones_YYYYMMDD_HHMMSS.sql.gz`

---

## 3. Prerrequisitos

### Para respaldo/restore local
- Cliente PostgreSQL 16 compatible: `pg_dump`, `psql`, `createdb`/`dropdb` (o `psql -c`). En Windows puede usarse Git Bash/WSL o instalación de PostgreSQL; en Linux/macOS el paquete `postgresql-client`.
- Acceso de red a las bases (local Docker Compose o túnel `kubectl port-forward` a los Services `ClusterIP`).
- Credenciales disponibles **solo** vía variables de entorno (nunca hardcodeadas).

### Para respaldo/restore en Kubernetes (UCT)
- `kubectl` configurado contra el clúster UCT y contexto apuntando al namespace `student-daalvarez` (o pasar `-n student-daalvarez`).
- Recursos aplicados: `ConfigMap/backend-config`, `Secret/backend-secrets` y los PostgreSQL (`infrastructure/k8s/databases/postgres-all.yaml`).
- Permisos para `get/list/create/delete` sobre `jobs`, `cronjobs`, `pods`, `pods/exec`/`pods/log`, `pvc` (el `Role` de `ci-deployer` ya contempla `jobs`; verificar permisos sobre `cronjobs/pvc` si se aplica con otro sujeto).

---

## 4. Respaldo manual

### 4.1 Local con Docker Compose (`web/backend/docker-compose.yml`)

```bash
# Desde la raíz del repositorio (ajustar shell en Windows: Git Bash/WSL)
export BACKUP_DIR=./backups
export AUTH_DB_HOST=127.0.0.1 AUTH_DB_PORT=5432 AUTH_DB_NAME=auth_usuarios AUTH_DB_USER=auth_user AUTH_DB_PASSWORD=auth_password
export INCIDENTES_DB_HOST=127.0.0.1 INCIDENTES_DB_PORT=5433 INCIDENTES_DB_NAME=incidentes INCIDENTES_DB_USER=incidentes_user INCIDENTES_DB_PASSWORD=incidentes_password
export NOTIFICACIONES_DB_HOST=127.0.0.1 NOTIFICACIONES_DB_PORT=5434 NOTIFICACIONES_DB_NAME=notificaciones NOTIFICACIONES_DB_USER=notificaciones_user NOTIFICACIONES_DB_PASSWORD=notificaciones_password

bash scripts/backup-db.sh
```

Comportamiento: respalda las 3 bases en orden; si **cualquier** respaldo falla, el script elimina el archivo parcial y **aborta con error** (no reporta éxito parcial).

### 4.2 Manual por servicio (pg_dump directo)

```bash
# Ejemplo: Incidentes (reemplazar HOST/PORT/USER/PASSWORD)
export PGPASSWORD="$INCIDENTES_DB_PASSWORD"
pg_dump -h "$INCIDENTES_DB_HOST" -p "$INCIDENTES_DB_PORT" -U incidentes_user -d incidentes \
  --format=plain --no-owner --no-acl --clean --if-exists | gzip -c > backup_incidentes_$(date -u +%Y%m%d_%H%M%S).sql.gz
unset PGPASSWORD
```

### 4.3 Respaldo manual en Kubernetes (Job ad-hoc desde el CronJob)

```bash
# Ejecutar un respaldo inmediato usando la misma plantilla del CronJob
kubectl -n student-daalvarez create job --from=cronjob/postgres-backup postgres-backup-manual-$(date +%s)

# Seguir logs
kubectl -n student-daalvarez logs -f job/postgres-backup-manual-<timestamp>
```

Los archivos quedan en el PVC `postgres-backups-pvc` (montado en `/backups`).

### 4.4 Copiar respaldos desde el PVC (para resguardo externo)

```bash
# Crear un pod auxiliar que monte el PVC
kubectl -n student-daalvarez run backup-access --rm -it --restart=Never \
  --image=postgres:16-alpine \
  --overrides='{"spec":{"containers":[{"name":"backup-access","image":"postgres:16-alpine","command":["sh"],"stdin":true,"tty":true,"volumeMounts":[{"name":"b","mountPath":"/backups"}]}],"volumes":[{"name":"b","persistentVolumeClaim":{"claimName":"postgres-backups-pvc"}}]}}'

# Dentro del pod: listar
ls -lh /backups

# En otra terminal (copiar a máquina local)
kubectl -n student-daalvarez cp backup-access:/backups/backup_auth-usuarios_YYYYMMDD_HHMMSS.sql.gz ./backup_auth-usuarios_YYYYMMDD_HHMMSS.sql.gz
```

---

## 5. Frecuencia y política de retención

| Parámetro | Valor configurado | Notas |
|---|---|---|
| Frecuencia (`CronJob`) | `0 3 * * *` (diario, 03:00 UTC) | Ajustar a ventana de bajo uso/mantenimiento. `timeZone: UTC` explícito. |
| Concurrencia | `Forbid` | Evita ejecuciones simultáneas. |
| Historial de Jobs K8s | `successfulJobsHistoryLimit: 2`, `failedJobsHistoryLimit: 2` | Limita objetos Job/Pod retenidos en el clúster. |
| Deadline/timeout | `activeDeadlineSeconds: 1800`, `startingDeadlineSeconds: 300`, `backoffLimit: 0` | Falla controlada sin reintentos automáticos infinitos. |
| Almacenamiento | PVC `postgres-backups-pvc` (5Gi, `cinder-ceph`, RWO) | **Persistente**; no depende del almacenamiento efímero del Pod. |
| Retención de archivos `.sql.gz` | **Recomendada: 14–30 días** (operativa) | No hay purga automática en el script base; implementar rotación (p. ej. Job/Cron adicional `find /backups -name 'backup_*.sql.gz' -mtime +30 -delete` o copia externa + limpieza) según capacidad del PVC y políticas institucionales. |
| Resguardo externo | Recomendado | Copiar periódicamente fuera del clúster (ver 4.4) para protección ante pérdida del PVC/clúster. |

---

## 6. Restauración — principios generales

1. **Detener escrituras** de la aplicación hacia la base objetivo (escalar a 0 el/los microservicios que la usan, o poner en mantenimiento) para evitar inconsistencias.
2. **Seleccionar el respaldo correcto** (servicio + fecha/hora). Verificar integridad: `gzip -t archivo.sql.gz`.
3. El `pg_dump` se genera con `--clean --if-exists --no-owner --no-acl` (SQL plano). La restauración se realiza con `psql`:
   ```bash
   gunzip -c backup_<servicio>_<ts>.sql.gz | PGPASSWORD="$PASS" psql -h "$HOST" -p "$PORT" -U "$USER" -d "$DB" -v ON_ERROR_STOP=1
   ```
4. **Verificación posterior** (conteos, tablas críticas, smoke de API) antes de reabrir tráfico.
5. **No restaura Supabase Storage:** tras restaurar BD, las referencias a evidencias pueden apuntar a objetos que deben existir en Storage; validar por separado.

> **Advertencia:** `--clean` emite `DROP ... IF EXISTS` para objetos del dump. Restaurar sobre una base con datos productivos los reemplaza según el contenido del respaldo. Para pruebas seguras, preferir una **base lógica temporal** (sección 8.2) o un entorno aislado.

---

## 7. Restauración paso a paso por servicio (Kubernetes)

Variables comunes (ejemplo): `NS=student-daalvarez`. Obtener password desde Secret (no imprimir en logs compartidos):
```bash
export NS=student-daalvarez
# Ejemplo para extraer (uso local seguro): kubectl -n $NS get secret backend-secrets -o jsonpath='{.data.postgres-incidentes}' | base64 -d
```

### 7.1 Auth/Usuarios (`auth_usuarios`, `auth_user`, `postgres-auth-svc`)

**Prerrequisitos:** respaldo `backup_auth-usuarios_*.sql.gz` verificado (`gzip -t`); acceso al Service; microservicio `auth-usuarios` escalado a 0 o en mantenimiento.

```bash
# 1) Detener escritor
kubectl -n $NS scale deployment/auth-usuarios --replicas=0

# 2) Restaurar (desde máquina con psql + port-forward O desde pod con cliente, ver 8.2)
kubectl -n $NS port-forward svc/postgres-auth-svc 15432:5432 &
PF_PID=$!
export PGPASSWORD="<postgres-auth-secret>"
gunzip -c backup_auth-usuarios_YYYYMMDD_HHMMSS.sql.gz | psql -h 127.0.0.1 -p 15432 -U auth_user -d auth_usuarios -v ON_ERROR_STOP=1
unset PGPASSWORD
kill $PF_PID
```

**Verificación posterior:**
```bash
PGPASSWORD="<secret>" psql -h 127.0.0.1 -p 15432 -U auth_user -d auth_usuarios -c "\dt"
PGPASSWORD="<secret>" psql -h 127.0.0.1 -p 15432 -U auth_user -d auth_usuarios -c "SELECT COUNT(*) FROM \"Usuario\";"
# Reabrir servicio y smoke test
kubectl -n $NS scale deployment/auth-usuarios --replicas=1
kubectl -n $NS rollout status deployment/auth-usuarios
curl -fsS https://<dominio-api>/api/v1/health  # o endpoint de auth según routing
```

### 7.2 Incidentes (`incidentes`, `incidentes_user`, `postgres-incidentes-svc`)

**Prerrequisitos:** respaldo `backup_incidentes_*.sql.gz`; `incidentes` (y `api-gateway` si conviene reducir tráfico) en mantenimiento/escalado controlado.

```bash
kubectl -n $NS scale deployment/incidentes --replicas=0
kubectl -n $NS port-forward svc/postgres-incidentes-svc 15433:5432 &
PF_PID=$!
export PGPASSWORD="<postgres-incidentes-secret>"
gunzip -c backup_incidentes_YYYYMMDD_HHMMSS.sql.gz | psql -h 127.0.0.1 -p 15433 -U incidentes_user -d incidentes -v ON_ERROR_STOP=1
unset PGPASSWORD; kill $PF_PID
```

**Verificación posterior:**
```bash
PGPASSWORD="<secret>" psql -h 127.0.0.1 -p 15433 -U incidentes_user -d incidentes -c "\dt"
PGPASSWORD="<secret>" psql -h 127.0.0.1 -p 15433 -U incidentes_user -d incidentes -c "SELECT COUNT(*) FROM \"Incidente\";"
kubectl -n $NS scale deployment/incidentes --replicas=1
kubectl -n $NS rollout status deployment/incidentes
```

### 7.3 Notificaciones (`notificaciones`, `notificaciones_user`, `postgres-notificaciones-svc`)

**Prerrequisitos:** respaldo `backup_notificaciones_*.sql.gz`; `notificaciones` en mantenimiento/escalado a 0.

```bash
kubectl -n $NS scale deployment/notificaciones --replicas=0
kubectl -n $NS port-forward svc/postgres-notificaciones-svc 15434:5432 &
PF_PID=$!
export PGPASSWORD="<postgres-notificaciones-secret>"
gunzip -c backup_notificaciones_YYYYMMDD_HHMMSS.sql.gz | psql -h 127.0.0.1 -p 15434 -U notificaciones_user -d notificaciones -v ON_ERROR_STOP=1
unset PGPASSWORD; kill $PF_PID
```

**Verificación posterior:**
```bash
PGPASSWORD="<secret>" psql -h 127.0.0.1 -p 15434 -U notificaciones_user -d notificaciones -c "\dt"
PGPASSWORD="<secret>" psql -h 127.0.0.1 -p 15434 -U notificaciones_user -d notificaciones -c "SELECT COUNT(*) FROM \"Notificacion\";" 2>/dev/null || echo "(ajustar nombre de tabla al esquema real)"
kubectl -n $NS scale deployment/notificaciones --replicas=1
kubectl -n $NS rollout status deployment/notificaciones
```

> **Nota:** Los nombres de tabla en verificaciones SQL deben coincidir con el esquema real de Prisma (citar con comillas si usan mayúsculas). Ajustar consultas de conteo según el modelo.

---

## 8. Procedimientos de restauración en entornos controlados

### 8.1 Restaurar en entorno local (Docker Compose)

Útil para validar un respaldo o recuperar datos en desarrollo.

```bash
cd web/backend
docker compose up -d   # asegura postgres-auth-usuarios/incidentes/notificaciones

# Auth (puerto local 5432 según docker-compose)
export PGPASSWORD=auth_password
gunzip -c ../../backups/backup_auth-usuarios_YYYYMMDD_HHMMSS.sql.gz | \
  psql -h 127.0.0.1 -p 5432 -U auth_user -d auth_usuarios -v ON_ERROR_STOP=1

# Incidentes (5433)
export PGPASSWORD=incidentes_password
gunzip -c ../../backups/backup_incidentes_YYYYMMDD_HHMMSS.sql.gz | \
  psql -h 127.0.0.1 -p 5433 -U incidentes_user -d incidentes -v ON_ERROR_STOP=1

# Notificaciones (5434)
export PGPASSWORD=notificaciones_password
gunzip -c ../../backups/backup_notificaciones_YYYYMMDD_HHMMSS.sql.gz | \
  psql -h 127.0.0.1 -p 5434 -U notificaciones_user -d notificaciones -v ON_ERROR_STOP=1
unset PGPASSWORD
```

**Verificación:** conectar con `psql` a cada puerto y ejecutar `\dt` + `SELECT COUNT(*)` en tablas clave; luego levantar servicios backend apuntando al `.env` local.

### 8.2 Restaurar en una base lógica temporal dentro del mismo Pod PostgreSQL existente

Permite validar el dump **sin sobreescribir** la base operativa (usa el mismo servidor/Pod, distinta base lógica). Requiere `psql`/`createdb` dentro del contenedor y acceso al archivo de respaldo (montar PVC o copiar con `kubectl cp`).

```bash
export NS=student-daalvarez
# Ejemplo con Incidentes: abrir shell en el pod postgres
kubectl -n $NS exec -it deploy/postgres-incidentes -- sh

# Dentro del pod (variables según el contenedor)
export PGPASSWORD="$POSTGRES_PASSWORD"
# Crear DB temporal
createdb -U "$POSTGRES_USER" incidentes_restore_tmp || true
# Suponiendo el respaldo copiado a /tmp/backup_incidentes_YYYYMMDD_HHMMSS.sql.gz
gunzip -c /tmp/backup_incidentes_YYYYMMDD_HHMMSS.sql.gz | \
  psql -U "$POSTGRES_USER" -d incidentes_restore_tmp -v ON_ERROR_STOP=1
# Verificar
psql -U "$POSTGRES_USER" -d incidentes_restore_tmp -c "\dt"
psql -U "$POSTGRES_USER" -d incidentes_restore_tmp -c 'SELECT COUNT(*) FROM "Incidente";'
# Limpieza (cuando termine la validación)
dropdb -U "$POSTGRES_USER" incidentes_restore_tmp
unset PGPASSWORD
```

Para acceder a respaldos del PVC `postgres-backups-pvc` desde el Pod de Postgres, puede usarse un Pod auxiliar que monte **ambos** (si la política de storage lo permite) o copiar el archivo (`kubectl cp` al pod/auxiliar). Alternativamente, ejecutar la restauración desde un Pod `postgres:16-alpine` temporal que monte `postgres-backups-pvc` y se conecte a `postgres-incidentes-svc` (requiere credencial en Secret).

---

## 9. Operación y verificación del CronJob

```bash
kubectl -n student-daalvarez apply -f infrastructure/k8s/jobs/postgres-backup.yaml
kubectl -n student-daalvarez get cronjob,pvc,configmap -l app=postgres-backup
kubectl -n student-daalvarez get jobs --sort-by=.metadata.creationTimestamp
kubectl -n student-daalvarez logs job/<nombre-job>
```

Comprobar que, ante fallo de una base, el Job termina en error y **no** deja el set completo reportado como exitoso sin todos los archivos válidos (el script aborta limpiamente).

---

## 10. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| `pg_dump: ... connection refused` | Service/host incorrecto, Pod DB no listo | Verificar `kubectl get svc,pods`, endpoints y `DB_HOST_*`. |
| `password authentication failed` | Secret desincronizado | Comparar `backend-secrets` con `POSTGRES_PASSWORD` real (rotación coordinada). |
| `permission denied` en `/backups` | UID/GID del contenedor vs PVC | Revisar `securityContext.fsGroup` y permisos del volumen. |
| Job queda activo mucho tiempo | Red lenta / DB grande / bloqueo | Revisar `activeDeadlineSeconds`, logs, locks en DB; ajustar recursos si es necesario. |
| `gzip: invalid` / archivo vacío | `pg_dump` falló parcialmente | El script aborta y borra parciales; revisar log `.part.log` en ejecución manual. |

---

## 11. Referencias

- `infrastructure/k8s/databases/postgres-all.yaml` — despliegue PostgreSQL y credenciales por servicio.
- `infrastructure/k8s/config/backend-config.yaml`, `infrastructure/k8s/config/backend-secrets.yaml` — configuración y secretos.
- `web/backend/docker-compose.yml` — entorno local (puertos 5432/5433/5434).
- `docs/MIGRACIONES_PRISMA.md` — operación de esquemas/migraciones (complementario a restore de datos).
- PostgreSQL: [`pg_dump`](https://www.postgresql.org/docs/16/app-pgdump.html), [`psql`](https://www.postgresql.org/docs/16/app-psql.html).
