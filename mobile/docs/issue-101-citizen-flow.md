# Issue #101 — Auditoría e integración del flujo Ciudadano

## A. Auditoría inicial

Issue: https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/101

Base auditada: `integration` y `origin/integration`, commit `2d02c95`.
La rama local `feature/101-citizen-flow` ya existía en ese mismo commit, sin
modificaciones locales. Se ejecutaron `git status --short --branch`,
`git branch -avv`, `git fetch origin` y
`git rev-list --left-right --count HEAD...origin/integration` (resultado `0 0`).
No fue necesario cambiar, actualizar ni modificar `integration`. No existía una
rama remota `origin/feature/101-citizen-flow` después del fetch.

Los archivos de esta tabla son relativos a
`app/src/main/java/cl/uct/ojociudadano/`, salvo indicación contraria.
La auditoría se hizo antes de modificar código; los estados describen esa base.

| Funcionalidad | Estado inicial | Archivos involucrados | Problema detectado | Acción necesaria |
|---|---|---|---|---|
| Login y sesión (#94–#96) | Implementada | `ui/login/AuthViewModel.kt`, `data/repository/RemoteAuthRepository.kt`, `data/session/SessionManager.kt` | Sin sistema paralelo ni usuario temporal en producción | Reutilizar |
| Persistencia y logout | Implementada | `data/session/EncryptedSharedPreferencesSessionStorage.kt`, `ui/startup/SessionStartupGate.kt`, `ui/profile/ProfileScreen.kt` | Persistencia cifrada, descarte de sesiones inválidas y cambio de navegación existentes | Ejecutar regresiones |
| JWT (#95) | Implementada | `data/remote/auth/BearerTokenInterceptor.kt`, `data/remote/RetrofitClient.kt`, `data/session/SessionIdentity.kt` | El snapshot evita mezclar identidad antigua y JWT nuevo; 401 invalida solamente la sesión emisora | Conservar |
| Destino inicial (#98) | Implementada | `navigation/InitialDestination.kt`, `ui/startup/SessionStartupGate.kt` | Ciudadano, Administrador y roles ambiguos tienen destinos separados | Conservar y probar |
| Rutas protegidas (#99) | Implementada | `navigation/RoleAccessPolicy.kt`, `navigation/ProtectedNavigation.kt` | Valida callback y composición del destino, además de ocultar elementos | Conservar y probar navegación directa |
| Inicio Ciudadano | Implementada | `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt` | Resume incidentes propios, abre formulario/lista/detalle y recarga al volver | Conservar |
| Todos los reportes | Parcial | `ui/reports/ReportsScreen.kt`, `ui/reports/ReportsViewModel.kt` | Ya consulta lista general; faltan actualización explícita y recarga al volver a una entrada conservada | Conectar actualización |
| Mis reportes (#96) | Implementada | `data/repository/RemoteIncidentRepository.kt`, `ui/reports/ReportsViewModel.kt` | Envía `usuarioReportaId` de sesión como filtro al servidor y filtra respuesta como defensa adicional | Conservar; no considerarlo autorización del backend |
| Detalle Ciudadano | Implementada | `ui/reports/IncidentDetailScreen.kt`, `ui/reports/IncidentDetailViewModel.kt` | Consulta por ID, muestra estado, descripción, ubicación y evidencias; sin acciones administrativas | Reutilizar desde creación |
| Crear incidente | Parcial | `ui/report/CreateReportScreen.kt`, `CreateReportViewModel.kt`, `CreateReportFormValidator.kt` | Ya usa identidad real y valida campos; la confirmación no ofrece abrir el incidente creado | Conectar confirmación y detalle |
| GPS y permisos (#83) | Implementada | `data/location/DefaultLocationClient.kt`, `ui/permission/LocationPermissionLauncher.kt`, `ui/report/CreateReportViewModel.kt` | Coordenadas del dispositivo, permisos, ajustes y timeout de 15 segundos existentes | Reutilizar sin mapas nuevos |
| Referencias de ubicación | Parcial | `ui/report/CreateReportViewModel.kt`, `data/model/CreateIncidentInput.kt` | Dirección, comuna, región y sector se enviaban como textos provisionales, aunque el DTO exige los cuatro campos | Solicitar y validar datos reales del lugar |
| Cámara, galería y archivos (#88–#91) | Implementada | `ui/report/photo/`, `ui/report/EvidencePhotoSection.kt`, `data/media/CacheEvidencePhotoFileStore.kt` | Archivos temporales, tokens de operaciones, vista previa, eliminación y límites existentes | Conservar |
| Creación con/sin fotografías (#91) | Implementada | `ui/report/CreateReportViewModel.kt`, `CreateReportSubmissionState.kt` | Primero crea incidente; luego sube evidencias. Distingue pendientes confirmadas e inciertas; el reintento no recrea el incidente | Conservar y probar |
| Errores y reintentos | Parcial | `ui/reports/IncidentErrors.kt`, `ui/report/CreateReportViewModel.kt`, `CreateReportScreen.kt` | Listas controlan errores; creación/categorías muestran excepciones crudas; falta botón para recargar categorías | Mensajes controlados y reintento |
| Loading y vacío | Implementada | `ui/components/LoadingContent.kt`, `ui/reports/IncidentComponents.kt`, `ReportsScreen.kt` | Indicadores centrados y mensajes vacíos existentes | Conservar |
| Operaciones administrativas | Parcial | `data/repository/RemoteIncidentRepository.kt` | Solo cambio de estado comprobaba rol localmente; prioridad, asignación, candidatos e historial dependían de los guardas de UI/servidor | Reutilizar `withCurrentIdentity(requiredRole = ADMIN)` |
| Autorización backend (#100) | Parcial | Gateway y microservicio, revisados en lectura | Rol vigente validado; no se observó vinculación de autor del cuerpo al JWT ni propiedad al subir evidencia | Documentar; no modificar backend |

No se crearon pantallas, DTOs, repositorios, APIs ni dependencias nuevas.
La arquitectura efectiva del módulo mantiene los repositorios existentes en
`data/repository` y los estados en ViewModels; no se introdujeron use cases
duplicados ni llamadas Retrofit desde Composables.

## B. Cambios realizados

| Archivo modificado | Cambio y justificación |
|---|---|
| `ui/report/CreateReportFormState.kt` | Conserva dirección de referencia, comuna, región, sector y error de validación durante la vida del formulario |
| `ui/report/CreateReportFormValidator.kt` | Impide enviar referencias vacías o compuestas solo por espacios |
| `ui/report/CreateReportViewModel.kt` | Recibe referencias reales, elimina espacios exteriores, conserva coordenadas GPS y reemplaza errores crudos de creación/categorías por mensajes controlados. Elimina log temporal de diagnóstico |
| `ui/report/CreateReportScreen.kt` | Campos requeridos del lugar, reintento de categorías y botón «Ver reporte creado» solo al finalizar; conserva advertencias de éxito parcial |
| `navigation/AppNavigation.kt` | Abre detalle por el ID retornado y elimina la entrada del formulario finalizado, utilizando navegación protegida existente |
| `ui/reports/ReportsScreen.kt` | Actualización explícita y recarga al entrar/regresar; conserva alcance y filtros del ViewModel |
| `data/repository/RemoteIncidentRepository.kt` | Extiende la comprobación administrativa existente a historial, asignación, prioridad y candidatos; dependencia compartida imprescindible para impedir invocaciones internas del Ciudadano |
| `app/src/test/.../ui/report/CreateReportViewModelTest.kt` | Valida datos del lugar, mensajes de 400/401/403/503, recuperación de categorías; adapta casos existentes a los campos obligatorios |
| `app/src/test/.../ui/report/CreateReportFormValidatorTest.kt` | Comprueba cada referencia faltante, incluidos espacios |
| `app/src/test/.../integration/AuthenticatedIncidentsFlowTest.kt` | Ciudadano no emite solicitudes administrativas; Administrador conserva acceso con JWT |
| `app/src/test/.../integration/EvidenceUploadFlowTest.kt` | Integra creación sin fotos, consulta propia y detalle con repositorio/Retrofit reales contra MockWebServer; actualiza creación con fotos |
| `app/src/test/.../data/repository/RemoteRepositoryTest.kt` | El caso de prioridad establece una sesión Administrador acorde a su operación |
| `app/src/androidTest/.../ui/report/CreateReportEvidenceRetryTest.kt` | Verifica abrir reporte sin reenviar y reintentar categorías, además de reintentos de evidencias existentes |
| `app/src/androidTest/.../ui/reports/IncidentBrowsingTest.kt` | Comprueba actualización de una lista que inicialmente estaba vacía |
| `README.md` | Actualiza descripción del flujo y enlaza este informe |

Archivos creados: este informe y `docs/issue-101-pr.md`, borrador local de PR.
No se modificaron pantallas administrativas ni archivos fuera de `mobile`.

## C. Criterios de aceptación de la Issue

«Cumplido» en esta tabla significa implementación y evidencia automatizada local;
no significa validación E2E contra el despliegue real. Esta última sigue pendiente.

| Criterio | Estado | Evidencia / límite |
|---|---|---|
| Ciudadano autenticado ingresa al flujo | Cumplido | `InitialDestinationTest`, `RoleNavigationTest`, `SessionStartupGateTest` |
| Identidad real de sesión | Cumplido | `AuthenticatedIncidentsFlowTest`: query, cuerpo, restauración y cambio de cuenta |
| Operaciones protegidas con JWT real de sesión | Cumplido | `BearerTokenInterceptorTest`, `AuthenticatedIncidentsFlowTest`, `EvidenceUploadFlowTest`; JWT de prueba en servidor local, sin credenciales remotas |
| Consultar incidentes generales | Cumplido | `ReportsViewModelTest`, `IncidentBrowsingTest`, repositorio y endpoint existentes |
| Consultar reportes propios | Cumplido | Query con UUID autenticado y descarte de autores ajenos, cubiertos en integración local |
| Crear incidente | Cumplido | `CreateReportViewModelTest`, `EvidenceUploadFlowTest`; datos reales requeridos, identidad fuera de la UI |
| Ubicación GPS de #83 | Parcialmente cumplido | Cliente real conservado; pruebas con `LocationClient` sustituido cubren denegación/no disponible. Falta recorrido en dispositivo con GPS físico |
| Fotografías de #91 | Parcialmente cumplido | ViewModel, HTTP multipart y estados parciales probados localmente; falta carga contra almacenamiento remoto real |
| Flujo sin fotografías | Cumplido | Creación → consulta propia → detalle mediante Retrofit y MockWebServer |
| Loading, éxito, vacío y error | Cumplido | Pruebas unitarias y Compose; 401, 403, 5xx y conexión cubiertos |
| Ciudadano sin funciones Administrador | Cumplido | Política y navegación interna protegida; prueba adicional deniega llamadas administrativas en repositorio sin HTTP |
| Operaciones respetan autorización backend | Parcialmente cumplido | Android usa contratos y JWT existentes; falta comprobación remota y quedan riesgos de identidad/propiedad del servidor descritos abajo |
| Rama `feature/101-citizen-flow` | Cumplido | Git local; commits preparados en la rama solicitada |
| PR hacia `integration` | No cumplido | No publicado: instrucción explícita de no hacer push y riesgos pendientes |
| PR vinculado con `Closes #101` | No cumplido | Texto preparado solamente en `docs/issue-101-pr.md` |

No se declara cerrada ni completada la Issue #101.

## D. Pruebas

El proyecto Gradle es `ojoCiudadano` y su módulo es `:app`, definido por
`settings.gradle.kts`; `:mobile` no es un subproyecto Gradle.

Validación inicial, antes de cambios:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
```

Resultado: `BUILD SUCCESSFUL` (47 segundos).
Log de sesión: `/tmp/issue101-baseline.log`.
El primer intento dentro del sandbox no pudo crear el lock de `~/.gradle`;
se repitió con el acceso requerido al SDK/caché y pasó.

Validación final: mismo comando; log `/tmp/issue101-final-validation.log`.
Resultado: `BUILD SUCCESSFUL` (49 segundos). Los XML de
`app/build/test-results/testDebugUnitTest/` registran 437 casos: 434 aprobados,
3 omitidos, cero fallos y cero errores. Las tres omisiones corresponden a
`RealIncidentCreationIntegrationTest`, `RealIncidentRepositoryIntegrationTest`
y `RealCategoryRepositoryIntegrationTest`, por sus condiciones de entorno.
Lint final: cero errores y 36 advertencias; APK debug y pruebas instrumentadas
compilados correctamente. No se actualizaron dependencias fuera de alcance.

En una ejecución intermedia fallaron dos pruebas nuevas al compilar por omitir
su parámetro `categoryRepository`. Se corrigieron los fixtures. La ejecución
posterior pasó. No era un fallo de la base ni de producción.

Pruebas instrumentadas seleccionadas:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.startup.RoleNavigationTest,cl.uct.ojociudadano.ui.startup.SessionStartupGateTest,cl.uct.ojociudadano.ui.reports.IncidentBrowsingTest,cl.uct.ojociudadano.ui.reports.CitizenPresentationTest,cl.uct.ojociudadano.ui.report.CreateReportEvidenceRetryTest,cl.uct.ojociudadano.ui.report.EvidencePhotoSectionTest,cl.uct.ojociudadano.ui.admin.AdminIncidentsScreenTest,cl.uct.ojociudadano.ui.admin.AdminIncidentDetailScreenTest \
  --console=plain
```

Dispositivo: `emulator-5554`, Motorola AVD, Android 17 (API 37).
Log: `/tmp/issue101-instrumented.log`.
Resultado: `BUILD SUCCESSFUL` (2 minutos 20 segundos), 64 pruebas aprobadas,
cero fallos, errores u omisiones. Evidencia XML:
`app/build/outputs/androidTest-results/connected/debug/TEST-Motorola(AVD) - 17-_app-.xml`.
Se ejecutó `git diff --check` sin problemas.

La primera consulta ADB dentro del sandbox no pudo abrir su socket. Fuera del
sandbox, inicialmente no había dispositivos listados; al intentar arrancar el
AVD se informó una instancia existente. Se usó la instancia disponible
`emulator-5554`; no se forzó otro AVD ni se detuvieron dispositivos ajenos.

Cobertura reutilizada: sesión ausente/restaurando/expirada, rol distinto,
listados exitosos/vacíos y errores, asociaciones de autor, creación con/sin
fotografías, GPS denegado/no disponible, campos inválidos, errores de creación y
evidencias, progreso, envíos duplicados, reintentos inciertos, navegación interna
protegida y regresiones administrativas.

### Recorridos E2E solicitados

| Escenario | Automatización ejecutada | Validación manual / remota |
|---|---|---|
| 1. Login → rol → inicio | Integración de autenticación y pruebas Compose de navegación | Pendiente con cuenta Ciudadano del despliegue |
| 2. Inicio → todos → detalle → regreso | Navegación protegida, listado/callback Compose y detalle de repositorio | Pendiente recorrido completo con backend real |
| 3. Inicio → propios → detalle | Identidad/query/resultados mediante MockWebServer y UI de alcance | Pendiente confirmar cuenta y datos reales |
| 4. Crear sin fotos → confirmación | ViewModel + Retrofit + MockWebServer + consulta posterior propia/detalle; UI de confirmación | Pendiente GPS físico y escritura real |
| 5. Crear con fotos → evidencias | Integración HTTP multipart y tests de vista previa/reintentos/éxito parcial | Pendiente galería/cámara del dispositivo y almacenamiento remoto |
| 6. Intentar funciones Admin | Pruebas de navegación directa y rechazo en repositorio | No se probaron peticiones maliciosas contra producción |
| 7. Navegar → persistir → logout | Tests de sesión, restauración, navegación y limpieza del back stack | Pendiente recorrido manual con cuenta real |

No hubo validación manual E2E ni GPS físico ni subida a almacenamiento remoto.
`AUTH_EMAIL`, `AUTH_PASSWORD`, `RUN_API_INTEGRATION_TESTS` y
`RUN_WRITE_INTEGRATION_TESTS` no estaban configuradas. Las pruebas remotas
optativas se omiten por sus condiciones existentes; no se consideran aprobadas.
No se crearon registros ficticios en producción.

## E. Riesgos y dependencias externas

La revisión del backend fue exclusivamente de lectura, en la misma base
`2d02c95`. Los siguientes hallazgos son evidencia de código, no reproducciones
remotas contra la versión desplegada:

1. **Identidad del autor en creación.**
   `web/backend/apps/incidentes/src/dto/create-incidente.dto.ts:106` exige
   `usuarioReportaId`; `incidentes.service.ts:130–164` lo guarda y lo usa en el
   historial. El controlador del Gateway reenvía la petición y `RolesGuard`
   comprueba identidad/rol vigente, pero no vincula ese campo con `user.sub`.
   Android envía únicamente el UUID de su sesión; esto no impide suplantación
   desde un cliente modificado. Requiere revisión/corrección del backend aparte.
2. **Propiedad al adjuntar evidencias.**
   `web/backend/apps/incidentes/src/evidencias/evidencias.service.ts:64–97`
   comprueba que el incidente exista, sin recibir identidad del solicitante.
   El Gateway permite las rutas generales a roles CIUDADANO/ADMIN. No se observó
   comprobación de propiedad para impedir adjuntar evidencias a un incidente
   ajeno. Android solo sube al ID recién creado, pero eso no es autorización
   suficiente para otros clientes.
3. **Consulta propia y consulta general.**
   `incidentes.service.ts:50–55` aplica el filtro de autor recibido. Android lo
   envía al servidor y además filtra la respuesta. El listado general y detalle
   permiten consultar incidentes de otros autores por el alcance de la Issue;
   no se afirma que recibir esos incidentes constituya por sí mismo una fuga.
   Si el despliegue debiera ocultar otros datos, sus políticas deben validarse
   del lado servidor; el filtrado local no protege los datos ya recibidos.
4. **Roles de sesión.** El servidor consulta roles actuales en `/usuarios/me`;
   Android conserva roles del login hasta iniciar otra sesión. Un cambio remoto
   de rol puede devolver 403; no debe interpretarse como lista vacía ni logout
   automático. 401 sigue invalidando la sesión mediante el interceptor existente.
5. **Respuestas inciertas al escribir.** La API no incorpora una clave de
   idempotencia para crear incidentes. Se conserva el bloqueo de envíos en curso
   y posteriores al éxito. Un timeout de creación puede ocultar una escritura
   ya aplicada; antes de repetir debe verificarse «Mis reportes». Para evidencias
   ya existe confirmación explícita de reintento incierto y se reutiliza.
6. **GPS y persistencia del formulario.** Se conserva el comportamiento de #83,
   que acepta última ubicación disponible del proveedor y tiene timeout. La
   precisión/antigüedad y los permisos deben validarse en dispositivo real.
   Las selecciones y referencias viven en el ViewModel durante el formulario;
   no se añadió restauración del borrador tras muerte del proceso.

Estos riesgos impiden declarar verificada la seguridad E2E del flujo completo.
No se alteraron backend, infraestructura, base de datos ni reglas administrativas.

## F. Git y Pull Request

Commit de implementación y pruebas: `1b93f4a`
(`feat(mobile): completar integracion del flujo Ciudadano #101`).
El informe y el borrador se registran en un segundo commit de documentación.

Todo el trabajo queda en `feature/101-citizen-flow`. No se ejecutó push, no se
creó un PR y no se hizo merge. El título y la descripción para una futura revisión
se conservan en `docs/issue-101-pr.md`; la base propuesta es `integration`.
No debe publicarse como cierre verificado hasta resolver los riesgos críticos
de autor/propiedad y completar validación con el despliegue real.
