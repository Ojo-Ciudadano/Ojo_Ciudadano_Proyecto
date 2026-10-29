# Interfaz ciudadana e incidentes Android

Fecha de verificación: 28 de octubre de 2026 (America/Santiago).
Rama: `feature/202-android-user-registration`. Base anterior conservada: `e50efd7`.
Solo se modificó `mobile/`. Sin push, PR, merge ni cambios de backend.

## Diagnóstico

La arquitectura sigue siendo Compose → ViewModel → Repository → Retrofit → API Gateway.
`AppContainer` sigue proporcionando los repositorios y el `SessionManager` existentes.
No se cambiaron el JWT, su persistencia, invalidación por 401, restauración ni cierre de sesión.

En Inicio, `HomeScreen` dependía de `HomeViewModel.incidents`, inicialmente vacío.
`HomeViewModel.loadIncidents()` capturaba excepciones y únicamente las registraba;
la UI no podía distinguir una lista vacía de un fallo y mostraba ausencia de reportes.
Esto explica el falso vacío de Inicio desde el código. Sin las imágenes adjuntas no
se puede identificar de forma concluyente cuál de las pantallas aparecía en la captura.
El texto literal «No hay incidentes» también existía en `ReportsScreen`; su ViewModel
ya tenía estados de carga, vacío y error, pero el renderizado sustituía toda la
pantalla y ocultaba la cabecera, filtros y navegación durante esos estados.

## Contratos existentes utilizados

Todos los paths siguientes pasan por la base configurada de Retrofit. No se agregó
ninguna ruta HTTP. La ruta `incident/{id}` es exclusivamente navegación local Android.

| Operación | Endpoint | Alcance |
| --- | --- | --- |
| Todos | `GET /api/v1/incidentes` | Sin filtro de reportante, JWT existente |
| Propios | `GET /api/v1/incidentes?usuarioReportaId={session.userId}` | Usuario real autenticado |
| Detalle | `GET /api/v1/incidentes/{id}` | Consulta autorizada por el servidor |
| Filtros | `GET /api/v1/categorias` | Categorías existentes |
| Evidencias | `GET /api/v1/incidentes/{id}/evidencias` | Metadata autorizada |
| Fotografía | `GET /api/v1/evidencias/{id}/archivo` | Descarga autenticada mediante Repository |

Se mantienen `POST /api/v1/auth/login`, `POST /api/v1/auth/register` y el flujo existente
`POST /api/v1/incidentes`, incluida la subida existente de fotografías.

La inspección de solo lectura de `incidentes.controller.ts`, `incidentes.service.ts`
y el proxy del Gateway encontró lectura general y por ID protegida por JWT, sin
requisito de rol Administrador; el filtro por reportante es opcional. Solo la
modificación de prioridad está restringida a ADMIN. El contrato inspeccionado no
contiene una bandera de privacidad o un endpoint público anónimo: «Todos» significa
los reportes que el servidor autoriza a esta sesión, no acceso sin autenticación.

## Resultado funcional

- Pestañas «Todos los incidentes» y «Mis incidentes», disponibles también durante
  carga, vacío y error. La selección inicial es «Mis incidentes», coherente con
  «Mis reportes recientes» del Inicio.
- «Ver todos» abre el listado; cada tarjeta abre el detalle del ID seleccionado.
- Se conservan filtros de categoría y fecha. Fechas inválidas no causan un cierre.
- Cambiar de alcance cancela la carga pendiente y limpia filas anteriores.
- Los reportes propios usan `SessionManager.requireIdentity()`, el mismo ID y token
  de sesión que las operaciones existentes. Se verifica la sesión tras la respuesta
  y se filtran de nuevo los resultados por `reporterUserId == identity.userId`,
  incluso si el servidor omite por error el filtro. No hay IDs temporales.
- HTTP 401, 403, 404, 5xx, conectividad/TLS y datos no interpretables se presentan
  como errores. Solo una consulta exitosa sin filas produce el estado vacío.
  Los mensajes no muestran diagnósticos internos del backend.
- Tarjetas y detalle presentan título, categoría, descripción, ubicación, fecha,
  estado y la primera evidencia de imagen disponible. No muestran ID, nombre,
  correo ni teléfono del reportante.
- Evidencias se obtienen por Repository y Retrofit; no hay HTTP en Compose,
  tokens en URLs ni caché pública de fotografías. Se limita la lectura a 10 MiB
  y se reduce la resolución decodificada. Los errores de imagen tienen reintento
  independiente y no convierten una lista válida en vacía.
- Inicio saluda con el nombre de sesión y muestra hasta tres reportes propios
  recientes. Las estadísticas corresponden únicamente a los incidentes propios:
  «Nuevo» y «En revisión» → En revisión; «En atención» → En atención;
  «Resuelto» y «Cerrado» → Resueltos, según los estados del seed inspeccionado.
  En carga/error se muestra «—»; cero solo después de una respuesta exitosa.
- Perfil muestra datos de la sesión real. Las rutas y el cierre de sesión por rol
  mantienen el flujo previo; no se agregó acceso ciudadano a acciones administrativas.

## Diseño y movimiento

Inicio, listado, detalle, login, registro, formulario de reporte, perfil y navegación
inferior comparten la paleta de acciones rojas, azul marino y superficies claras.
Hay cabecera reutilizable, tarjetas redondeadas, elevación suave, jerarquía Material 3
y mensajes de estado reutilizables. El tema también conserva modo oscuro.

Login/registro utilizan un contenedor compartido con entrada `AnimatedVisibility`
(fade de 180 ms), `AnimatedContent` para estados de autenticación y
`animateContentSize` para validaciones. El formulario de reporte también ajusta
su tamaño mediante animación. Navigation Compose usa fades de entrada/salida de
180/120 ms, incluyendo regreso. Son animaciones nativas que utilizan la escala de
animación del sistema; no hay bucles ni retardos artificiales.

