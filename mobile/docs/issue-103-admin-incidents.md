# Issue #103 — Listado administrativo de incidentes Android

## Base y alcance

- Rama local: `feature/103-admin-incidents-list`; destino futuro: `integration`.
- Al comenzar, el árbol estaba limpio y la rama ya existía en `55dc538`, idéntica
  a `integration` y a `origin/integration` después de `git fetch origin integration`.
  No fue necesario crear la rama, actualizar su contenido ni mezclar ramas.
- Se confirmó el merge del PR #210 (#102) en `04eb8fe`. Se reutilizan su panel,
  ruta `admin/incidents`, regreso al panel y protección de destinos.
- Todos los cambios pertenecen a `mobile/`. No se modifican servidor, web,
  contratos, infraestructura, dependencias ni autenticación. Sin push ni PR.

## Archivos

Rutas relativas a `mobile/`:

| Archivo | Propósito |
| --- | --- |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsScreen.kt` | Pantalla Compose, lista, tarjetas e interfaz futura de selección por ID. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsViewModel.kt` | Estado observable atómico, carga inicial, control de concurrencia, reintento y factory. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsPendingScreen.kt` | Eliminado: se sustituye el placeholder de #102. |
| `app/src/main/java/cl/uct/ojociudadano/ui/admin/AdminHomeScreen.kt` | Actualiza la descripción del acceso a incidentes. |
| `app/src/main/java/cl/uct/ojociudadano/navigation/AppNavigation.kt` | Usa el listado en la ruta administrativa protegida existente. |
| `app/src/test/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsViewModelTest.kt` | Carga, resultados, vacío, errores, reintento, cancelación y consultas duplicadas. |
| `app/src/test/java/cl/uct/ojociudadano/integration/AuthenticatedIncidentsFlowTest.kt` | Añade dos casos de consulta general con sesión ADMIN mediante MockWebServer. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/admin/AdminIncidentsScreenTest.kt` | Cuatro casos de presentación, reintento, tamaño compacto y selección futura. |
| `app/src/androidTest/java/cl/uct/ojociudadano/ui/startup/RoleNavigationTest.kt` | Adapta las pruebas de #102 al listado, conservando bloqueo por rol y limpieza de navegación. |
| `docs/issue-103-admin-incidents.md` | Entrega técnica, validación, pruebas manuales y texto del futuro PR. |

## Comportamiento y arquitectura

`AdminHomeScreen` → `admin/incidents` → `AdminIncidentsScreen` →
`AdminIncidentsViewModel` → `IncidentRepository.getIncidents()` →
`RemoteIncidentRepository` → `ApiService.getIncidents()`.

La UI obtiene el ViewModel mediante la factory y el repositorio de `AppContainer`.
Observa un `StateFlow<AdminIncidentsState>` con estado y filas publicados juntos.
El ViewModel carga en `init` mediante `viewModelScope`, ignora cargas concurrentes
y propaga la cancelación. Compose no ejecuta operaciones de red. Retrofit mantiene
sus llamadas suspendidas existentes.

Se reutilizan `CitizenHeader`, `IncidentLoadState`, `IncidentsUiState`,
`incidentMessage()` e `incidentDate()`. No se usa `ReportsViewModel`, porque su
inicialización consulta reportes propios y categorías para filtros ciudadanos.
Tampoco se usa la tarjeta ciudadana, que consulta fotografías por fila y ofrece
abrir un detalle exclusivo de Ciudadano.

El listado usa `LazyColumn` con claves `incident.id`, conserva el orden recibido
y presenta título, descripción resumida, categoría, estado rotulado, fecha,
dirección/sector/comuna e ID real. La ubicación vacía se omite y las fechas inválidas
usan el texto existente de fecha no disponible. No se muestra prioridad opcional
ni información del reportante. Las tarjetas se adaptan al ancho y al tema Material.

Hay indicador de carga, resultados, mensaje de vacío solo ante una respuesta
exitosa sin filas y error con botón Reintentar. El reintento llama al ViewModel.
Los errores limpian las filas anteriores y reutilizan mensajes seguros para
conectividad, sesión, autorización, servidor y respuesta no interpretable.

`AdminIncidentsContent.onIncidentSelected: ((String) -> Unit)?` permite conectar
#104 usando el ID original. Actualmente no se proporciona callback en producción:
las tarjetas son informativas, sin acción vacía ni navegación a detalle ciudadano.
No se crean rutas de detalle, filtros ni operaciones de gestión.

## Contrato y sesión reutilizados

Única consulta del listado: `GET /api/v1/incidentes`, sin parámetros de filtro.
La respuesta existente es `List<IncidentDto>`, convertida mediante `toDomain()`:

- `id`, `titulo`, `descripcion`, `fechaCreacion`.
- `categoria.nombre`, `estado.nombre`.
- `ubicacion.direccionReferencia`, `ubicacion.sector`, `ubicacion.comuna`.
- `nivelPrioridad` es nullable; no se requiere ni se muestra.

Se inspeccionaron DTO, mapper, API Service y repositorio Android. Además se leyó,
sin modificarlo, el contrato en `docs/API_FILTROS_INCIDENTES.md` del repositorio
principal y `web/backend/apps/incidentes/src/incidentes.service.ts`: la consulta
sin filtros conserva el arreglo y orden por fecha descendente. No hay una
limitación del contrato local que impida utilizarlo para ADMIN.

