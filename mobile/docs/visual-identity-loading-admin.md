
## Resultados de la entrega

Se ejecutaron desde mobile/:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.ui.components.LoadingContentTest,cl.uct.ojociudadano.ui.components.BrandResourcesTest,cl.uct.ojociudadano.ui.startup.RoleNavigationTest,cl.uct.ojociudadano.ui.admin.AdminIncidentDetailScreenTest,cl.uct.ojociudadano.ui.admin.AdminIncidentsScreenTest,cl.uct.ojociudadano.ui.reports.IncidentBrowsingTest,cl.uct.ojociudadano.ui.login.LoginScreenTest,cl.uct.ojociudadano.ui.registration.RegistrationNavigationTest,cl.uct.ojociudadano.ui.map.MapScreenTest --console=plain
```

BUILD SUCCESSFUL, salida 0: 420 casos unitarios (417 aprobados, 3 integraciones
remotas omitidas, sin fallos/errores), 49/49 instrumentadas aprobadas, APK debug
y compilación androidTest correctos. Lint: 0 errores y 36 advertencias. La primera pasada
instrumentada acotada pasó 36/36; la ampliada perdió conexión con el emulador
(device offline) y se repitió completa con éxito, sin modificar pruebas para
ocultar el fallo. Log de recuperación: `/tmp/visual-recovery-validation.log`.

Se inspeccionaron la previsualización circular, el ícono renderizado del APK,
el panel y la pantalla temporal. También se verificó visualmente el splash
mediante un arranque en frío de MainActivity en Motorola AVD Android 17: logo
oficial centrado y transición normal al login. Capturas locales:
`/tmp/ojo-launcher-device.png`, `/tmp/ojo-admin-dashboard.png`,
`/tmp/ojo-admin-coming-soon.png` y `/tmp/ojo-login.png` (splash).
Las dos pruebas que generan capturas se repitieron mediante am instrument después
de instalar ambos APK: 2/2 aprobadas. Esto permitió recuperar las imágenes antes
de la limpieza del runner. Sigue pendiente inspección visual en otras versiones
Android y máscaras de fabricantes; no se afirma que se hayan probado dispositivos
físicos ni todos los niveles de API soportados.

Durante el trabajo apareció `mobile/logoOjo.png` ya preparado en el índice por el
usuario. Se conserva intacto y fuera de los commits de estas tres mejoras. El
rediseño posterior del login con otro logo se gestiona por separado.
