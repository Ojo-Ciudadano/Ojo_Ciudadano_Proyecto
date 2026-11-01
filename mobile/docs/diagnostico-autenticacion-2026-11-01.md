# Diagnóstico de login y registro Android — 1 de noviembre de 2026

## Estado inicial y alcance

Rama: `integration`. HEAD inicial: `c3964264edfd53898269b1b7cc19778bc01e5d22`.
`git status --short` estaba vacío. La modificación local de la conversación
anterior ya no existía al comenzar esta revisión. No se cambiaron ramas ni se
hicieron pull, merge, rebase o push. Todos los cambios están en `mobile/`.

Se consultó GitHub mediante el conector: `integration` apuntaba al mismo commit
que la copia local. Se leyeron backend, frontend, manifiestos y workflow solo
para diagnosticar; no se modificaron ni desplegaron.

## Actualización posterior — 07:53 UTC

Al repetir el login de la cuenta Ciudadano facilitada por el usuario, la API
respondió **200**, con `access_token`, `token_type: Bearer` y `usuario` con
identidad completa y rol `CIUDADANO`. Se observó esta respuesta únicamente en
una solicitud de diagnóstico con `curl --insecure`; no se imprimió el token.
Las credenciales fueron aceptadas y el fallo 502 anterior no se reprodujo.

La solicitud con verificación TLS y el certificado incluido sigue fallando con
error 60 (`self-signed certificate`), sin respuesta HTTP. Por lo tanto, el bloqueo
confirmado actual para Android sigue siendo TLS. No se alteró la validación de
certificados ni se verificó un login desde Android. Tampoco se verificó un
registro remoto exitoso. Los 502 documentados abajo corresponden a la medición
inicial, no al estado de esta comprobación posterior.

## Causas confirmadas en la medición inicial

1. **URL distinta en la aplicación y en el cliente por defecto.**
   `AppContainer` crea los clientes con `BuildConfig.API_BASE_URL`. Su valor por
   defecto en `app/build.gradle.kts` seguía siendo
   `https://ojociudadano.dev.censei.cl/`, mientras que `RetrofitClient` ya indicaba
   `https://api-ojociudadano.dev.censei.cl/api/v1/`. Cambiar solo Retrofit no
   cambiaba las solicitudes reales de la aplicación. Afectaba tanto al login
   como al registro.
2. **El antiguo dominio ahora sirve el frontend.** Su raíz responde 200 con
   HTML; `POST /api/v1/auth/login` devuelve 405 y HTML de Nginx. No es una
   respuesta de autenticación. Los manifiestos del frontend y el servidor
   estático concuerdan con esta observación.
3. **TLS del dominio nuevo incorrecto.** Una conexión a
   `api-ojociudadano.dev.censei.cl:443`, con SNI para ese dominio, entrega
   `CN=Kubernetes Ingress Controller Fake Certificate`, autofirmado, con
   `subjectAltName=DNS:ingress.local`. El certificado incluido en Android tiene
   SAN `ojociudadano.dev.censei.cl`, no el dominio nuevo. `curl --cacert` con ese
   certificado funciona para la raíz del frontend, pero falla con error 60
   para la API. Android conserva la verificación de confianza y hostname.
4. **Microservicio de autenticación inaccesible desde el Gateway.** Ambas
   cuentas de prueba facilitadas recibieron 502 con JSON
   `message: "Auth service no disponible"`. No se obtuvieron JWT ni usuarios.
   El texto corresponde a la rama de `auth-service.ts` que no recibe respuesta
   del microservicio `auth-usuarios`.
5. **Clasificación incompleta de errores Android.** TLS se mostraba como
   conectividad genérica; 405 como error inesperado. En login,
   `MalformedJsonException` hereda de `IOException` y podía etiquetar HTML
   recibido con HTTP 200 como fallo de red. El registro ya distinguía ese
   último caso. El mapper de login se ejecutaba fuera de `executeApiCall` y
   los DTO declaraban como no nulos campos que Gson puede omitir o devolver
   como null.
