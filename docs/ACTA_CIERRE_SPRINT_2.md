# Acta de Cierre - Sprint 2 (Taller II)

## 1. Identificación del Cierre

- **Proyecto:** Ojo Ciudadano — Plataforma de Reportes e Incidentes Urbanos
- **Asignatura / Equipo:** Taller de Integración II (en coordinación técnica con Taller de Integración IV)
- **Sprint evaluado:** Sprint 2
- **Issue de referencia:** [#161](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/161) (`MTN-08`)
- **Informe base de referencia:** [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md) (Issue [#160](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/160), [PR #265](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/265))
- **Fecha de cierre:** 6 de noviembre de 2026
- **Rama de integración:** `integration`
- **Repositorio:** [Ojo-Ciudadano/Ojo_Ciudadano_Proyecto](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto)

### Participantes Confirmados

#### Equipo Taller II (Backend, Plataforma Web, QA y Documentación)
| Integrante | Usuario GitHub | Áreas de responsabilidad en Sprint 2 |
| :--- | :--- | :--- |
| **Martín Maier** | `@martinchinaski` | API de evidencias (#86), generación de notificaciones (#113), ADR `/api/v1` (#126), CORS y entornos (#127), estrategia K8s Web (#128), guards por rol en React (#130), checklist de PR (#159), acta de cierre (#161) y corrección de Ingress/Dockerfile en `integration`. |
| **Cristóbal Santibáñez** | `@cristobalsantibanez2025-sudo` | Modelo Prisma de evidencias (#80), prioridad de incidentes (#124), despliegue Web en K8s UCT (#129), mapa Web y marcadores (#139, #140, #141), seeds reproducibles (#151, #152), health probes K8s (#153) y backup/restore de PostgreSQL (#154). |
| **María Marilao** | `@mmarilao2025-pixel` | Integración de Supabase Storage (#85), evidencias en respuestas (#106), matriz y API de estados (#116, #117), filtros de consulta (#119), API de historial (#120), asignación de responsables (#123) y detalle ampliado (#125). |
| **Franchesca Marcial** | `@fmarcial066` | Autorización por roles en backend (#100), API de notificaciones de usuario (#114), perfil `/usuarios/me` (#118), API de listado y cambio de rol de usuarios (#121, #122), consumo de notificaciones Web (#144) y gestión Web de usuarios y roles (#147, #148). |
| **Bárbara Sáez** | `@Barbara282005` | Tabla administrativa, filtros y paginación Web (#131, #132), detalle administrativo, estado, asignación y prioridad Web (#133, #134, #135), formulario de reporte y subida de fotos Web (#136, #137), galería de evidencias (#138), listado de notificaciones (#145), inicio ciudadano (#146) y autorregistro Web ([PR #266](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/266)). |
| **Bernardo Sandoval** | `@Kurainek` | Navegación y filtros en mapa Web (#142, #143), cronología de historial Web (#149), manejo de errores 401/403 (#150), plan de pruebas Web por rol (#155), contratos Swagger (#156), colección Postman (#157), automatización E2E (#158) e informe final del Sprint 2 (#160). |

#### Equipo Taller IV (Aplicación Móvil Android e Infraestructura Compartida)
| Integrante | Usuario GitHub | Rol en la integración |
| :--- | :--- | :--- |
| **Diego Álvarez** | `@diegoa18` | Administración del namespace `student-daalvarez` en Kubernetes UCT (#78, #84), cliente remoto Android (#79, #87, #115) y flujos administrativos/notificaciones/perfil Android (#162 a #168). |
| **Oscar Barra** | `@oscarbarra` | Permisos, ubicación GPS, captura/galería de evidencias y mapa interactivo en Android (#81 a #83, #88 a #91, #107 a #112). |
| **Máximo Mora** | `@ummitta` | Autenticación, sesión JWT, navegación por rol, registro ciudadano y flujo de gestión de incidentes en Android (#92 a #99, #101 a #105, #202). |

---

## 2. Resumen del Objetivo del Sprint 2 y Resultado Alcanzado

### Objetivo del Sprint 2
Completar la integración de extremo a extremo entre el monorepo de microservicios backend (`api-gateway`, `auth-usuarios`, `incidentes`, `notificaciones`), la plataforma Web en React y la aplicación móvil Android sobre el entorno remoto en **Kubernetes UCT**, cubriendo:
1. Gestión de evidencias fotográficas sobre **Supabase Storage**.
2. Ciclo de vida completo de incidentes (transiciones válidas de estado, historial auditable, asignación de responsable y nivel de prioridad).
3. Autorización diferenciada por rol (`CIUDADANO` y `ADMIN`) tanto en el backend como en las interfaces de usuario.
4. Despacho y consulta de notificaciones ante eventos del incidente.
5. Operación en Kubernetes UCT (Ingress, semillas reproducibles, health probes y respaldos de bases de datos) y aseguramiento de calidad (Swagger, Postman, plan de pruebas y flujo E2E).

### Resultado Alcanzado (Coherencia con el Informe Final #160 y Estado del Repositorio)
De acuerdo con lo documentado en [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md) y el estado verificable de la rama `integration` al 6 de noviembre de 2026:
- **Implementación en código e integración de ramas:** El **100% de las Issues funcionales, de infraestructura y de documentación asignadas a Taller II** (así como las de Taller IV del Sprint 2) se encuentran implementadas, revisadas mediante Pull Request y fusionadas en `integration`. Se incluye además la pantalla de registro de usuarios en la vista de login Web ([PR #266](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/266)).
- **Estado de verificación reportado en #160:** Tal como consigna el informe final ([`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md), secciones 1, 3, 4, 5 y 8), al momento de emitir dicho informe (5 de noviembre de 2026) el cierre verificable fue declarado **parcial** debido a que:
  1. La corrida original del script E2E (#158) registrada el 2 de noviembre de 2026 (`03:40:22 UTC`) en [`docs/REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json) falló en el paso `[01]` con `HTTP 404 Not Found` de Nginx al consultar `https://api.ojociudadano.dev.censei.cl/api/v1/auth/login`.
  2. No se versionaron en el repositorio las capturas visuales de los flujos Web de Ciudadano y Administrador ni la ejecución completa de la matriz manual de pruebas de roles (#155).
- **Correcciones posteriores en infraestructura remota (5 y 6 de noviembre de 2026):**
  - Mediante el [PR #256](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/256) y los commits posteriores en `integration` (`89d891f`, `a69af46`, `94ac8a2`), se separaron los manifiestos de Ingress (`api-gateway-ingress` para `ojociudadano.dev.censei.cl` y `api-ojociudadano-ingress` para `api-ojociudadano.dev.censei.cl`), se ajustó la regla de `api-gateway-ingress.yaml` a una única ruta `/` para cumplir con la política del webhook `rke2-edu-ingress-hostname` del clúster UCT y se delegó el proxy de `/api/` a Nginx (`web/frontend/nginx.conf`) conservando el prefijo `/api/v1`.
  - Con la sincronización de ambos Ingress en el clúster UCT (`student-daalvarez`), el host `ojociudadano.dev.censei.cl` quedó operativo. No obstante, para mantener estricta fidelidad con [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md), el archivo versionado [`docs/REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json) y la ausencia de capturas en el repositorio se mantienen registrados dentro de los pendientes de evidencia documental que se detallan en la Sección 7 de esta acta.

---

## 3. Entornos de Ejecución: Remoto y Local

En concordancia con la Sección 2 de [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md) y los documentos de arquitectura ([`docs/ESTRATEGIA_DESPLIEGUE_WEB_K8S.md`](./ESTRATEGIA_DESPLIEGUE_WEB_K8S.md), [`docs/CONFIGURACION_ENTORNOS_CORS.md`](./CONFIGURACION_ENTORNOS_CORS.md)):

| Entorno | Tecnología / Ubicación | Propósito y Rol en el Proyecto |
| :--- | :--- | :--- |
| **Entorno Remoto (Producción Académica)** | **Kubernetes UCT** (clúster RKE2 / `vcluster` en namespace `student-daalvarez`) + **Supabase Storage** (`incident-evidences`) + **GHCR** | Entorno oficial de despliegue remoto e integración entre la Web (`https://ojociudadano.dev.censei.cl`), la API dedicada para clientes móviles (`https://api-ojociudadano.dev.censei.cl`), los 4 servicios NestJS y las 3 instancias PostgreSQL aisladas. |
| **Entorno Local (Desarrollo)** | **Docker Compose** (`web/backend/docker-compose.yml`) + procesos locales Node.js/Vite | Herramienta de desarrollo local para levantar las tres bases de datos PostgreSQL (`5432`, `5433`, `5434`), ejecutar migraciones/seeds y correr pruebas locales. **No sustituye la verificación en el entorno remoto Kubernetes UCT.** |

---

## 4. Reasignación de las Siete Issues a Taller II (Sin Duplicidad de Trabajo)

Tal como establece la Sección 7 de [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md), las siguientes **siete Issues** existentes en el backlog original fueron reasignadas oficialmente a integrantes de **Taller II** durante la planificación del Sprint 2 para cubrir las dependencias de backend requeridas por ambos talleres. **Representan trabajo único ejecutado por Taller II en GitHub y no constituyen tareas duplicadas:**

| Issue | Título | Responsable (Taller II) | PR de respaldo | Estado en GitHub |
| :---: | :--- | :--- | :---: | :---: |
| [#80](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/80) | Implementar el modelo persistente de evidencias de incidentes mediante Prisma | Cristóbal Santibáñez (`@cristobalsantibanez2025-sudo`) | [PR #180](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/180) | `closed` |
| [#85](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/85) | Implementar almacenamiento y recuperación de evidencias mediante Supabase Storage en el microservicio de Incidentes | María Marilao (`@mmarilao2025-pixel`) | [PR #184](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/184) | `closed` |
| [#86](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/86) | Implementar la API para subir y consultar evidencias asociadas a un incidente | Martín Maier (`@martinchinaski`) | [PR #189](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/189) | `closed` |
| [#100](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/100) | Implementar autorización de endpoints protegidos para los roles Ciudadano y Administrador | Franchesca Marcial (`@fmarcial066`) | [PR #183](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/183) | `closed` |
| [#106](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/106) | Incorporar las evidencias disponibles a las respuestas de incidentes requeridas por la aplicación | María Marilao (`@mmarilao2025-pixel`) | [PR #186](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/186) | `closed` |
| [#113](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/113) | Implementar la generación y persistencia de notificaciones ante cambios relevantes del incidente | Martín Maier (`@martinchinaski`) | [PR #216](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/216) | `closed` |
| [#114](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/114) | Implementar la API para consultar las notificaciones del usuario autenticado | Franchesca Marcial (`@fmarcial066`) | [PR #223](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/223) | `closed` |

---

## 5. Estado Real de las Issues del Sprint 2 en GitHub

A continuación se registra el estado real verificado en GitHub al 6 de noviembre de 2026 para la totalidad de las Issues del Sprint 2, separando explícitamente el cierre de implementación en GitHub del estado de verificación visual/remota reportado en [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md).

### 5.1 Issues Completadas de Taller II (Cerradas y Fusionadas en `integration`)

| Issue | Título | Responsable | PR Fusionado | Estado GitHub | Estado de Verificación (según #160 y repo) |
| :---: | :--- | :--- | :---: | :---: | :--- |
| [#80](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/80) | Modelo persistente de evidencias mediante Prisma | `@cristobalsantibanez2025-sudo` | [PR #180](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/180) | `closed` | Implementada y probada en tests; verificación remota pendiente en #160 |
| [#85](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/85) | Almacenamiento y recuperación en Supabase Storage | `@mmarilao2025-pixel` | [PR #184](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/184) | `closed` | Implementada y probada en tests; verificación remota pendiente en #160 |
| [#86](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/86) | API para subir y consultar evidencias de un incidente | `@martinchinaski` | [PR #189](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/189) | `closed` | Implementada con tests unitarios/e2e; verificación remota pendiente en #160 |
| [#100](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/100) | Autorización de endpoints protegidos por rol | `@fmarcial066` | [PR #183](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/183) | `closed` | Implementada; matriz manual #155 pendiente de ejecución completa |
| [#106](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/106) | Evidencias en respuestas de incidentes | `@mmarilao2025-pixel` | [PR #186](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/186) | `closed` | Implementada; verificación remota pendiente en #160 |
| [#113](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/113) | Generación y persistencia de notificaciones | `@martinchinaski` | [PR #216](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/216) | `closed` | Implementada con tests unitarios; captura remota pendiente en #160 |
| [#114](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/114) | API para consultar notificaciones del usuario autenticado | `@fmarcial066` | [PR #223](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/223) | `closed` | Implementada; verificación remota pendiente en #160 |
| [#116](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/116) | Definir matriz de transiciones válidas de estados | `@mmarilao2025-pixel` | [PR #196](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/196) | `closed` | Documentada en [`docs/MATRIZ_TRANSICIONES_INCIDENTES.md`](./MATRIZ_TRANSICIONES_INCIDENTES.md) y validada |
| [#117](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/117) | `PATCH /incidentes/:id/estado` con historial | `@mmarilao2025-pixel` | [PR #206](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/206) | `closed` | Implementada y cubierta por pruebas unitarias |
| [#118](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/118) | `GET /usuarios/me` para perfil y rol autenticado | `@fmarcial066` | [PR #199](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/199) | `closed` | Implementada y cubierta por pruebas unitarias |
| [#119](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/119) | Filtros por estado, comuna y categoría en `GET /incidentes` | `@mmarilao2025-pixel` | [PR #213](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/213) | `closed` | Implementada y documentada en [`docs/API_FILTROS_INCIDENTES.md`](./API_FILTROS_INCIDENTES.md) |
| [#120](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/120) | `GET /incidentes/:id/historial` | `@mmarilao2025-pixel` | [PR #214](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/214) | `closed` | Implementada y documentada en [`docs/API_HISTORIAL_INCIDENTE.md`](./API_HISTORIAL_INCIDENTE.md) |
| [#121](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/121) | `GET /usuarios` paginado con filtro por rol | `@fmarcial066` | [PR #198](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/198) | `closed` | Implementada y cubierta por pruebas unitarias |
| [#122](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/122) | `PATCH /usuarios/:id/rol` restringido al Administrador | `@fmarcial066` | [PR #219](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/219) | `closed` | Implementada y documentada en [`docs/CAMBIO_ROLES.md`](./CAMBIO_ROLES.md) |
| [#123](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/123) | Asignación y reasignación de responsables de incidentes | `@mmarilao2025-pixel` | [PR #215](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/215) | `closed` | Implementada y cubierta por pruebas unitarias |
| [#124](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/124) | Prioridad inicial por categoría y modificación | `@cristobalsantibanez2025-sudo` | [PR #192](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/192) | `closed` | Implementada y cubierta por pruebas unitarias |
| [#125](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/125) | Ampliar detalle de incidentes con historial y asignación | `@mmarilao2025-pixel` | [PR #218](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/218) | `closed` | Implementada y cubierta por pruebas unitarias |
| [#126](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/126) | ADR sobre continuidad del versionado `/api/v1` | `@martinchinaski` | [PR #190](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/190) | `closed` | Documentada en [`docs/ADR_VERSIONADO_API_V1.md`](./ADR_VERSIONADO_API_V1.md) |
| [#127](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/127) | Configurar orígenes CORS y URLs por entorno | `@martinchinaski` | [PR #179](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/179) | `closed` | Implementada y documentada en [`docs/CONFIGURACION_ENTORNOS_CORS.md`](./CONFIGURACION_ENTORNOS_CORS.md) |
| [#128](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/128) | Definir estrategia de despliegue Web en Kubernetes UCT | `@martinchinaski` | [PR #182](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/182) | `closed` | Documentada en [`docs/ESTRATEGIA_DESPLIEGUE_WEB_K8S.md`](./ESTRATEGIA_DESPLIEGUE_WEB_K8S.md) |
| [#129](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/129) | Crear imagen y desplegar la Web en Kubernetes UCT | `@cristobalsantibanez2025-sudo` | [PR #207](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/207), [PR #226](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/226), [PR #256](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/256) | `closed` | Desplegada en K8s UCT; requiere `rollout` de nueva imagen para últimos PRs Web |
| [#130](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/130) | Guards de ruta y menú por rol en React | `@martinchinaski` | [PR #200](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/200) | `closed` | Implementada; captura remota pendiente en #160 |
| [#131](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/131) | Tabla de gestión de incidentes del Administrador | `@Barbara282005` | [PR #212](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/212) | `closed` | Implementada; captura pendiente en #160 |
| [#132](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/132) | Filtros y paginación en tabla administrativa Web | `@Barbara282005` | [PR #222](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/222) | `closed` | Implementada; captura pendiente en #160 |
| [#133](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/133) | Construir detalle del incidente en la Web | `@Barbara282005` | [PR #229](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/229) | `closed` | Implementada; captura pendiente en #160 |
| [#134](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/134) | Cambio de estado de incidentes en la Web | `@Barbara282005` | [PR #231](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/231) | `closed` | Implementada; captura pendiente en #160 |
| [#135](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/135) | Asignación de responsables y prioridad en la Web | `@Barbara282005` | [PR #232](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/232) | `closed` | Implementada; captura pendiente en #160 |
| [#136](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/136) | Formulario de nuevo reporte en la Web | `@Barbara282005` | [PR #241](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/241) | `closed` | Implementada; captura pendiente en #160 |
| [#137](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/137) | Subida de fotografías en formulario de nuevo reporte Web | `@Barbara282005` | [PR #257](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/257) | `closed` | Implementada con pruebas unitarias; captura y redeploy de imagen Web pendientes |
| [#138](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/138) | Galería de evidencias en el detalle del incidente Web | `@Barbara282005` | [PR #264](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/264) | `closed` | Implementada con pruebas unitarias; captura y redeploy de imagen Web pendientes |
| [#139](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/139) | Pantalla de mapa de incidentes en la Web | `@cristobalsantibanez2025-sudo` | [PR #217](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/217) | `closed` | Implementada; captura pendiente en #160 |
| [#140](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/140) | Marcadores de incidentes georreferenciados en mapa Web | `@cristobalsantibanez2025-sudo` | [PR #225](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/225) | `closed` | Implementada; captura pendiente en #160 |
| [#141](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/141) | Información resumida al seleccionar marcador en mapa Web | `@cristobalsantibanez2025-sudo` | [PR #237](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/237), [PR #254](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/254) | `closed` | Popup e información integrados junto con #142; captura pendiente en #160 |
| [#142](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/142) | Navegación desde marcador al detalle del incidente Web | `@Kurainek` | [PR #254](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/254) | `closed` | Implementada con pruebas unitarias; captura pendiente en #160 |
| [#143](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/143) | Filtros por estado y categoría en el mapa Web | `@Kurainek` | [PR #235](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/235) | `closed` | Implementada; captura pendiente en #160 |
| [#144](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/144) | Consumo de la API de notificaciones en la Web | `@fmarcial066` | [PR #244](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/244) | `closed` | Implementada con pruebas unitarias; captura pendiente en #160 |
| [#145](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/145) | Construir lista de notificaciones en la Web | `@Barbara282005` | [PR #247](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/247) | `closed` | Implementada con pruebas unitarias; captura pendiente en #160 |
| [#146](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/146) | Inicio del Ciudadano con Todos y Mis reportes | `@Barbara282005` | [PR #245](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/245) | `closed` | Implementada; captura pendiente en #160 |
| [#147](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/147) | Lista de usuarios con sus roles en la Web | `@fmarcial066` | [PR #224](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/224) | `closed` | Implementada con pruebas unitarias; captura pendiente en #160 |
| [#148](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/148) | Selector de cambio de rol en la Web | `@fmarcial066` | [PR #251](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/251) | `closed` | Implementada con pruebas unitarias; captura y redeploy de imagen Web pendientes |
| [#149](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/149) | Mostrar timeline de historial del incidente en la Web | `@Kurainek` | [PR #249](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/249) | `closed` | Implementada con pruebas unitarias; captura pendiente en #160 |
| [#150](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/150) | Manejar errores 401 y 403 de forma visible en la Web | `@Kurainek` | [PR #208](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/208) | `closed` | Implementada con pruebas unitarias; captura pendiente en #160 |
| [#151](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/151) | Cargar seed de ciudadanos demo en Auth/Usuarios | `@cristobalsantibanez2025-sudo` | [PR #227](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/227) | `closed` | Implementada y documentada en [`docs/MIGRACIONES_PRISMA.md`](./MIGRACIONES_PRISMA.md) |
| [#152](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/152) | Cargar seed de incidentes demo en La Araucanía | `@cristobalsantibanez2025-sudo` | [PR #238](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/238) | `closed` | Implementada y documentada en [`docs/MIGRACIONES_PRISMA.md`](./MIGRACIONES_PRISMA.md) |
| [#153](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/153) | Configurar probes de salud backend en Kubernetes UCT | `@cristobalsantibanez2025-sudo` | [PR #250](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/250) | `closed` | Implementada y documentada en [`docs/KUBERNETES_HEALTH_PROBES.md`](./KUBERNETES_HEALTH_PROBES.md) |
| [#154](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/154) | Documentar y automatizar backup y restore de PostgreSQL | `@cristobalsantibanez2025-sudo` | [PR #260](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/260) | `closed` | Implementada en [`infrastructure/k8s/jobs/postgres-backup.yaml`](../infrastructure/k8s/jobs/postgres-backup.yaml) y [`docs/POSTGRESQL_BACKUP_RESTORE.md`](./POSTGRESQL_BACKUP_RESTORE.md) |
| [#155](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/155) | Plan de pruebas Web por rol y matriz de permisos | `@Kurainek` | [PR #211](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/211) | `closed` | Documentado en [`docs/Plan_Pruebas_Web_Roles.md`](./Plan_Pruebas_Web_Roles.md); ejecución manual completa pendiente |
| [#156](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/156) | Documentar en Swagger módulos de Sprint 2 | `@Kurainek` | [PR #230](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/230) | `closed` | Publicado en `/api/docs` y verificado con tests de contrato |
| [#157](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/157) | Elaborar colección Postman del Sprint 2 | `@Kurainek` | [PR #234](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/234) | `closed` | Versionada en [`postman/`](../postman/) |
| [#158](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/158) | Automatizar pruebas E2E del ciclo de vida de incidentes | `@Kurainek` | [PR #263](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/263) | `closed` | Script fusionado (`web/backend/test/incident-lifecycle-e2e.ts`); [`docs/REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json) conserva el fallo 404 del 2-nov previo al fix de Ingress |
| [#159](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/159) | Elaborar checklist de revisión cruzada de PRs | `@martinchinaski` | [PR #191](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/191) | `closed` | Documentado en [`docs/CHECKLIST_REVISION_CRUZADA_PR.md`](./CHECKLIST_REVISION_CRUZADA_PR.md) |
| [#160](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/160) | Redactar informe final del Sprint 2 con evidencia | `@Kurainek` | [PR #265](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/265) | `closed` | Versionado en [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md) |

### 5.2 Issues Completadas de Taller IV en el Repositorio Compartido (Cerradas y Fusionadas)

| Rango de Issues | Áreas Cubiertas (Android e Infraestructura K8s) | Responsables | PRs de Respaldo | Estado GitHub |
| :--- | :--- | :--- | :--- | :---: |
| [#78](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/78), [#79](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/79), [#84](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/84), [#87](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/87), [#115](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/115), [#162](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/162)–[#168](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/168) | Infraestructura K8s UCT y Supabase, configuración remota Android, datos de evidencias y notificaciones, gestión administrativa (filtros, historial, asignación, prioridad), pantalla de notificaciones y perfil autenticado | `@diegoa18` | PRs #170, #173, #178, #193, #220, #236, #240, #252, #253, #255, #256, #262, #268 | `closed` |
| [#81](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/81)–[#83](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/83), [#88](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/88)–[#91](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/91), [#107](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/107)–[#112](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/112) | Permisos de ubicación, GPS en tiempo real, cámara/galería/previsualización de fotos, mapa interactivo, marcadores, resumen, navegación al detalle, filtros de mapa y visualización de evidencias en Android | `@oscarbarra` | PRs #185, #187, #197, #209, #233, #239, #246, #248, #258, #259, #267, #269, #271 | `closed` |
| [#92](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/92)–[#99](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/99), [#101](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/101)–[#105](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/105), [#202](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/202) | Login, `AuthViewModel`, gestión de sesión JWT, rol en sesión, navegación y restricciones por rol, registro ciudadano y flujos Ciudadano/Administrador (listado, detalle y cambio de estado) en Android | `@ummitta` | PRs #174–#176, #181, #188, #194, #201, #203–#205, #210, #221, #228, #242, #261, #270 | `closed` |

### 5.3 Issues en Revisión al Cierre

| Issue | Título | Responsable | Rama / PR | Estado Real |
| :---: | :--- | :--- | :--- | :--- |
| [#161](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/161) | Redactar el acta de cierre del Sprint 2 | Martín Maier (`@martinchinaski`) | `feature/161-sprint2-closing-minutes` | `open` (única Issue abierta del repositorio; se cierra al fusionar el PR de esta acta) |

---

## 6. Registro de Horas Asignadas vs. Horas Reales

Siguiendo el criterio de integridad del proyecto, **no se inventan ni estiman horas reales ni asignadas que no estén respaldadas en el repositorio o en GitHub Issues/Projects**:

- **Estado de los datos en el repositorio y GitHub API:** Ninguna de las Issues del Sprint 2 (`#78` a `#168`, `#202`) ni los documentos versionados en [`docs/`](./) contienen campos numéricos confirmados de horas estimadas ni registros de horas reales trabajadas por integrante.
- **Distinción explícita:** La ausencia de registro horario se declara formalmente como **`Información no registrada / Sin datos confirmados en el repositorio`**, y en ningún caso equivale a `0 horas`.

| Categoría | Horas Asignadas | Horas Reales | Observación Verificable |
| :--- | :---: | :---: | :--- |
| Desarrollo Backend y Microservicios (Taller II) | Sin registro en GitHub | Sin registro en GitHub | Datos faltantes (no equivale a `0`). Respaldado por commits y PRs en sección 5.1. |
| Desarrollo Frontend Web y Pruebas (Taller II) | Sin registro en GitHub | Sin registro en GitHub | Datos faltantes (no equivale a `0`). Respaldado por commits y PRs en sección 5.1. |
| Infraestructura K8s, QA y Documentación (Taller II) | Sin registro en GitHub | Sin registro en GitHub | Datos faltantes (no equivale a `0`). Respaldado por commits y PRs en sección 5.1. |

---

## 7. Trabajo Pendiente, Limitaciones Verificadas y Acuerdos de Continuidad

En coherencia directa con las Secciones 3, 4, 5, 8 y 9 de [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md), aunque el código fuente de las Issues del Sprint 2 ya está fusionado en `integration`, **quedan pendientes las siguientes actividades de verificación y operación para consolidar la evidencia remota**:

### 7.1 Pendientes y sus Dependencias

| ID | Pendiente Identificado | Dependencia Técnica | Estado Actual | Responsable Acordado |
| :---: | :--- | :--- | :--- | :--- |
| **P-01** | **Actualizar la imagen Docker de `web-frontend` en Kubernetes UCT (`student-daalvarez`)** con los últimos PRs fusionados en `integration` (#137 subida de fotos, #138 galería de evidencias, #148 selector de rol y #266 registro Web). | Acceso a `ghcr.io/ojo-ciudadano` y credenciales de `kubectl` en `student-daalvarez`, dado que no existe CD automático hacia el clúster UCT. | El pod `web-frontend` en el clúster sirve la compilación previa (`v1.0.3` del 5-nov 04:26 UTC). | Diego Álvarez (`@diegoa18`) en coordinación con Taller II |
| **P-02** | **Incorporar capturas visuales identificadas (Rol, Entorno y Funcionalidad)** de los flujos Web Ciudadano, Administrador y gestión de roles en el informe final. | Depende de **P-01** para que las vistas de #137, #138, #148 y #266 estén publicadas en `https://ojociudadano.dev.censei.cl` (o indicar explícitamente si se toman en `Local - Docker Compose`). | Declarado pendiente en Sección 3 de [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md). | Bernardo Sandoval (`@Kurainek`) / Bárbara Sáez (`@Barbara282005`) / Franchesca Marcial (`@fmarcial066`) |
| **P-03** | **Actualizar el artefacto versionado [`docs/REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json)** con una corrida posterior a la corrección del Ingress ([PR #256](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/256) y commit `89d891f`). | Depende de que los servicios en `student-daalvarez` y el seed de ciudadanos (#151) estén activos durante la ejecución de `npm run test:e2e:lifecycle`. | El JSON versionado en `integration` aún refleja el fallo `HTTP 404` del 2 de noviembre (`api.ojociudadano.dev.censei.cl`). | Bernardo Sandoval (`@Kurainek`) |
| **P-04** | **Ejecutar y registrar la campaña manual completa de la matriz de permisos y roles (#155)**, incluyendo casos negativos (`CP-NEG-01` a `CP-NEG-07`) y pruebas de concurrencia. | Depende de **P-01** para verificar las restricciones sobre la última versión de la UI Web remota. | Plan documentado en [`docs/Plan_Pruebas_Web_Roles.md`](./Plan_Pruebas_Web_Roles.md); ejecución pendiente según Sección 4 de #160. | Equipo QA Taller II (`@Kurainek`, `@fmarcial066`, `@martinchinaski`) |

### 7.2 Acuerdos de Continuidad y Próximos Pasos

1. **Despliegue de sincronización del Frontend Web (P-01):**
   - Una vez fusionada la totalidad de las ramas en `integration`, se construirá y publicará la nueva imagen del contenedor `web-frontend` hacia GHCR (compilando el bundle estático con `VITE_API_URL=https://ojociudadano.dev.censei.cl`) y se ejecutará `kubectl rollout restart deployment/web-frontend -n student-daalvarez` siguiendo el procedimiento de [`docs/DESPLIEGUE_WEB_K8S_GHCR.md`](./DESPLIEGUE_WEB_K8S_GHCR.md).
2. **Actualización de evidencia remota (P-02, P-03 y P-04):**
   - Con la imagen actualizada en el clúster, se regenerará [`docs/REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json) apuntando a `https://ojociudadano.dev.censei.cl/api/v1` y se adjuntarán las capturas reales identificando su entorno, sin mezclar capturas locales con remotas.
3. **Preservación de convenciones y controles de calidad:**
   - Cualquier ajuste correctivo posterior a este cierre deberá ingresar a `integration` mediante Pull Request aplicando [`docs/CHECKLIST_REVISION_CRUZADA_PR.md`](./CHECKLIST_REVISION_CRUZADA_PR.md) (#159), respetando el prefijo `/api/v1` documentado en [`docs/ADR_VERSIONADO_API_V1.md`](./ADR_VERSIONADO_API_V1.md) (#126) y manteniendo una única ruta `/` en `api-gateway-ingress.yaml` para cumplir con la política del webhook `rke2-edu-ingress-hostname` de la UCT.

---

## 8. Trazabilidad de Fuentes Utilizadas

Todas las afirmaciones de esta acta son verificables en los siguientes artefactos versionados del repositorio:

- **Informe Final del Sprint 2 (#160):** [`docs/INFORME_FINAL_SPRINT_2.md`](./INFORME_FINAL_SPRINT_2.md)
- **Reporte E2E estructurado (#158):** [`docs/REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json) y script [`web/backend/test/incident-lifecycle-e2e.ts`](../web/backend/test/incident-lifecycle-e2e.ts)
- **Plan de Pruebas Web y Matriz de Roles (#155):** [`docs/Plan_Pruebas_Web_Roles.md`](./Plan_Pruebas_Web_Roles.md)
- **Estrategia y Guía de Despliegue Web en K8s UCT (#128 / #129):** [`docs/ESTRATEGIA_DESPLIEGUE_WEB_K8S.md`](./ESTRATEGIA_DESPLIEGUE_WEB_K8S.md) y [`docs/DESPLIEGUE_WEB_K8S_GHCR.md`](./DESPLIEGUE_WEB_K8S_GHCR.md)
- **Probes de Salud y Backups PostgreSQL en K8s (#153 / #154):** [`docs/KUBERNETES_HEALTH_PROBES.md`](./KUBERNETES_HEALTH_PROBES.md) y [`docs/POSTGRESQL_BACKUP_RESTORE.md`](./POSTGRESQL_BACKUP_RESTORE.md)
- **ADR de Versionado, CORS y Checklist de PRs (#126 / #127 / #159):** [`docs/ADR_VERSIONADO_API_V1.md`](./ADR_VERSIONADO_API_V1.md), [`docs/CONFIGURACION_ENTORNOS_CORS.md`](./CONFIGURACION_ENTORNOS_CORS.md) y [`docs/CHECKLIST_REVISION_CRUZADA_PR.md`](./CHECKLIST_REVISION_CRUZADA_PR.md)
- **Contratos de API y Seeds (#86 / #116 / #117 / #119 / #120 / #122 / #151 / #152 / #157):** [`docs/API_EVIDENCIAS_INCIDENTES.md`](./API_EVIDENCIAS_INCIDENTES.md), [`docs/MATRIZ_TRANSICIONES_INCIDENTES.md`](./MATRIZ_TRANSICIONES_INCIDENTES.md), [`docs/API_CAMBIO_ESTADO_INCIDENTE.md`](./API_CAMBIO_ESTADO_INCIDENTE.md), [`docs/API_FILTROS_INCIDENTES.md`](./API_FILTROS_INCIDENTES.md), [`docs/API_HISTORIAL_INCIDENTE.md`](./API_HISTORIAL_INCIDENTE.md), [`docs/CAMBIO_ROLES.md`](./CAMBIO_ROLES.md), [`docs/MIGRACIONES_PRISMA.md`](./MIGRACIONES_PRISMA.md) y [`postman/`](../postman/)
