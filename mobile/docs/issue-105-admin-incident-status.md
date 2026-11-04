# Issue #105 — Cambio de estado administrativo Android

## Auditoría inicial (2026-11-04)

Árbol limpio; rama `feature/105-admin-incident-status` ya existente, sin commits
propios y exactamente en `28044ca9f24f7851d434954f66019ff916c634fc`, igual a
`integration` y `origin/integration` después de `git fetch origin`. Se reutiliza
sin recrearla ni mezclar ramas. No había cambios locales que preservar.
No se realizó merge hacia integration.

| Dependencia | Disponibilidad comprobada en la base |
| --- | --- |
| #103 | Listado administrativo y selección por ID, merge PR #221 (`8bc7ac8`). |
| #104 | Detalle, ViewModel y ruta protegida, merge PR #228 (`9f0d38b`). |
| #116 | Matriz versionada (`950d4a1`), `docs/MATRIZ_TRANSICIONES_INCIDENTES.md`. |
| #117 | PATCH real, validaciones y Gateway, merge PR #206 (`0bcb0d2`). |
| #162 | Modelos ampliados, mappers y operaciones administrativas, merge PR #220 (`28044ca`). La Issue seguía abierta en GitHub, pero el código está incorporado. No incluye cambio de estado: su alcance lo reserva para #105. |
| #105 | Abierta; no había implementación equivalente ni PR de esta rama al comenzar. |

Se inspeccionaron DTOs, Retrofit, repositorio, ambos ViewModels administrativos,
navegación, sesión/JWT, errores compartidos y pruebas. No se necesita incorporar
código desde ramas pendientes ni duplicar #162. Los documentos históricos de
#103/#104 mencionan dependencias entonces pendientes; prevalece el árbol actual.
No se encontraron instrucciones AGENTS.md aplicables a Android.

**Fallo previo reproducido:** la compilación unitaria de la base falla porque
`AdminIncidentDetailViewModelTest.Repository` no implementa cuatro métodos de
#162 (`getIncidentHistory`, `assignResponsible`, `updatePriority`, `getCandidates`).
Se adaptó exclusivamente ese doble de pruebas; evidencia inicial en
`/tmp/issue105-baseline.log`.

## Contrato verificado (lectura del backend, sin modificarlo)

- Gateway: `web/backend/apps/api-gateway/src/proxy/proxy.controller.ts`.
- Controlador: `web/backend/apps/incidentes/src/incidentes.controller.ts`.
- Entrada: `web/backend/apps/incidentes/src/dto/update-estado.dto.ts`.
- Validación/persistencia: `web/backend/apps/incidentes/src/incidentes.service.ts`,
  método `updateEstado`; salida mediante `toIncidenteResponse`.
- Reglas: `docs/MATRIZ_TRANSICIONES_INCIDENTES.md` v1.0 y
  `web/backend/apps/incidentes/src/estado/transiciones-incidente.constants.ts`.

`PATCH /api/v1/incidentes/{id}/estado`, `id` UUID del incidente en el path.
Body JSON obligatorio: `estado: String` con nombre canónico y
`observacion: String` con contenido después de `trim()`. No hay límite de
caracteres documentado ni implementado en ese contrato. No se envía usuarioId:
el backend obtiene el actor del JWT. Bearer JWT válido y rol ADMIN obligatorios.

Respuesta HTTP 200: objeto de incidente resumen, con `id`, `estado`
(`id`, `nombre`, `orden`), categoría, ubicación y demás campos existentes.
Se reutilizan `IncidentDto`, `IncidentMapper` e `Incident`; el PATCH no garantiza
el historial ampliado del GET. Por eso se aplica la respuesta confirmada y luego
se consulta `GET /api/v1/incidentes/{id}`.

Códigos documentados: 400 (UUID/estado/observación inválidos), 401 (JWT), 403 (rol),
404 (incidente o estado inexistente), 409 (transición prohibida, incluido el mismo
estado). El Gateway contempla 503 ante indisponibilidad del servicio. 500/otros
5xx, timeout, conectividad y respuesta inesperada se manejan defensivamente;
no se presentan como códigos de negocio nuevos.

Matriz exacta: Nuevo → En revisión → En atención → Resuelto;
Resuelto → En atención o Cerrado. Cerrado no tiene salidas.
Estados desconocidos no ofrecen acciones. No se fijan UUID de estados.

## Implementación y archivos

Rutas relativas a mobile/:

| Archivo | Cambio y responsabilidad |
| --- | --- |
| `app/src/main/java/cl/uct/ojociudadano/data/model/IncidentStatusTransitions.kt` | Nuevo: matriz centralizada por nombres canónicos. |
| `app/src/main/java/cl/uct/ojociudadano/data/remote/dto/UpdateIncidentStatusRequestDto.kt` | Nuevo: campos exactos estado/observacion. |
| `app/src/main/java/cl/uct/ojociudadano/data/remote/ApiService.kt` | PATCH suspendido con TokenSnapshot. |
| `app/src/main/java/cl/uct/ojociudadano/data/repository/IncidentRepository.kt` | Expone updateStatus. |
| `app/src/main/java/cl/uct/ojociudadano/data/repository/RemoteIncidentRepository.kt` | Operación real, validación local, rol ADMIN, identidad y JWT existentes. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailViewModel.kt` | Selección, edición, validación, carga, exclusión de duplicados, resultado y sincronización. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailScreen.kt` | Acciones válidas, diálogo Material, observación y mensajes/carga. |
| `app/src/test/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailViewModelTest.kt` | Matriz, validación, payload, concurrencia, errores, cancelación y sincronización; corrige el doble incompatible de la base. |
| `app/src/test/java/cl/uct/ojociudadano/integration/AuthenticatedIncidentsFlowTest.kt` | Contrato PATCH, JWT, roles, 400/401/403/404/409/500/503, logout, expiración, respuestas tardías y JSON inesperado. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailScreenTest.kt` | Acciones de cada estado, confirmación, observación, cancelación, carga y recarga posterior. |
| `app/src/test/java/cl/uct/ojociudadano/data/evidence/EvidenceImageFetcherTest.kt` | Adapta doble al nuevo método de Repository. |
| `app/src/test/java/cl/uct/ojociudadano/data/repository/RemoteRepositoryTest.kt` | Adapta doble de ApiService. |
| `app/src/test/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsViewModelTest.kt` | Adapta doble de Repository. |
| `app/src/test/java/cl/uct/ojociudadano/ui/report/CreateReportViewModelTest.kt` | Adapta doble de Repository. |
| `app/src/test/java/cl/uct/ojociudadano/ui/reports/ReportsViewModelTest.kt` | Adapta doble de Repository. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/reports/IncidentBrowsingTest.kt` | Adapta doble de Repository para conservar regresiones ciudadanas. |
| `docs/issue-105-admin-incident-status.md` | Nuevo: auditoría, contrato, entrega y verificaciones. |

Flujo: acción válida → diálogo con estado actual/destino → observación con
contenido → ViewModel → Repository → Retrofit → PATCH real. El estado previo
se conserva durante el envío. Se deshabilita el formulario y se publica el bloqueo
antes de lanzar la coroutine para impedir dobles confirmaciones.

Después del 200, se utiliza el incidente devuelto y se recalculan las acciones;
se recarga el detalle para sincronizar sus datos ampliados. Si falla esa recarga,
se conserva el resultado confirmado, se comunica el éxito junto al fallo de
sincronización y se ofrece **Recargar detalle**, que solo hace GET. Se suspenden
nuevas acciones hasta completar esa sincronización.

Ante rechazo del PATCH, no se cambia el incidente confirmado. Se conserva la
observación y se permite corregir/reintentar; 401/403/404 o pérdida de sesión
bloquean nuevas acciones. Ante conflicto o resultado incierto, el mensaje indica
volver a abrir el detalle para consultar el estado actual antes de reintentar.
No hay reintentos automáticos de escritura ni actualizaciones optimistas.

Se reutilizan ruta ADMIN_INCIDENT_DETAIL, protectedComposable, RoleAccessPolicy,
SessionStartupGate, SessionManager, BearerTokenInterceptor, executeApiCall,
StateFlow y los componentes/tema existentes. La navegación por rol impide componer
el detalle para Ciudadano y descarta el flujo al invalidar/cambiar sesión.
El Repository exige adicionalmente ADMIN y la identidad vigente antes del PATCH,
etiqueta la petición con su generación de sesión y descarta respuestas obsoletas.
401 mantiene la invalidación global existente; 403 no cierra sesión. No se guardan
ni registran JWT adicionales ni se muestran diagnósticos privados.

## Validación ejecutada

```bash
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
bash ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.admin.AdminIncidentDetailScreenTest,cl.uct.ojociudadano.ui.admin.AdminIncidentsScreenTest,cl.uct.ojociudadano.ui.startup.RoleNavigationTest,cl.uct.ojociudadano.ui.reports.IncidentBrowsingTest --console=plain
git diff --check
```

- Resultado final Gradle: BUILD SUCCESSFUL, código de salida 0.
- Unitarias: 346 casos, 343 aprobados, 3 integraciones remotas omitidas, 0 fallos/errores.
- Instrumentadas: 24/24 aprobadas, 0 omitidas, Motorola AVD Android 17.
- APK debug y compilación de pruebas instrumentadas correctos.
- Lint: 0 errores, 25 advertencias (mismo total documentado en #104).
- Regresiones cubiertas: listado/detalle, navegación por rol, invalidación/logout,
  navegación ciudadana, login/sesión y demás suites unitarias existentes.
- Diff revisado: únicamente mobile/, sin dependencias nuevas, secretos ni generados.

Logs: `/tmp/issue105-validation-final.log`, `/tmp/issue105-instrumented.log`.
Las pruebas HTTP usan MockWebServer exclusivamente en tests. Producción utiliza
el servicio remoto y la sesión real existentes, sin endpoints ni datos simulados.

## Límites y comprobación manual pendiente

No se ejecutó una mutación contra el backend desplegado con credenciales y datos
reales. Las verificaciones anteriores acreditan contrato HTTP local, lógica,
compilación y UI, no la disponibilidad del despliegue.

Para QA: iniciar como ADMIN, seleccionar un incidente conocido, comprobar sus
acciones, abrir/cancelar el diálogo, ingresar observación y confirmar. Contrastar
estado e historial en backend, repetir desde Resuelto y verificar Cerrado terminal.
Comprobar pérdida de conexión, 409 concurrente y fallo de GET posterior; no repetir
PATCH tras una confirmación exitosa. Cerrar sesión/iniciar como Ciudadano y
comprobar ausencia de accesos administrativos. Rotación y restauración de proceso
recargan desde backend; la edición del diálogo no se persiste tras muerte del proceso.

Rama destino del PR: integration. Directiva: Closes #105. Sin merge ni cierre manual.
