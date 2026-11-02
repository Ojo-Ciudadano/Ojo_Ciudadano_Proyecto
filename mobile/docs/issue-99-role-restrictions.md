# Issue #99: restricciones por rol en Android

## Implementación

Se extienden #97 y #98 usando `SessionManager.state` como única fuente de sesión.
`RoleAccessPolicy` reutiliza `resolveInitialDestination`: únicamente una sesión localmente
válida con un único rol conocido recibe acceso a su flujo. No se infieren prioridades
entre roles. No se usan datos personales para decidir permisos.

| Estado de sesión | Destinos autorizados |
| --- | --- |
| Restaurando | Ninguno; el gate conserva la pantalla de carga |
| Sin autenticar | `login`, `register` |
| Solo CIUDADANO | `home`, `reports`, `incident/{id}`, `create_report`, `notifications`, `map`, `profile` |
| Solo ADMIN | `admin` provisional |
| Autenticada inválida, rol ausente, desconocido o ambiguo | `role_unavailable`, con cierre de sesión |

Las pantallas actuales de perfil, mapa, notificaciones y reportes forman parte de la
interfaz ciudadana. No hay actualmente pantallas funcionales compartidas con el flujo
administrativo; no se añade un menú administrativo ni acciones futuras.

## Navegación y acciones

- `navigateIfAllowed` consulta el estado actual al ejecutar la acción. Si no tiene permiso,
  devuelve `false` sin modificar la navegación. Los accesos del inicio, reportes,
  registro y barra inferior usan este mecanismo; volver a `home` comprueba el mismo permiso.
- Todos los destinos se registran mediante `protectedComposable`. Antes de componer
  una pantalla, sus ViewModels o botones, se consulta la política. Esto cubre también
  llamadas directas a `NavController.navigate` que omitan el control de la acción.
- Un destino rechazado no compone contenido. Un `LaunchedEffect` de su entrada activa
  vuelve al destino seguro de #98, descarta el historial activo y limpia los estados
  de navegación guardados. Se comprueba nuevamente la sesión antes de redirigir.
- El menú filtra sus entradas usando la misma política. Los botones y acciones
  ciudadanas están contenidos en destinos exclusivos protegidos. El administrador
  conserva el acceso independiente a su pantalla provisional y a cerrar sesión.
- Se mantiene el gate de #98: espera la restauración y descarta el controlador y su
  historial cuando cambia la cuenta, la sesión o el rol. La recomposición normal no
  crea un nuevo controlador ni reinicia el NavHost.

Las rutas no clasificadas se deniegan por defecto. Para #101/#102, registrar cada ruta
en la política y en el grafo con `protectedComposable`; sus accesos deben usar
`navigateIfAllowed`. Una pantalla futura compartida puede incluir `CITIZEN` y
`ADMINISTRATOR` en su conjunto de destinatarios. Una futura acción sin navegación
necesitará su permiso explícito en esta política antes de mostrarla y ejecutarla.
Esta issue no implementa flujos, gestión ni acciones administrativas nuevas.
Las restricciones del cliente no sustituyen la autorización del backend.

## Verificación ejecutada

```bash
./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest --tests 'cl.uct.ojociudadano.navigation.*' --tests 'cl.uct.ojociudadano.data.session.*' --tests 'cl.uct.ojociudadano.ui.login.AuthViewModelTest'
git diff --check
```

Resultado: compilación debug correcta, compilación de las pruebas de navegación correcta,
40 pruebas unitarias aprobadas, sin fallos ni omisiones. La política tiene cobertura
para ambos roles, todas las rutas actuales, rutas concretas de incidentes, rutas
desconocidas, restauración, sesiones inválidas, logout y cambio de cuenta en ambas
direcciones. Se adapta la prueba existente de recomposición a destinos autorizados y
se añade un caso de navegación programática administrativa hacia destinos ciudadanos.
**Las pruebas instrumentadas solo se compilaron; no se ejecutaron. No se realizaron
pruebas manuales ni se usaron emuladores.**

## Escenarios manuales pendientes

Usar cuentas existentes cuyo rol real se conozca, sin incluir sus credenciales en el
código ni en esta guía. El correo de una cuenta no determina sus permisos.

