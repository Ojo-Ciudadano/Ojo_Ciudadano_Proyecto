# Rediseño visual del inicio de sesión

## Rama y preservación del trabajo

Se mantiene exclusivamente `fix/105-admin-incident-status`. Al comenzar existían
los cambios autorizados de identidad visual, carga y panel administrativo, y el
archivo `mobile/logoOjo.png` ya preparado en el índice por el usuario. No se
sobrescribieron. Se ejecutaron status, consulta de rama y fetch; no había commits
nuevos de integration pendientes después del merge local `c77a9fb`.

## Imagen utilizada

La solicitud nombraba «logo ojo.png»; al consultar su ubicación, el usuario indicó
`/home/max/AndroidStudioProjects/Ojo_Ciudadano_Proyecto/mobile`. En ese directorio
la imagen nueva existe como `logoOjo.png` (416×160, RGB). Se comunicó esa diferencia
de nombre y se utilizó esa marca horizontal, conservando el original. El login
no utiliza el antiguo `mobile/logo.png`.

Se copió sin transformar a
`app/src/main/res/drawable-nodpi/login_brand_logo.png`. Ambos archivos tienen SHA-256:
`36c6081bcb082a2b5fc515e9b09762fc46078d1860dc8424d521b54b2f8f421c`.
No se añade transparencia a una imagen que originalmente no la tiene.

## Diseño

- `LoginBrandLayout`: encabezado azul marino #0B1F4D coincidente con el fondo de
  la imagen, logo centrado, subtítulo «Tu ciudad, tu reporte» y borde inferior
  redondeado. Tarjeta blanca de hasta 480 dp de ancho, esquinas de 28 dp y
  elevación sutil. Se utiliza el tema claro existente dentro del login para
  conservar el contraste de texto y campos sobre la tarjeta blanca.
- Logo con ContentScale.Fit y relación 416:160, ancho máximo de 300 dp; no se
  estira ni se recorta. Tiene descripción accesible «Ojo Ciudadano».
- «Bienvenido de nuevo» e «Inicia sesión para continuar», campos redondeados,
  botón rojo con altura mínima de 56 dp y enlace de registro que puede saltar
  de línea en anchos reducidos.
- Contenedor desplazable, imePadding y espaciados más compactos cuando baja la
  altura disponible. Se conserva el padding de sistema que entrega el Scaffold
  de navegación; no se agregan offsets para aparentar alineación.
- Visibilidad de contraseña, mensajes de validación y errores conservados.
  Se mantiene CitizenAuthProgress centrado y los controles se deshabilitan
  durante la carga. La acción del teclado también ignora el envío durante carga.

No se modificaron AuthViewModel, LoginRoute, Repository, Retrofit, interceptor
Bearer, JWT, persistencia de sesión ni navegación por roles para este rediseño.
Los cambios funcionales de esas capas de tareas anteriores se conservan.

## Verificación

Desde mobile/ se ejecutó:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.login.LoginScreenTest,cl.uct.ojociudadano.ui.login.LoginRouteTest,cl.uct.ojociudadano.ui.registration.RegistrationNavigationTest,cl.uct.ojociudadano.ui.startup.RoleNavigationTest --console=plain
```

BUILD SUCCESSFUL, salida 0. Resultados:

- 420 casos unitarios: 417 aprobados, 3 integraciones remotas omitidas, sin fallos
  ni errores. Las omisiones requieren habilitación explícita de pruebas remotas.
- Lint: 0 errores, 36 advertencias.
- APK debug y compilación androidTest correctos.
- 27/27 instrumentadas aprobadas en Motorola AVD Android 17: campos, contraseña,
  validaciones, callbacks de credenciales, carga sin controles activos, errores,
  registro y navegación por roles. Incluyen pantalla de 320×480 dp con teclado,
  desplazamiento hasta el botón y centrado/proporción geométrica del logo.
- Antes de conectar el encabezado se verificó el formulario con 15/15 pruebas
  instrumentadas. Prevalece el resultado de la ejecución final de 27 casos.
- Log final: `/tmp/login-redesign-final-validation.log`.
- Inspección visual del APK real tras su arranque: `/tmp/ojo-login-settled.png`.
- `git diff --check` correcto. Sin cambios propios de backend o infraestructura.

No se ejecutó un login contra el despliegue usando credenciales reales. Las
pruebas verifican el flujo existente con sus dobles de test y contratos locales;
no certifican la disponibilidad de un despliegue remoto.

Después de ordenar los imports se repitieron `testDebugUnitTest lintDebug
assembleDebug` desde mobile/: BUILD SUCCESSFUL, salida 0, evidencia en
`/tmp/login-redesign-final-checks.log`. La inspección visual de la captura confirmó
el logo completo, los colores, la tarjeta y el enlace de registro sin recortes.
