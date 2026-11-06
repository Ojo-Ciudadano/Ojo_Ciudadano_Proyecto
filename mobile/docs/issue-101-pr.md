# Borrador local de Pull Request — no publicado

- Base: `integration`
- Head: `feature/101-citizen-flow`
- Título: `feat(mobile): completar flujo funcional principal del Ciudadano - #101`

No publicar como cierre verificado hasta revisar los riesgos de identidad y
propiedad del backend y completar los recorridos remotos. No se hizo push por
instrucción del usuario.

---

Closes #101

## Objetivo

Completar el flujo funcional principal del Ciudadano.

## Cambios realizados

- Reutilización del inicio del Ciudadano y navegación protegida existentes.
- Consulta de incidentes generales y reportes propios con actualización explícita y al regresar.
- Creación con identidad autenticada y JWT mediante los mecanismos existentes.
- Conservación del GPS real y captura de dirección de referencia, comuna, región y sector requeridos por la API, eliminando textos provisionales.
- Conservación del flujo de evidencias, sus reintentos y éxitos parciales.
- Consulta del incidente recién creado desde la confirmación sin reenviar el formulario.
- Comprobación de rol administrativo en operaciones restringidas del repositorio compartido.
- Mensajes controlados de error y reintento de categorías.

## Pruebas realizadas

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain`: `BUILD SUCCESSFUL`.
- Unitarias e integración local: 437 casos, 434 aprobados, 3 integraciones remotas omitidas; cero fallos/errores.
- `:app:connectedDebugAndroidTest` con ocho clases seleccionadas de Ciudadano, sesión, navegación y Administración: 64 aprobadas, cero fallos/omisiones, Motorola AVD Android 17/API 37.
- Lint: cero errores; 36 advertencias.
- `git diff --check`: correcto.

## Consideraciones

- Cambios limitados a Android; sin modificaciones de backend ni infraestructura.
- El código backend revisado acepta el autor del cuerpo sin vincularlo al JWT y no comprueba propiedad al adjuntar evidencias. Android no puede sustituir esa autorización del servidor.
- No se ejecutó E2E manual/remoto con cuenta Ciudadano, GPS físico ni almacenamiento remoto. No había credenciales configuradas.
- La evidencia HTTP automatizada usa MockWebServer y GPS sustituido; no es una validación del despliegue.
- Una creación con timeout puede haberse aplicado en el servidor: la API no ofrece idempotencia. Verificar reportes propios antes de repetir.
- Informe completo: `mobile/docs/issue-101-citizen-flow.md`.