| Escenario | Pasos y resultado esperado |
| --- | --- |
| Ciudadano permitido | Iniciar sesión con CIUDADANO; recorrer inicio, reportes, detalle, creación, mapa, notificaciones y perfil. Mantener las funciones actuales y el menú ciudadano. |
| Ciudadano contra administrador | Comprobar ausencia de opciones administrativas. Solicitar `admin` con ambos métodos descritos debajo: bloqueo previo o redirección a `home`, sin mostrar contenido administrativo. |
| Administrador permitido | Iniciar sesión con ADMIN; mostrar `admin` provisional y cierre de sesión, sin depender del inicio o menú ciudadano. |
| Administrador contra ciudadano | Solicitar cada destino ciudadano, incluido `incident/<id>`: bloqueo previo o retorno a `admin`, sin componer pantallas ciudadanas. |
| Restauración | Cerrar y reabrir la app con cada rol. Esperar la restauración, sin pantalla del otro rol ni acceso antes de confirmar la sesión. |
| Cierre de sesión | Cerrar sesión desde perfil o pantalla provisional; volver a login. Atrás no recupera pantallas autenticadas. |
| Cambio de cuentas | ADMIN → logout → CIUDADANO y CIUDADANO → logout → ADMIN. No conservar opciones, estado ni historial de la cuenta anterior. |
| Rol desconocido o ausente | Con una sesión de prueba, usar rol ausente o desconocido. Solo mostrar acceso no disponible y cierre de sesión. |
| Rol ambiguo | Con una sesión de prueba con ADMIN y CIUDADANO, en ambos órdenes, no conceder ninguno de los flujos. |
| Atrás y recomposición | Recorrer rutas autorizadas y volver con Atrás; comprobar que cambios de configuración/recomposición conservan el destino permitido. Tras un rechazo o logout, Atrás no recupera el destino rechazado. |
| Navegación programática y estado guardado | Guardar estado de una pestaña, intentar un destino ajeno con navegación directa y luego volver/restaurar pestañas. No recuperar destinos restringidos ni crear bucles. |

Para probar llamadas programáticas sin añadir botones de producción, detener la app
con el depurador dentro de un callback de `AppNavigation` (hilo principal) y evaluar:

```kotlin
navController.navigateIfAllowed(sessionManager, AppRoutes.ADMIN) // Ciudadano: false
navController.navigate(AppRoutes.ADMIN) // Ciudadano: redirigir a home
navController.navigateIfAllowed(sessionManager, AppRoutes.CREATE_REPORT) // Admin: false
navController.navigate(AppRoutes.CREATE_REPORT) // Admin: redirigir a admin
```

Usar solo las expresiones correspondientes al rol activo. Para roles ausentes,
desconocidos o múltiples, usar un fixture de sesión de pruebas; los ejemplos existentes
en `RoleAccessPolicyTest` y `RoleNavigationTest` no requieren modificar el backend.

## Criterios de aceptación de #99

| Criterio | Estado de implementación |
| --- | --- |
| Rol de sesión determina pantallas y acciones | Implementado: política aplicada a accesos y destinos |
| Ciudadano no accede a destinos administrativos | Implementado: guardia previa y de destino |
| Acciones administrativas ocultas o deshabilitadas para Ciudadano | Implementado para la interfaz actual: destino administrativo protegido; aún no hay acciones de gestión |
| Sin identificadores de usuario hardcodeados | Implementado |
| Reutiliza sesión sin segunda fuente de verdad | Implementado: `SessionManager.state` |
| Intentos no permitidos controlados | Implementado: bloqueo previo o retorno seguro con limpieza de historial |
| Restricciones centralizadas sin duplicación innecesaria | Implementado: política y registro protegido común |
| Cierre y cambio de sesión actualizan restricciones | Implementado reutilizando el gate de #98 y consultando sesión actual |
| Solo restricciones del cliente; no sustituye backend | Cumplido: cambios exclusivos en Android |
| Rama `feature/99-role-restrictions` | Cumplido |
| PR hacia `integration` | Pendiente por prohibición explícita del usuario |
| PR vinculado con `Closes #99` | Pendiente por prohibición explícita del usuario |

La validación manual de los comportamientos de interfaz queda pendiente del usuario.
Los commits quedan locales; no se hace push ni se crea un PR.