Se mantienen el formulario desplazable, manejo del teclado, etiquetas y acciones
de accesibilidad, validaciones, mostrar/ocultar contraseña y regreso al login.
La confirmación de registro aparece en el login al retornar exitosamente.
No se implementó recuperación de contraseña.

La pantalla de mapa de esta rama es un placeholder (`MapScreen`), no una integración
cartográfica. Se conservó y hereda el tema; Inicio permite abrir esa ruta. No se
inventaron coordenadas ni se agregó otro proveedor de mapas.

## Verificación ejecutada

Comandos desde `mobile/`:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.reports.IncidentBrowsingTest,cl.uct.ojociudadano.ui.reports.CitizenPresentationTest
```

- Suite unitaria final: 231 casos, 0 fallos/errores; 3 integraciones remotas omitidas.
- Suite instrumentada previa a añadir los nuevos casos: 67 casos, 0 fallos;
  1 integración remota omitida.
- Cinco nuevos casos instrumentados: 5/5 correctos. Después del ajuste final de
  superficies se repitieron con `adb shell am instrument`: 5/5 correctos.
- `assembleDebug`, compilación de pruebas y lint correctos; lint conserva
  advertencias no bloqueantes, principalmente preexistentes.
- Cobertura: autenticación, registro, validaciones, restauración, logout y roles
  existentes; listado correcto/vacío/error, 401/403/500/503, conectividad, serialización,
  filtrado defensivo de propietarios, detalle por ID, cambio de pestañas, cancelación
  de respuestas pendientes, filtros y fechas inválidas.
- En emulador Motorola Android 17, 1080×2400 y densidad 420, se probaron formularios
  y dashboard en un viewport de 320 dp con teclado y desplazamiento. Se inspeccionaron
  capturas de Inicio, login y registro y se corrigieron las superficies lavanda
  predeterminadas. No se observaron cierres inesperados durante las suites.
- Las imágenes de referencia del usuario no estaban adjuntas. Se siguió la descripción
  visual; no se afirma comparación píxel a píxel.
- El conjunto de pruebas usa datos ficticios únicamente en `test` y `androidTest`.
  Producción no contiene reportes simulados ni contadores de ejemplo.

## Bloqueos y límites de la verificación

La consulta HTTPS al Gateway configurado, desde este entorno, falla con
`curl: (60) SSL certificate problem: self-signed certificate`, incluso fuera del
sandbox. No se obtuvo estado HTTP ni JSON. Se mantuvo la validación TLS.

No es posible confirmar en esta ejecución la cantidad real de incidentes, la
consulta remota de otros ciudadanos, el contrato efectivamente desplegado, login,
registro o descarga remota de fotografías. Las integraciones remotas no se declaran
exitosas. Los flujos están verificados con repositorios/servidores de prueba.

El contrato local permite consultar otros ciudadanos con JWT y Android lo utiliza,
pero la comprobación real queda pendiente de una conexión TLS válida y una sesión
de prueba autorizada. No hay evidencia suficiente para atribuir el bloqueo actual a
Supabase, una respuesta vacía o un HTTP 500/503; esos estados sí están cubiertos en pruebas.
Infraestructura debe revisar la cadena/certificado; no se modificó infraestructura.

El mapa funcional sigue pendiente de la implementación correspondiente de los
compañeros. No había infraestructura cartográfica en esta rama para reutilizar.

## Commits locales

- `6b6a16d` fix(android): distinguish incident failures and protect own report scope
- `e355500` feat(android): add citizen incident tabs, details and personal dashboard
- `6de5fa8` style(android): modernize authentication theme and native transitions
- `83ddf3f` style(android): unify citizen navigation, profile and report form
- `bf21879` style(android): refine neutral surfaces after emulator review
- `25985b8` test(android): cover incident scopes, errors and compact citizen screens

Este informe se guarda en un commit adicional de documentación.

## Archivos modificados

Rutas relativas a `mobile/` (incluyen archivos nuevos):

- `.gitignore`
- `app/src/androidTest/java/cl/uct/ojociudadano/ui/registration/RegistrationNavigationTest.kt`
- `app/src/androidTest/java/cl/uct/ojociudadano/ui/reports/CitizenPresentationTest.kt`
- `app/src/androidTest/java/cl/uct/ojociudadano/ui/reports/IncidentBrowsingTest.kt`
- `app/src/androidTest/java/cl/uct/ojociudadano/ui/startup/RoleNavigationTest.kt`
- `app/src/main/java/cl/uct/ojociudadano/data/repository/RemoteIncidentRepository.kt`
- `app/src/main/java/cl/uct/ojociudadano/navigation/AppNavigation.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/components/AppBottomNavigation.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/components/CitizenAuthLayout.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/components/CitizenComponents.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/home/HomeScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/home/HomeViewModel.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/login/LoginScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/profile/ProfileScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/registration/RegistrationScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/report/CreateReportScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/IncidentComponents.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/IncidentDetailScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/IncidentDetailViewModel.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/IncidentErrors.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/IncidentPhoto.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/ReportsScreen.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/reports/ReportsViewModel.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/theme/Color.kt`
- `app/src/main/java/cl/uct/ojociudadano/ui/theme/Theme.kt`
- `app/src/test/java/cl/uct/ojociudadano/data/repository/RemoteRepositoryTest.kt`
- `app/src/test/java/cl/uct/ojociudadano/ui/reports/ReportsViewModelTest.kt`
- `docs/citizen-interface-and-incidents.md`