`RemoteIncidentRepository` exige identidad autenticada, pasa el `TokenSnapshot`
a Retrofit y comprueba que la sesión siga vigente después de responder.
`BearerTokenInterceptor` adjunta el JWT existente. Solo se conserva la invalidación
existente por 401 de la sesión correspondiente; errores de red, 403 o 5xx no
incorporan cierres de sesión nuevos.

La ruta sigue registrada exclusivamente para ADMIN en `RoleAccessPolicy` y está
envuelta en `protectedComposable`, que valida antes de componer pantalla/ViewModel
y redirige al destino permitido si se intenta eludir `navigateIfAllowed`.
`SessionStartupGate` desecha navegación ante cambios de sesión, cuenta o rol.
No se cambia la política existente ni se depende solamente de ocultar el acceso.

## Verificaciones ejecutadas

Desde `mobile/`, el 2 de noviembre de 2026:

```bash
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
bash ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.admin.AdminIncidentsScreenTest,cl.uct.ojociudadano.ui.startup.RoleNavigationTest --console=plain
bash ./gradlew :app:testDebugUnitTest --tests cl.uct.ojociudadano.integration.AuthenticatedIncidentsFlowTest :app:lintDebug --console=plain
git diff --check
```

- Primera ejecución: compilación debug y de pruebas instrumentadas correctas;
  297 casos unitarios, 0 fallos/errores y 3 integraciones remotas omitidas.
  Incluye los cinco nuevos casos del ViewModel.
- Pruebas instrumentadas en Motorola AVD, Android 17: 13/13 correctas, sin omitidas.
  Cuatro casos nuevos de pantalla y nueve de navegación por rol. Se comprueba un
  ancho de 320 dp, estados, regreso, reintento, callback por ID, campos vacíos,
  sesión invalidada y rechazo de navegación interna para Ciudadano.
- Después de agregar dos casos del contrato ADMIN: suite dirigida
  `AuthenticatedIncidentsFlowTest`, 16/16 correctos. Comprueba JWT, consulta general
  sin filtro de reportante, conservación de ID/categoría/estado/sector y prioridad
  ausente; 403 y 503 preservan la sesión. Usa Retrofit real con MockWebServer local.
- Lint actualizado: 0 errores, 25 advertencias; `git diff --check` correcto.

Logs de esta ejecución en `/tmp/issue103-validation.log`,
`/tmp/issue103-instrumented.log` y `/tmp/issue103-contract-validation.log`.

No se ejecutó una consulta contra el backend desplegado con credenciales reales.
Las pruebas con servidor local y datos de prueba no acreditan disponibilidad
remota ni contenido real del despliegue. Producción utiliza exclusivamente el
repositorio remoto existente, sin datos simulados. No se cambió la configuración TLS.

## Pruebas manuales pendientes en Android Studio

1. Abrir `mobile/`, sincronizar Gradle y ejecutar la variante debug con el Gateway
   configurado. Iniciar sesión con una cuenta ADMIN autorizada.
2. Abrir **Gestión de incidentes** desde el panel. Comprobar carga y listado;
   contrastar ID, título, categoría, estado, fecha y ubicación con incidentes reales
   conocidos del entorno, incluidos los reportados por otras cuentas.
3. Girar el dispositivo y regresar desde segundo plano: no debe dispararse una
   consulta adicional por recomposición. Verificarlo con Network Inspector.
   Debe existir una consulta general, sin peticiones por cada fotografía/categoría.
4. Pulsar una tarjeta: en #103 no abre un detalle. Volver mediante cabecera y botón
   Atrás del sistema; ambos deben retornar al panel administrativo.
5. Desactivar conectividad antes de entrar al listado: comprobar error comprensible
   y sesión conservada. Reactivar conectividad y pulsar **Reintentar** para recuperar
   el listado. La respuesta vacía y los errores 401/403/503 ya tienen cobertura local;
   contrastarlos en un entorno de QA que permita esos escenarios sin alterar backend.
6. Revisar un entorno autorizado sin incidentes: debe mostrar **Aún no hay incidentes
   para mostrar.**, sin confundirlo con error de red.
7. Cerrar sesión, iniciar con CIUDADANO y comprobar que no hay acceso administrativo
   y que sus pantallas siguen funcionando. El intento de navegación interna directa
   está cubierto por `RoleNavigationTest`.
8. Revisar modo oscuro, fuente ampliada, orientación horizontal y TalkBack; confirmar
   lectura de títulos, estado, fecha e ID sin superposiciones. El ancho de 320 dp
   tiene cobertura instrumentada, pero no se afirma inspección manual de estos modos.

## Texto sugerido del Pull Request

Título: **feat(android): implementar listado administrativo de incidentes (#103)**

Destino: `integration`.

```markdown
El acceso Gestión de incidentes del panel Administrador ahora muestra el listado
real mediante el Repository existente y GET /api/v1/incidentes con la sesión JWT.
Incluye resumen por incidente, carga, vacío, errores comprensibles y reintento,
manteniendo la protección por rol de #102 tanto en acciones como en el destino.

Conserva los IDs y prepara un callback de selección para #104, sin implementar
detalle ni enlazar al detalle ciudadano. Solo cambia mobile/; sin endpoints,
dependencias, filtros ni operaciones administrativas nuevas.

Validación: compilación debug, compilación de pruebas y lint sin errores;
297 casos unitarios sin fallos (3 omitidos); 13/13 instrumentados en Android 17;
16/16 de la suite HTTP autenticada tras añadir los dos casos ADMIN.
La consulta al despliegue con credenciales reales queda pendiente de revisión manual.

Closes #103
```
