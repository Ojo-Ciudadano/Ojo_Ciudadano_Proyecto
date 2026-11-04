# Issue #104 — Detalle administrativo de incidentes Android

## A. Resumen y dependencias

Implementa la consulta de un incidente seleccionado desde el listado administrativo
de #103, con navegación por ID, contenido estructurado, carga, errores controlados,
reintento recuperable y regreso al listado. Conserva el modelo completo, su ID y
estado para futuras ampliaciones, sin implementar acciones administrativas.

Inspección inicial del 3 de noviembre de 2026:

- Árbol limpio; `feature/104-admin-incident-detail` ya existía en `7259cb4`, sin
  commits propios respecto de `integration`. Se preservó esa rama.
- Se ejecutó `git switch integration` y `git pull --ff-only origin integration`:
  ya estaba actualizada. Después se regresó a la rama existente de #104.
- PR #221 (#103): integrada mediante `8bc7ac8`, disponible en la base de trabajo.
  Su listado y callback de selección por ID se reutilizan.
- PR #220 (#162): abierta, no integrada. Sus operaciones de historial,
  asignación y prioridad no son necesarias para #104. No se copió código de esa rama.
- La consulta `getIncidentById` y sus modelos ya estaban disponibles. También se
  inspeccionaron, solo en lectura, el controlador, servicio y DTO de detalle de
  `web/backend/apps/incidentes`: el endpoint exige UUID y devuelve los campos
  comunes que ya consume Android. No falta una dependencia imprescindible.

## B. Archivos creados y modificados

Rutas relativas a `mobile/`:

| Archivo | Cambio y propósito |
| --- | --- |
| `app/src/main/java/cl/uct/ojociudadano/navigation/AppNavigation.kt` | Modificado: ruta de detalle administrativo, callback con ID codificado y regreso al listado. |
| `app/src/main/java/cl/uct/ojociudadano/navigation/RoleAccessPolicy.kt` | Modificado: permiso exclusivo ADMIN para el patrón y las rutas concretas de detalle. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsScreen.kt` | Modificado: conecta la selección existente; extrae el indicador de estado para compartirlo con el detalle. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailScreen.kt` | Creado: pantalla Compose, observación del ViewModel y secciones de información. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailViewModel.kt` | Creado: estado observable, validación de ID, carga, política de reintento y factory. |
| `app/src/test/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailViewModelTest.kt` | Creado: seis pruebas de consulta, estado, errores, concurrencia, conservación por propietario y cancelación. |
| `app/src/test/java/cl/uct/ojociudadano/navigation/RoleAccessPolicyTest.kt` | Modificado: permisos del nuevo patrón/rutas concretas y rechazo de rutas malformadas. |
| `app/src/test/java/cl/uct/ojociudadano/integration/AuthenticatedIncidentsFlowTest.kt` | Modificado: dos pruebas HTTP de detalle ADMIN, JWT, datos de otro reportante y errores de sesión. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/admin/AdminIncidentDetailScreenTest.kt` | Creado: cuatro pruebas de presentación, errores/reintento, campos vacíos y contenido extenso a 320 dp en tema oscuro. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsScreenTest.kt` | Modificado: actualiza el nombre de la prueba del callback ahora conectado. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/startup/RoleNavigationTest.kt` | Modificado: transporte/decodificación de argumento, regreso por cabecera y sistema, rechazo ciudadano e invalidación dentro del detalle. |
| `docs/issue-104-admin-incident-detail.md` | Creado: entrega, evidencia, límites de verificación y propuesta de PR sin publicar. |

## C. Arquitectura

`AdminIncidentsScreen` → `admin/incidents/{id}` → `AdminIncidentDetailScreen` →
`AdminIncidentDetailViewModel` → `IncidentRepository.getIncidentById(id)` →
`RemoteIncidentRepository` → `ApiService.getIncidentById(id)` →
`GET /api/v1/incidentes/{id}`.

No se cambian Repository, Retrofit, DTOs ni mappers. `AppContainer` sigue proporcionando
el repositorio remoto. El ViewModel pertenece a la entrada de navegación, carga en
`init` con `viewModelScope` y expone `StateFlow<AdminIncidentDetailState>`.
Recomponer no inicia consultas. Los argumentos restaurados permiten reconstruirlo
después de una muerte de proceso; los cambios de configuración conservan el mismo
ViewModel mientras exista su propietario. El reintento ignora solicitudes concurrentes
y la cancelación se propaga al eliminar el propietario.

El estado contiene `incidentId`, `incident` y `currentStatus` (derivado del modelo
consultado), además del estado visual y `canRetry`. No se transporta el incidente
mediante variables globales ni se utiliza el detalle ciudadano para esta navegación.
Se reutilizan `IncidentsUiState`, `incidentMessage()`, `incidentDate()`,
`IncidentLoadState`, `CitizenHeader` y `CitizenMessage`.