6. **Correo con espacios en el formulario de login.** La UI rechazaba un correo
   con espacios exteriores antes de llegar al `trim()` del ViewModel. El
   registro ya permitía recortarlos. Es un fallo independiente del servidor.

Las comprobaciones remotas se realizaron el 2026-11-01, aproximadamente entre
07:35 y 07:38 UTC. Para observar únicamente respuestas HTTP detrás del error
TLS se usó `curl --insecure` por solicitud durante el diagnóstico. No se añadió
ninguna excepción TLS a Android ni se guardaron credenciales o JWT en el repo.

## Evidencia y límites del servidor

| Comprobación | Resultado observado |
|---|---|
| GET raíz del frontend con el certificado incluido | 200, HTML |
| POST login en el dominio del frontend, cuerpo vacío | 405, HTML |
| API nueva con verificación TLS y certificado incluido | curl 60; sin respuesta HTTP |
| POST login en API nueva, cuerpo vacío, diagnóstico sin TLS | 400, validación JSON del Gateway |
| POST registro en API nueva, cuerpo vacío, diagnóstico sin TLS | 400, validación JSON del Gateway |
| POST login en API nueva con las dos cuentas de prueba, diagnóstico sin TLS | 502, `Auth service no disponible` |

El 400 por cuerpo vacío prueba que el Gateway atiende esas rutas; **no** prueba
que el microservicio ni la base de datos estén disponibles. No se creó ninguna
cuenta ni se verificó un registro exitoso remoto. El fallo de registro al
reenviar un cuerpo válido es una consecuencia esperable del microservicio
inaccesible y del mismo método `forward`, pero no se probó con una creación.
Tampoco se pueden confirmar las contraseñas ni la existencia de las cuentas
con una respuesta 502.

## Historial y contrato actual

- `950479a`, `c0b3e33`, `246f1ca`, `c8bc0a0`, `9774639`, `96193b9`, `e04cf29`
  y `478ade4`: integración de login, sesión cifrada, JWT, identidad, roles y
  navegación de #92–#98. Se revisaron sus implementaciones actuales.
- `a6344b7`, `d860d7d`, `7652a6d` y `dd563eb`: registro #202 e integración.
- `d68185e` (17 de octubre): sincronización del contrato de login; el código
  actual devuelve `access_token`, `token_type` y `usuario` con roles de texto.
- `58eed51` (1 de noviembre): movió el dominio de Retrofit y la configuración
  de confianza de red, y añadió el ingreso separado de la API. Dejó pendiente
  el dominio de `BuildConfig`, que es el utilizado en producción Android.
- `39eac44`: actualizó expectativas de pruebas de Retrofit sin comprobar la
  configuración con la que `AppContainer` construye el cliente.

No se encontró una modificación reciente del contrato exitoso que exigiera
inventar otro envoltorio, otro nombre para el JWT o roles nuevos. No fue posible
contrastar una respuesta exitosa desplegada porque el microservicio falla.

| Operación | Petición | Éxito según código vigente del servidor |
|---|---|---|
| POST `/api/v1/auth/login` | `email`, `password` | 200: `access_token`, `token_type`, `usuario: {id, nombre, email, roles: string[]}` |
| POST `/api/v1/auth/register` | `nombre`, `email`, `password`, `telefono` opcional | 201: usuario directo, sin JWT |

El Gateway acepta contraseñas de login desde 4 caracteres; el microservicio exige
6. El login Android mantiene su validación mínima de campo obligatorio y muestra
la validación del servidor. Registro exige 6 caracteres, nombre y correo;
teléfono es texto opcional. No se recorta la contraseña. No se envían roles:
`UsuariosService.create` asigna `CIUDADANO` en el servidor. Tras el registro se
vuelve al login con confirmación, sin autenticación automática.

## Correcciones Android

- `app/build.gradle.kts`: dominio por defecto de la API separado del frontend.
- `data/remote/RetrofitClient.kt`: el valor por defecto procede de BuildConfig,
  la misma configuración que usan `AppContainer` y el almacenamiento de sesión.
  Se conservan las opciones `API_BASE_URL` de Gradle, entorno y local.properties.
