# Checklist de Revisión Cruzada de Pull Requests (Sprint 2)

Este documento proporciona una guía estructurada y una plantilla reutilizable para realizar la **revisión cruzada (*cross-review*)** de Pull Requests durante el Sprint 2 del proyecto **Ojo Ciudadano**, garantizando que cada entrega cumpla con el flujo Git acordado, los límites de alcance, los criterios de aceptación, la compatibilidad de contratos y la seguridad del repositorio antes de su integración en la rama `integration`.

---

## 1. Propósito y Alcance de Aplicación

Todo Pull Request abierto por un integrante del equipo (Backend, Plataforma Web, Aplicación Móvil Android, Infraestructura Kubernetes o Documentación) debe ser revisado por al menos otro integrante antes de su fusión (`merge`).

El checklist se estructura en **dos niveles**:
1. **Verificaciones Transversales (Obligatorias para todo PR)**: Flujo Git, trazabilidad con la Issue, respeto de alcance/fuera de alcance, criterios de aceptación, evidencia de verificación y seguridad (ausencia de secretos).
2. **Verificaciones Condicionales (Según la naturaleza del cambio)**: Contratos de API (`/api/v1`) y consumidores, migraciones de base de datos (Prisma 7) y configuraciones de entorno/infraestructura. Si una sección condicional no aplica al cambio revisado (por ejemplo, en un PR exclusivamente documental o de estilos UI), se marca como `N/A` (*No Aplica*) sin bloquear la revisión.

---

## 2. Checklist de Revisión Cruzada

### 2.1. Flujo Git y Trazabilidad (Obligatorio)

- [ ] **Nomenclatura de la rama de origen (`head`)**: La rama sigue estrictamente la convención `feature/<numero-issue>-<slug>` (ej. `feature/86-evidence-api`, `feature/130-role-guards-menu`), donde `<numero-issue>` coincide con el número real de la Issue asignada.
- [ ] **Rama de destino (`base`)**: El Pull Request apunta hacia la rama **`integration`** (nunca directamente hacia `main`).
- [ ] **Vinculación automática de cierre**: La descripción del PR incluye la directiva `Closes #<numero>` con el número real de la Issue que resuelve (ej. `Closes #86`), permitiendo la actualización automática del estado en GitHub Projects.
- [ ] **Estado de fusión y commits**: La rama está actualizada o es fusionable sin conflictos contra `integration` y los mensajes de commit siguen un formato descriptivo y atómico.

---

### 2.2. Objetivo, Alcance y Fuera de Alcance (Obligatorio)

- [ ] **Alineación con el objetivo**: Los archivos creados o modificados responden directamente al **Objetivo** y al **Alcance** definidos en la descripción de la Issue.
- [ ] **Respeto estricto del "Fuera de alcance"**: El PR **no** introduce funcionalidades, refactorizaciones invasivas, endpoints adicionales, pantallas ni pruebas que la Issue declare explícitamente en su sección *Fuera de alcance* (evitando solapamientos o conflictos con otras Issues del Sprint 2).
- [ ] **Limpieza de cambios**: No se incluyen archivos temporales, carpetas de compilación (`dist/`, `build/`, `node_modules/`), ni modificaciones accidentales en archivos ajenos a la tarea.

---

### 2.3. Criterios de Aceptación y Evidencia (Obligatorio)

- [ ] **Cumplimiento de Criterios de Aceptación**: Se verificó punto por punto cada ítem listado en la sección **Criterios de aceptación** de la Issue correspondiente.
- [ ] **Evidencia de funcionamiento y pruebas**:
  - En cambios de **código (Backend / Web / Android)**: Las pruebas unitarias y/o de integración pertinentes al cambio pasan satisfactoriamente y los checks de CI/CD en GitHub Actions finalizan con éxito.
  - En cambios de **interfaz o infraestructura**: Se adjunta o describe la evidencia de compilación, capturas o validación técnica correspondiente.
  - En cambios **documentales**: Los enlaces internos son válidos, la sintaxis Markdown es correcta y el contenido cubre todos los puntos exigidos por la Issue.

---

### 2.4. Seguridad y Ausencia de Secretos (Obligatorio)

- [ ] **Sin credenciales ni secretos versionados**: El diff del PR **no** contiene archivos `.env` reales, contraseñas de PostgreSQL, tokens JWT firmados de producción, claves privadas de certificados TLS, tokens de acceso a Kubernetes (`K8S_TOKEN`) ni llaves administrativas de Supabase Storage (`SUPABASE_SERVICE_ROLE_KEY`).
- [ ] **Uso de plantillas seguras**: Si el cambio introduce nuevas variables de entorno, solo se versionan nombres o valores de ejemplo no sensibles en `.env.example` o en `ConfigMap` (`backend-config.yaml`).
- [ ] **Encapsulación en respuestas**: Los endpoints no filtran información sensible interna (como `passwordHash`, rutas privadas de buckets de almacenamiento o trazas crudas de base de datos).

---

### 2.5. Contratos de API y Compatibilidad con Consumidores *(Condicional: Aplica si se crean o modifican endpoints, DTOs o clientes HTTP)*

*Si el PR no toca endpoints, DTOs ni consumo de API, marcar esta sección como **N/A**.*