Se presentan título, categoría, ID y nombre real del estado; descripción sin
truncamiento; dirección, sector/comuna/región disponibles y coordenadas válidas del
modelo; creación y actualización disponible. Los campos vacíos usan un texto
descriptivo o se omiten. No se muestran IDs de categoría, ubicación, estado o reportante.
La UI usa tarjetas Material, desplazamiento vertical y colores del tema; el
indicador de estado conserva el aspecto del listado y no ofrece acciones de edición.

Política de errores:

| Caso | Presentación | Reintento |
| --- | --- | --- |
| ID ausente o malformado | Mensaje para regresar al listado; sin consulta HTTP | No |
| Conexión o timeout | Mensaje seguro existente de conectividad | Sí |
| HTTP 408 / 429 / 5xx | Mensaje HTTP existente sin diagnósticos internos | Sí |
| HTTP 401 | Sesión expirada; invalidación centralizada existente | No |
| HTTP 403 | Consulta no autorizada | No |
| HTTP 404 | Incidente no disponible | No |
| Otros 4xx, respuesta no interpretable o sesión ausente/cambiada | Mensaje seguro existente | No |

## D. Integración con #103

La tarjeta conserva `incident.id` y lo entrega por `onIncidentSelected`.
`AppNavigation` aplica `Uri.encode` y navega con `navigateIfAllowed`.
El destino recupera `entry.arguments["id"]` y lo entrega a la factory del ViewModel.
El ID se valida como UUID canónico antes de consultar, conservando el valor recibido.
El botón de cabecera vuelve a la entrada existente de `ADMIN_INCIDENTS`; si el
detalle se abrió directamente y no existe esa entrada, abre el listado autorizado.
Atrás del sistema mantiene el comportamiento de la pila de Navigation Compose.
La instancia del ViewModel del listado se conserva al volver por la pila.

## E. Seguridad y alcance

`RoleAccessPolicy` autoriza únicamente ADMIN para el nuevo destino. Se conserva
`protectedComposable`: valida antes de componer pantalla/ViewModel, incluso si
un llamador elude el guard de la acción o restaura una entrada restringida.
`SessionStartupGate` mantiene la eliminación del flujo al cambiar sesión, cuenta o rol.
El repositorio exige identidad y comprueba la sesión después de la consulta;
`BearerTokenInterceptor` adjunta JWT e invalida únicamente el token correspondiente
ante 401. No se agregan tokens, credenciales, logs ni lógica paralela de autorización.
No hay enlaces profundos externos nuevos.

Solo se modifica `mobile/`. Se excluyen cambios de estado, historial, responsables,
prioridad, fotografías, evidencias, mapas y edición. No hay dependencias nuevas ni
cambios de API/backend/web/infraestructura/CI. Sin push ni publicación de PR.

## F. Verificaciones ejecutadas

Desde `mobile/`:

```bash
bash ./gradlew :app:compileDebugKotlin --console=plain
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
bash ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.admin.AdminIncidentDetailScreenTest,cl.uct.ojociudadano.ui.admin.AdminIncidentsScreenTest,cl.uct.ojociudadano.ui.startup.RoleNavigationTest,cl.uct.ojociudadano.ui.reports.IncidentBrowsingTest --console=plain
bash ./gradlew :app:lintDebug --console=plain
git diff --check
```

- Compilación debug, APK y compilación de pruebas instrumentadas: correctas.
- Suite unitaria: 307 casos, 0 fallos/errores, 3 integraciones remotas omitidas.
  Incluye seis pruebas nuevas del ViewModel y dos HTTP autenticadas con MockWebServer.
- Suite instrumentada: 21/21 correctas, sin omitidas, en Motorola AVD Android 17.
  Incluye cuatro casos del detalle, cuatro del listado, once de navegación por rol
  y dos de regresión del listado ciudadano.
- Lint debug: sin errores, 25 advertencias. `git diff --check`: correcto.
- Logs: `/tmp/issue104-validation.log` y `/tmp/issue104-instrumented.log`.
  Informes generados bajo `app/build/reports/`.

Las pruebas HTTP utilizan el contrato existente mediante Retrofit y MockWebServer,
sin servidor desplegado. Las pruebas de navegación verifican el argumento codificado
con un ID inválido rechazado localmente; las de selección verifican por separado el
ID real de la tarjeta, y las del ViewModel verifican la consulta con UUID real de prueba.
No se ejecutó un recorrido completo con una sesión ADMIN y datos del backend real.
Tampoco se probó muerte real de proceso, rotación física, TalkBack o fuente ampliada.
No se afirma haber comprobado esos escenarios.