- `data/remote/dto/LoginResponseDto.kt`, `AuthUserDto.kt` y
  `data/mapper/LoginMapper.kt`: campos de transporte anulables y validación
  explícita de token, tipo e identidad antes de construir una sesión. Roles
  ausentes siguen siendo Missing; desconocidos siguen siendo Unknown. Los
  roles nulos/blancos dentro de una lista se rechazan, sin asignar privilegios.
- `data/repository/RemoteAuthRepository.kt`: deserialización y mapper de login
  dentro del manejo de errores existente; JSON mal formado se informa como
  respuesta inválida, no como desconexión.
- `ui/login/AuthViewModel.kt`, `ui/registration/RegistrationViewModel.kt`:
  mensajes en español para TLS, ruta no disponible y respuesta inválida;
  permanecen las distinciones entre 401, validación, 409, 5xx y desconexión.
- `ui/login/LoginFormValidator.kt`: admite espacios exteriores en el correo.

Se conserva la normalización de `/api/v1/` y no se duplica el prefijo. Los
clientes públicos de `AppContainer` siguen sin JWT. No se añadieron logs.
Se conserva el bloqueo de solicitudes duplicadas y el cuerpo de registro de
un solo envío automático. La contraseña del registro permanece solo en memoria.

## Compatibilidad de sesión y navegación

No se modificaron SessionManager, almacenamiento AES-GCM/Keystore, esquema v1,
caducidad local, invalidación por 401 ni aislamiento por entorno. Al cambiar el
dominio, una sesión cifrada del entorno anterior se descarta mediante el
mecanismo existente; hay que iniciar sesión otra vez. No se transfieren tokens
del dominio anterior automáticamente.

Se conservan UUID del usuario autenticado, JWT real, roles CIUDADANO/ADMIN,
navegación por rol y RoleAccessPolicy. Los roles ausentes, desconocidos o
ambiguos no reciben permisos implícitos. Un registro no establece sesión.
Un fallo de respuesta no se convierte en éxito ni persiste una sesión incompleta.

## Bloqueos que Android no puede resolver

1. **TLS / Ingress:** instalar y servir una cadena confiable con SAN
   `api-ojociudadano.dev.censei.cl`, vinculada al secreto TLS del ingreso
   correcto. Si la UCT utiliza una CA privada, el equipo debe proporcionar su
   certificado por un canal confiable para evaluar una actualización acotada
   de Android. No sirve confiar en el certificado predeterminado de Kubernetes
   ni reutilizar un certificado de otro hostname.
2. **Gateway → auth-usuarios:** comprobar pods, readiness, logs, endpoints de
   `auth-svc:3001` y `AUTH_SERVICE_URL`; el Gateway reporta que no puede contactar
   al servicio. No se determinó si la causa interna es pod caído, puerto,
   selector, red o configuración: requiere acceso al clúster. No hay contexto
   kubectl configurado en este entorno. No se hicieron cambios en el clúster.
3. **Manifiestos contradictorios:** `api-gateway-ingress.yaml` y
   `backend-ingress.yaml` declaran el mismo nombre y namespace con hosts
   diferentes (`api.ojociudadano...` y `api-ojociudadano...`). El orden de
   aplicación puede reemplazar la configuración. Es una inconsistencia
   confirmada en el repo; su efecto exacto en el despliegue es una hipótesis.
   Infraestructura debe definir el manifiesto y dominio vigentes.
4. **CI/CD:** `backend-ci.yml` filtra cambios del backend y del propio workflow;
   los pasos de despliegue actualizan imágenes y reinician deployments, no
   aplican los manifiestos de Ingress. Un commit exclusivamente Android no
   repara certificados, rutas ni disponibilidad del microservicio.

Hasta resolver TLS y la disponibilidad de auth-usuarios, los dos flujos reales
siguen bloqueados. Compilar correctamente no demuestra su recuperación remota.

## Verificación y pruebas manuales

Comprobaciones ejecutadas desde `mobile/`:

```sh
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --console=plain
```