- [ ] **Continuidad del prefijo `/api/v1`**: Las rutas expuestas en el API Gateway mantienen el prefijo `/api/v1` y respetan las condiciones de evolución aditiva.
- [ ] **Retrocompatibilidad con Web y Android**:
  - No se eliminan, renombran ni cambian de tipo los campos existentes en las respuestas JSON consumidas por la plataforma Web o la app Android.
  - Las nuevas colecciones en respuestas se inicializan de forma consistente como arreglo vacío `[]` cuando no existen registros asociados.
  - Los nuevos filtros o parámetros de consulta (`@Query()`) son opcionales para no romper llamadas existentes.
- [ ] **Estándar de errores y códigos HTTP**: Los errores de validación (`400`), autenticación (`401`), autorización (`403`), recurso inexistente (`404`), exceso de tamaño (`413`) o fallo de servicio externo (`502`/`503`) mantienen el formato uniforme de excepción del proyecto.

---

### 2.6. Migraciones de Base de Datos y Configuración *(Condicional: Aplica si se modifican esquemas Prisma, variables de entorno o manifiestos K8s)*

*Si el PR no incluye cambios de base de datos ni de configuración/infraestructura, marcar esta sección como **N/A**.*

- [ ] **Esquema y migraciones Prisma (`schema.prisma` / `migrations/`)**:
  - El archivo `migration.sql` generado corresponde exclusivamente al microservicio afectado (respetando el patrón *Database-per-Service*).
  - Se indica claramente si la migración requiere ejecutarse en el clúster mediante el flujo establecido en [MIGRACIONES_PRISMA.md](MIGRACIONES_PRISMA.md).
- [ ] **Variables de entorno y configuración**:
  - Toda nueva variable de entorno requerida al arrancar (`getOrThrow` o `ConfigService`) está documentada en `.env.example` y contemplada en la configuración del entorno de despliegue.
- [ ] **Manifiestos de Kubernetes (`infrastructure/k8s/`)**:
  - Respetan los límites de recursos (`requests` y `limits`) compatibles con la cuota del namespace `student-daalvarez` y el `LimitRange` del clúster UCT.

---

## 3. Procedimiento para Registrar Observaciones y su Resolución

Durante la revisión cruzada en GitHub, el revisor y el autor deben seguir el siguiente flujo trazable:

### Paso 1: Registro de observaciones por el Revisor
1. El integrante que revisa el PR inspecciona la pestaña **Files changed** en GitHub contrastando cada sección de este checklist.
2. Si detecta algún incumplimiento (por ejemplo: falta `Closes #<numero>`, se incluyó código fuera de alcance, falla un test o se rompe un contrato), debe dejar **comentarios en línea (*inline review comments*)** directamente sobre el archivo y línea afectada, indicando:
   - **Qué criterio se incumple** (ej. *"Alcance / Contrato: se eliminó el campo `ubicacion` requerido por Android"*).
   - **Acción concreta esperada** para resolverlo.
3. Al finalizar la revisión en **Review changes**:
   - Si no hay observaciones bloqueantes, selecciona **Approve** adjuntando el resumen del checklist verificado.
   - Si existen observaciones que deben corregirse antes de integrar, selecciona **Request changes** o **Comment** listando los puntos pendientes.

### Paso 2: Corrección y respuesta por el Autor del PR
1. El autor realiza los ajustes solicitados en su misma rama `feature/<numero-issue>-<slug>` y sube nuevos commits (`git push`).
2. En cada hilo de observación abierto en el PR, el autor responde brevemente indicando el commit donde quedó solucionado (ej. *"Corregido en `a1b2c3d`: se restauró el campo y se agregó el test unitario"*).

### Paso 3: Verificación del cierre y Aprobación Final
1. El revisor comprueba los nuevos commits en el PR y verifica que la observación haya sido efectivamente subsanada.
2. Una vez verificado, marca el hilo como resuelto (**Resolve conversation**), emite la aprobación (**Approve**) y, tras confirmar que los trabajos de CI/CD aplicables se encuentran en verde, se procede con el **Merge pull request** hacia `integration`.

---

## 4. Plantilla Copiable para Comentarios de Revisión en GitHub

Los revisores pueden copiar y pegar la siguiente plantilla en el comentario de revisión de cualquier Pull Request:

```markdown
### Revisión Cruzada de Pull Request — Issue #<NUMERO>

#### 1. Flujo Git y Alcance
- [x] Rama `feature/<numero-issue>-<slug>` apuntando a `integration`
- [x] Vinculación `Closes #<numero>` presente y correcta
- [x] Cambios acotados al alcance de la Issue (sin elementos fuera de alcance)
- [x] Criterios de aceptación cumplidos y con evidencia / pruebas pasando
- [x] Ausencia de secretos, tokens o credenciales en el diff

#### 2. Verificaciones Técnicas Condicionales (marcar N/A si no aplica)
- [ ] Contratos `/api/v1` y compatibilidad con consumidores (Web / Android) — `[Cumple / N/A]`
- [ ] Migraciones Prisma y aislamiento de base de datos — `[Cumple / N/A]`
- [ ] Variables de entorno (`.env.example`) y configuración K8s — `[Cumple / N/A]`

#### 3. Observaciones
- Sin observaciones bloqueantes / (Detallar observaciones puntuales y su estado de resolución).
```