### Criterios de aceptación y evidencia

| Criterio implementado | Evidencia ejecutada |
| --- | --- |
| Selección desde el listado y transporte del ID | Callback instrumentado y argumento/decodificación de Navigation. |
| Consulta mediante Repository y contrato existente | ViewModel unitario y pruebas HTTP autenticadas de detalle ADMIN. |
| Datos, estado, ID y modelo completo disponibles | Pruebas de estado y contenido Compose. |
| Carga, contenido y error controlado | Pruebas unitarias e instrumentadas del detalle. |
| Reintento solo recuperable, sin consultas duplicadas | Matriz unitaria de errores y prueba Compose de reintento. |
| Regreso al listado | Cabecera y Atrás del sistema en Navigation conservan la misma entrada. |
| Restricción administrativa y sesión | Política unitaria, navegación ciudadana rechazada incluso sin guard e invalidación en el detalle. |
| Flujo ciudadano conservado | Suite unitaria completa y dos pruebas instrumentadas de navegación de reportes ciudadanos. |
| UI sin Retrofit, cambios solo mobile y alcance #104 | Revisión del diff y cadena de dependencias descrita arriba. |

### Prueba manual pendiente con backend real

1. Ejecutar debug con el Gateway configurado e iniciar sesión con una cuenta ADMIN
   autorizada. Abrir **Gestión de incidentes** y seleccionar un incidente conocido.
2. Comparar ID, categoría, título, descripción, estado, fechas y ubicación con los
   datos de ese incidente en el entorno. Probar también otro incidente del listado.
3. Volver con la cabecera y con Atrás del sistema: debe conservarse el listado.
4. Girar el dispositivo y revisar Network Inspector: recomponer o rotar no debe
   volver a consultar; tras matar/restaurar el proceso debe recuperarse el ID y consultar.
5. Desconectar antes de abrir el detalle, comprobar el error y reactivar conexión:
   **Reintentar** debe recuperar el mismo incidente. En QA, contrastar 404/403/401/5xx;
   401 debe usar el cierre centralizado existente y los errores terminales no ofrecen reintento.
6. Cambiar a CIUDADANO y comprobar ausencia de accesos administrativos. Revisar
   fuente ampliada, TalkBack, orientación horizontal y ambos temas.

## G. Commits

Los hashes y mensajes finales se entregan en el informe de la sesión. Los cambios
se agrupan en implementación y pruebas/documentación, con mensajes en español y
selección explícita de archivos; sin `git add .`, push ni PR.

## H. Propuesta de Pull Request sin publicar

**Título:** `feat(android): implementar detalle administrativo de incidentes - #104`

**Rama origen:** `feature/104-admin-incident-detail`

**Rama destino:** `integration`

**Descripción preparada:**

```markdown
Al seleccionar un incidente del listado administrativo de #103, se abre su detalle
de consulta con el ID real y GET /api/v1/incidentes/{id}, usando el Repository y JWT
existentes. La cabecera y Atrás del sistema permiten volver al listado.

Cambios:
- Ruta administrativa protegida exclusivamente para ADMIN y selección conectada al listado.
- ViewModel con StateFlow, validación de UUID, modelo/ID/estado disponibles, carga,
  errores seguros y reintento para conectividad, timeout, 408/429 y 5xx.
- Tarjetas con identificación, estado real, descripción completa, ubicación y fechas,
  reutilizando cabecera, mensajes, tema y presentación de estado existentes.
- Pruebas de ViewModel, contrato HTTP autenticado, presentación y navegación por rol.

Integración: #103 está disponible mediante la PR #221. La PR #220 (#162) permanece
abierta y no es necesaria para la consulta existente; no se incorporó código suyo.

Verificación: compilación debug/APK/pruebas correctas; 307 casos unitarios sin fallos
(3 integraciones remotas omitidas); 21/21 instrumentados en Android 17, incluidas
regresiones ciudadanas; lint sin errores (25 advertencias); diff sin errores de formato.
Las pruebas HTTP usan MockWebServer. Falta contrastar el recorrido con sesión ADMIN
y datos del backend desplegado; no se afirma prueba real de rotación/muerte de proceso.

Prueba manual: iniciar como ADMIN → Gestión de incidentes → seleccionar un incidente;
contrastar datos y estado con el entorno; volver por cabecera/sistema; comprobar error
sin conexión y reintento al recuperarla. Ver pasos y evidencia en
mobile/docs/issue-104-admin-incident-detail.md.

Solo cambia mobile/. Sin contratos ni dependencias nuevas; sin backend, web,
infraestructura o CI/CD; sin cambio de estado, historial, asignaciones, prioridad,
edición, mapas o nuevas funciones de evidencias.

Closes #104
```