Resultado: BUILD SUCCESSFUL. Se descubrieron 287 pruebas unitarias: 284
aprobadas, 3 omitidas, cero errores o fallos. Las omitidas son las integraciones
remotas optativas de categorías, consulta de incidentes y creación de incidentes;
no se habilitaron sus variables de entorno. Lint: cero errores/fatales y 25
advertencias. Se generó `app/build/outputs/apk/debug/app-debug.apk` y se compiló
el código Kotlin de las pruebas instrumentadas; no se ejecutaron en dispositivo.

Una prueba de configuración reprodujo primero la divergencia de URL (1 fallo)
y pasó tras corregirla. También pasaron las 8 pruebas de Retrofit usando
`-PAPI_BASE_URL=https://prueba.example.invalid/api/v1/`, sin contactar ese dominio,
para comprobar que se mantienen los overrides. Después se volvió a generar
el APK y a ejecutar la suite con la configuración predeterminada.

Las pruebas nuevas cubren contrato de login y roles CIUDADANO/ADMIN, ausencia de
Authorization en login público, prefijo único, respuestas HTML/incompletas,
401/405/502, TLS, rechazo de sesiones incompletas y correo con espacios. Las
pruebas existentes de registro, persistencia, tokens y permisos siguen pasando.
Se ejecutó `git diff --check` sin errores. No se ejecutan pruebas manuales en
dispositivo: las realiza el usuario. Estos resultados no prueban un login ni
registro exitoso contra el servidor remoto.

Preparación: instalar el APK nuevo, comprobar que no haya una propiedad
`API_BASE_URL` apuntando al frontend y utilizar fecha/hora automáticas en el
dispositivo. La URL por defecto requiere conectividad DNS/HTTPS al dominio de
la API tanto en emulador como en dispositivo físico; no usa localhost ni
10.0.2.2. En una configuración local propia esas dos direcciones tienen otro
significado y el tráfico HTTP sigue restringido por la política Android.

Mientras el servidor entregue el certificado incorrecto debe mostrarse el
mensaje de conexión segura y volver a habilitarse el botón. Después de resolver
TLS, un 502 debe mostrarse como error del servidor. No debe aparecer éxito.

Con TLS y el microservicio funcionando:

1. Desde login, abrir Crear cuenta. Intentar enviar campos vacíos y una
   contraseña de menos de seis caracteres: deben aparecer errores locales.
2. Registrar un correo nuevo con nombre y contraseña válida; teléfono opcional.
   Pulsar repetidamente mientras carga: debe haber un solo envío. No debe
   ofrecerse el rol Administrador. Debe volver al login con «Cuenta creada».
3. Iniciar sesión con ese usuario. Debe entrar al flujo Ciudadano con su
   identidad real, sin controles administrativos.
4. Cerrar sesión: debe volver al login y el botón Atrás no debe recuperar las
   pantallas protegidas.
5. Iniciar sesión nuevamente, cerrar completamente la aplicación y abrirla
   antes de que expire el JWT: debe recuperar la misma cuenta y rol. Tras
   cerrar sesión y reiniciar no debe recuperarse esa sesión.
6. Cerrar sesión e ingresar con una cuenta ADMIN existente. Debe entrar al
   panel Administrador. Cerrar sesión y volver a la cuenta Ciudadano no debe
   conservar permisos administrativos.
7. Probar un correo válido con contraseña incorrecta: un 401 debe mostrar
   «El correo o la contraseña no son correctos». No debe establecer sesión.
8. Sin sesión, activar modo avión e intentar login y registro. Debe desaparecer
   la carga y mostrarse un error de conexión. Recuperar red y reintentar
   manualmente. Si se perdió la respuesta de un registro, intentar login antes
   de repetir la creación, como indica el mensaje.
9. Intentar registrar el mismo correo: 409 debe marcarlo como ya registrado.
   Probar login con espacios exteriores en el correo: debe funcionar igual,
   sin modificar la contraseña.

No introducir credenciales reales en archivos versionados ni compartir JWT,
contraseñas o capturas de respuestas que los contengan.
