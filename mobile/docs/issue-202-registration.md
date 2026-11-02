# Issue #202: registro Ciudadano en Android

## Base e integración

Implementación iniciada en `feature/202-android-user-registration`, existente y limpia,
sobre `766326e`, también HEAD de `integration` y `origin/integration` tras `git fetch origin`.
No se mezclaron ramas ni se modificó código fuera de Android.

Se revisaron el historial y las implementaciones de #79 (URL y Retrofit), #92
(formulario Compose), #93 (AuthViewModel), #94 (SessionManager), #97 (roles) y
#98 (destinos iniciales). Todas estaban integradas. No se detectaron dependencias
pendientes de otras ramas ni conflictos locales.

Se reutilizan ApiService, RetrofitClient, AuthRepository, RemoteAuthRepository,
executeApiCall/ApiException, MaterialTheme, LoginRoute y AppNavigation.
RegistrationViewModel se limita al registro y no recibe SessionManager: no puede
establecer una sesión ni cambiar roles. AuthViewModel y la persistencia de sesión
no se modifican. Los métodos nuevos requieren extender los dobles de prueba
existentes; no se deshabilitaron pruebas ni se alteraron sus expectativas.

LoginScreen y LoginRoute reciben parámetros opcionales compatibles con sus llamadas
anteriores. AppNavigation recibe un destino y contenido adicionales. Solo la rama
no autenticada de SessionStartupGate conecta el registro al repositorio. Los destinos
y la resolución de roles #97/#98 mantienen su comportamiento.

## Contrato verificado en código local

- Gateway: `web/backend/apps/api-gateway/src/auth/auth-controller.ts`,
  `auth-service.ts` y `dto/register-dto.ts`.
- Microservicio: `web/backend/apps/auth-usuarios/src/auth/auth.controller.ts`,
  `auth.service.ts`, `usuarios/dto/create-usuario.dto.ts`,
  `usuarios/usuarios.service.ts` y `filters/http-exception.filter.ts`.
- Prisma, consultado solo para confirmar la respuesta:
  `web/backend/apps/auth-usuarios/prisma/schema.prisma`.

`POST /api/v1/auth/register`, sin autenticación:

```json
{
  "nombre": "Ana Ciudadana",
  "email": "ana@example.invalid",
  "password": "123456",
  "telefono": "+56 9 1234 5678"
}
```

Nombre obligatorio, correo válido y contraseña de al menos seis caracteres.
El teléfono es texto opcional, sin patrón o longitud adicionales en los DTOs.
`IsOptional` acepta omisión o null; Android convierte el teléfono vacío/blanco en
null y Gson lo omite. Se recortan nombre, correo y teléfono; la contraseña se envía
exactamente como se ingresó. Nunca se envían roles.

HTTP 201 devuelve directamente `id`, `nombre`, `email`, `telefono` nullable y
`fechaRegistro`; no devuelve JWT ni un objeto anidado `usuario`. El backend asigna
CIUDADANO. Se validan los campos esenciales de la respuesta antes de indicar éxito.
No se observaron diferencias entre el objetivo de la Issue y este contrato.

Errores: 409 por correo duplicado; 400 por validaciones (también se admite 422);
502 si el gateway no alcanza auth-usuarios; otros 5xx y respuestas inesperadas.
El formato habitual contiene `statusCode`, `message` (texto o lista), `error`,
`path` y `timestamp`. Se reutiliza el parser existente. Android muestra mensajes
propios en español, vincula los campos reconocidos y no expone mensajes técnicos.

La URL sigue proviniendo de BuildConfig.API_BASE_URL, configurable mediante propiedad
Gradle, entorno o local.properties, con el HTTPS existente por defecto.

## Comportamiento y seguridad

Pantalla desplazable con imePadding, nombre, correo, contraseña visible/oculta y
teléfono opcional. Validación en ViewModel antes de llamar al repositorio, indicador
de carga, campos deshabilitados durante el envío y bloqueo de solicitudes duplicadas.
No se guarda el formulario ni la contraseña en Bundle, preferencias o DataStore.
Los objetos de solicitud ocultan sus datos en toString y no se agregan logs.

El interceptor JWT omite únicamente POST /api/v1/auth/register, de modo que tampoco
invalida una sesión existente si ese endpoint devuelve 401. Se mantienen los
interceptores y el cliente actual; no hay cliente, sesión o mecanismo JWT paralelos.
RegistrationRequestInterceptor marca exclusivamente ese cuerpo como one-shot para
impedir reenvíos automáticos, sin alterar las demás peticiones. Una pérdida de
respuesta informa que el usuario puede intentar ingresar antes de repetir manualmente.

