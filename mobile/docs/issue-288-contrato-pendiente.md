# Issue #288: integración pendiente del contrato de perfil

Revisión: 2026-11-24. Rama: `feature/s3-profile-personal-data-edit`.
Base local revisada: `9f4e033`. El árbol de trabajo estaba limpio y la rama
ya existía, sin diferencias respecto de `integration` local.

## Bloqueo confirmado

La [issue #325](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/325)
propone `PATCH /api/v1/usuarios/me` para nombre y teléfono, pero no publica
el DTO exacto, el formato válido del teléfono, la representación para eliminarlo
ni el cuerpo o código de la respuesta exitosa. La issue sigue abierta.

Se revisaron el backend local y los archivos remotos de `integration`:

- `web/backend/apps/auth-usuarios/src/presentation/controllers/usuarios.controller.ts`:
  implementa `GET me`, pero no `PATCH me`.
- `web/backend/apps/auth-usuarios/src/application/services/usuarios.service.ts`:
  implementa la consulta de perfil y el cambio administrativo de rol, pero no
  la actualización del nombre o teléfono del usuario autenticado.
- `web/backend/apps/api-gateway/src/infrastructure/config/swagger.config.ts`:
  documenta la consulta de `/api/v1/usuarios/me`, sin contrato de edición.
- `web/backend/apps/api-gateway/src/presentation/controllers/proxy.controller.ts`:
  el proxy genérico de usuarios no constituye una implementación del endpoint
  de edición en el servicio de autenticación.

El DTO de registro solo valida que `telefono` sea una cadena opcional. Un ejemplo
como `+56987654321` no especifica una regla de validación para editar el perfil.
Que Prisma y el GET admitan `null` tampoco confirma que el PATCH lo acepte para
borrar el teléfono. No se encontró una rama con «325» ni un PR con ese término
en las consultas remotas realizadas.

Para completar la integración se necesita:

1. El endpoint de #325 implementado y su contrato accesible por el Gateway.
2. Los nombres, tipos y restricciones de los campos editables, incluida la
   normalización del nombre y del teléfono.
3. La regla de formato del teléfono y cómo eliminarlo: `null`, cadena vacía u
   otra representación explícitamente aceptada.
4. El código y cuerpo de éxito: perfil confirmado o confirmación sin perfil
   que requiera una consulta posterior; además de los errores de validación.

No se añadió un formulario conectado a un contrato supuesto ni un guardado
simulado. La issue #288 permanece sin implementar y no está lista para cerrar.

## Puntos de integración Android identificados

- `ui/profile/ProfileScreen.kt`: `ProfileScreen` obtiene el ViewModel con el
  repositorio real del contenedor; `ProfileContent` muestra los datos confirmados.
- `ui/profile/ProfileViewModel.kt`: consulta y observa cambios de usuario en
  `SessionManager`, cancela cargas anteriores y descarta el perfil al salir.
- `data/repository/RemoteProfileRepository.kt`: usa `requireIdentity`,
  `executeApiCall` y `expectedSession`; rechaza respuestas de otra sesión.
- `data/remote/ApiService.kt`: declara `GET api/v1/usuarios/me`.
- `data/remote/RetrofitClient.kt` y `data/remote/auth/BearerTokenInterceptor.kt`:
  cliente autenticado compartido y tratamiento de HTTP 401.
- `data/session/SessionManager.kt`: expone `StateFlow<SessionState>` y persiste
  con `SessionStorage`. La generación de sesión protege frente a un 401 tardío.
- `data/session/AuthenticatedSession.kt`: contiene JWT, tipo de token, identidad,
  correo, nombre y roles; actualmente no contiene teléfono.
- `data/session/EncryptedSharedPreferencesSessionStorage.kt`: serializa y cifra
  la sesión. La sincronización futura debe conservar credenciales y demás datos,
  persistir el teléfono de forma compatible y evitar sobrescribir otra sesión.

## Verificación

Las comprobaciones del código actual no acreditan los criterios de edición:
nombre vacío, teléfono inválido u opcional, cancelación sin red, reintento del
borrador y sincronización tras guardar siguen pendientes del contrato.

Comando solicitado para verificar la base Android:
`./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain`.
Resultado: `BUILD SUCCESSFUL` en 39 segundos; 539 pruebas registradas, 3 omitidas,
0 fallos y 0 errores. `lintDebug` y `assembleDebug` completados correctamente.
El primer intento no pudo escribir el bloqueo de la caché `~/.gradle` dentro
del sandbox; la ejecución autorizada fuera de él completó las verificaciones.
Registro de ejecución local: `/tmp/issue288-checks.log`.
