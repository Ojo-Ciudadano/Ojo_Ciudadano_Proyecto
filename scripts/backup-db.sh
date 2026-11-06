#!/usr/bin/env bash
# Backup de las 3 bases PostgreSQL de Ojo Ciudadano (Auth/Usuarios, Incidentes, Notificaciones).
# - Usa pg_dump (compatible con PostgreSQL 16 del proyecto).
# - Lee conexiones/credenciales SOLO desde variables de entorno (sin secretos fijos).
# - Genera archivos independientes por servicio y fecha/hora.
# - Detecta fallos por base y aborta limpiamente si algún respaldo no se completa.
#
# Uso (ejemplo local con docker-compose desde web/backend):
#   export BACKUP_DIR=./backups
#   export AUTH_DB_HOST=127.0.0.1 AUTH_DB_PORT=5432 AUTH_DB_NAME=auth_usuarios AUTH_DB_USER=auth_user AUTH_DB_PASSWORD=auth_password
#   export INCIDENTES_DB_HOST=127.0.0.1 INCIDENTES_DB_PORT=5433 INCIDENTES_DB_NAME=incidentes INCIDENTES_DB_USER=incidentes_user INCIDENTES_DB_PASSWORD=incidentes_password
#   export NOTIFICACIONES_DB_HOST=127.0.0.1 NOTIFICACIONES_DB_PORT=5434 NOTIFICACIONES_DB_NAME=notificaciones NOTIFICACIONES_DB_USER=notificaciones_user NOTIFICACIONES_DB_PASSWORD=notificaciones_password
#   bash scripts/backup-db.sh

set -Eeuo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
TIMESTAMP="$(date -u +%Y%m%d_%H%M%S)"

log()  { printf '[backup-db] %s\n' "$*"; }
fail() { printf '[backup-db][ERROR] %s\n' "$*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Comando requerido no encontrado: $1"
}

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    fail "Variable de entorno requerida no definida: $name"
  fi
}

backup_one() {
  local service="$1" host_var="$2" port_var="$3" db_var="$4" user_var="$5" pass_var="$6"
  require_var "$host_var"
  require_var "$port_var"
  require_var "$db_var"
  require_var "$user_var"
  require_var "$pass_var"

  local host="${!host_var}" port="${!port_var}" db="${!db_var}" user="${!user_var}" pass="${!pass_var}"
  local file="${BACKUP_DIR}/backup_${service}_${TIMESTAMP}.sql.gz"
  local tmp="${file}.part"

  log "Iniciando respaldo: servicio=${service} db=${db} host=${host}:${port} -> ${file}"
  rm -f "$tmp"

  # pg_dump en formato SQL plano, comprimido. --no-owner/--no-acl facilita restore entre entornos.
  # ON_ERROR_STOP se aplica en restore (psql); aquí abortamos si pg_dump/gzip falla.
  if ! PGPASSWORD="$pass" pg_dump \
      --host="$host" \
      --port="$port" \
      --username="$user" \
      --dbname="$db" \
      --format=plain \
      --no-owner \
      --no-acl \
      --clean \
      --if-exists \
      --verbose 2>"${tmp}.log" | gzip -c > "$tmp"; then
    rm -f "$tmp"
    [[ -f "${tmp}.log" ]] && cat "${tmp}.log" >&2 || true
    rm -f "${tmp}.log"
    fail "Respaldo FALLÓ para ${service} (${db}). No se generó archivo final; revise conectividad/credenciales."
  fi

  # Verificar que el archivo comprimido no esté vacío/corrupto de forma obvia.
  if [[ ! -s "$tmp" ]] || ! gzip -t "$tmp" >/dev/null 2>&1; then
    rm -f "$tmp" "${tmp}.log"
    fail "Respaldo de ${service} (${db}) produjo un archivo inválido o vacío. Abortando limpiamente."
  fi

  mv "$tmp" "$file"
  rm -f "${tmp}.log"
  log "Respaldo OK: ${file} ($(wc -c < "$file" | tr -d ' ') bytes comprimidos)"
}

main() {
  require_cmd pg_dump
  require_cmd gzip
  require_cmd date

  mkdir -p "$BACKUP_DIR"
  log "Directorio de respaldos: ${BACKUP_DIR}"
  log "Timestamp UTC: ${TIMESTAMP}"

  # Orden fijo y explícito: si cualquiera falla, se aborta (set -e + fail).
  backup_one "auth-usuarios"    AUTH_DB_HOST AUTH_DB_PORT AUTH_DB_NAME AUTH_DB_USER AUTH_DB_PASSWORD
  backup_one "incidentes"        INCIDENTES_DB_HOST INCIDENTES_DB_PORT INCIDENTES_DB_NAME INCIDENTES_DB_USER INCIDENTES_DB_PASSWORD
  backup_one "notificaciones"    NOTIFICACIONES_DB_HOST NOTIFICACIONES_DB_PORT NOTIFICACIONES_DB_NAME NOTIFICACIONES_DB_USER NOTIFICACIONES_DB_PASSWORD

  log "Todos los respaldos completados exitosamente."
  log "Archivos generados:"
  ls -lh "${BACKUP_DIR}/backup_auth-usuarios_${TIMESTAMP}.sql.gz" \
         "${BACKUP_DIR}/backup_incidentes_${TIMESTAMP}.sql.gz" \
         "${BACKUP_DIR}/backup_notificaciones_${TIMESTAMP}.sql.gz"
}

main "$@"