Al completar el registro se elimina su entrada del back stack y se muestra la
confirmación en login. El ViewModel pertenece a esa entrada y se elimina al salir.
Volver mediante el botón o el retroceso del sistema regresa a login. Una nueva
visita crea un formulario y estado nuevos. No se llama a login ni a establish.

## Verificación

Ejecutados satisfactoriamente:

- `./gradlew :app:testDebugUnitTest`: 224 casos descubiertos, 221 aprobados,
  3 omitidos por condiciones preexistentes de pruebas contra servidor real
  (RealIncidentCreationIntegrationTest, RealIncidentRepositoryIntegrationTest y
  RealCategoryRepositoryIntegrationTest). Cero fallos.
- `./gradlew :app:assembleDebug`: APK generado en
  `app/build/outputs/apk/debug/app-debug.apk`.
- `:app:connectedDebugAndroidTest` en emulator-5554, API 37, con filtro de clases
  RegistrationNavigationTest, LoginScreenTest, LoginRouteTest,
  SessionStartupGateTest y RoleNavigationTest: 19 aprobadas, cero fallos/omisiones.

Nuevas pruebas: 8 de validación, 8 de ViewModel, 6 de repositorio con MockWebServer
y 4 de navegación Compose. Cubren la solicitud exacta, omisión de teléfono y roles,
201, 409, validación, conectividad, JSON inválido, ausencia de reenvíos automáticos,
JWT público y preservación del resto de peticiones autenticadas, estados, reintento
manual, duplicados, vuelta al login y ausencia de persistencia de sesión.

Un fallo inicial de una prueba nueva reveló que MalformedJsonException hereda de
IOException. Se corrigió su clasificación únicamente en RemoteAuthRepository.register,
sin alterar executeApiCall ni las pruebas existentes.

## Dependencia externa y pendientes

Se intentó HEAD sobre `https://ojociudadano.dev.censei.cl/api/v1/auth/register`,
tanto en el entorno restringido como fuera de él. curl devolvió error 60:
`SSL certificate problem: self-signed certificate`. No fue posible verificar un
estado HTTP ni el contrato desplegado. No se deshabilitó TLS y no se crearon cuentas.

El equipo de infraestructura/despliegue debe revisar el certificado y la cadena TLS
del dominio/API Gateway (configuración de referencia:
`infrastructure/k8s/services/api-gateway-ingress.yaml`). La causa exacta y el responsable
individual no se pueden determinar con esta comprobación. Bloquea la comprobación
remota desde este entorno, no la implementación ni las pruebas simuladas Android.

Pendiente: verificar el flujo real y la asignación efectiva de CIUDADANO una vez que
el certificado sea confiable y el usuario autorice expresamente crear una cuenta.
No se verificó creación real ni asignación de roles en una base de datos remota.

## Inventario Android

Creados:

- `data/model/RegistrationInput.kt` (RegistrationInput, RegisteredUser).
- `data/remote/dto/RegisterRequestDto.kt`, `RegisterResponseDto.kt`.
- `data/remote/auth/RegistrationRequestInterceptor.kt`.
- `ui/registration/RegistrationFormValidator.kt` (incluye RegistrationFieldErrors).
- `ui/registration/RegistrationViewModel.kt` (incluye estado y factory).
- `ui/registration/RegistrationScreen.kt` (incluye RegistrationRoute).
- Unit tests: RegistrationFormValidatorTest, RegistrationViewModelTest y RegistrationRepositoryTest.
- Android test: RegistrationNavigationTest.
- Este documento dentro de mobile/docs.

Modificados:

- `data/remote/ApiService.kt`, `RetrofitClient.kt`, `auth/BearerTokenInterceptor.kt`.
- `data/repository/AuthRepository.kt`, `RemoteAuthRepository.kt`.
- `ui/login/LoginScreen.kt`, `LoginRoute.kt`.
- `navigation/AppNavigation.kt`, `ui/startup/SessionStartupGate.kt`.
- Dobles de pruebas: RemoteRepositoryTest, AuthViewModelTest, LoginRouteTest,
  SessionStartupGateTest y RoleNavigationTest.

Las rutas de producción anteriores son relativas a
`app/src/main/java/cl/uct/ojociudadano/`; las de pruebas mantienen sus paquetes
bajo `app/src/test/java/` o `app/src/androidTest/java/`.
