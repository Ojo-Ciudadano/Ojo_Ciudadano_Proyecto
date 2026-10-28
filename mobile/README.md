![Logo Ojo Ciudadano](https://raw.githubusercontent.com/diegoa18/OjoCiudadanoAndroid/main/logo.png)

# Ojo Ciudadano - Aplicación Android

Aplicación móvil del proyecto **Ojo Ciudadano**, desarrollada en **Android Studio** con **Kotlin**.


## Descripción

**Ojo Ciudadano** es una aplicación móvil orientada al reporte y seguimiento de incidencias ciudadanas.

Este repositorio contiene exclusivamente el desarrollo de la aplicación móvil para Android.

## Identidad autenticada en incidentes

`RemoteIncidentRepository` obtiene el UUID desde el `SessionManager` de #94 en
cada operación. El formulario no acepta un identificador de usuario y no lee
directamente la persistencia. `Sprint1ApiDefaults.USER_ID` fue eliminado en #96.

Contratos comprobados en `web/backend/apps/incidentes/src`:

- «Mis reportes» usa `GET /api/v1/incidentes?usuarioReportaId=<UUID de sesión>`.
  Inicio conserva la consulta general sin filtro.
- `POST /api/v1/incidentes` exige `usuarioReportaId` en el cuerpo
  (`CreateIncidenteDto`, validación UUID). `IncidentesService` lo utiliza para el
  incidente y su historial. Actualmente el backend no lo deriva del JWT.
- El JWT lo incorpora el interceptor existente de #95. La etiqueta local
  `TokenSnapshot` comprueba que la petición siga perteneciendo a la misma sesión;
  no se serializa ni cambia el contrato HTTP.

Una sesión ausente, en restauración, expirada o sin UUID válido impide enviar
solicitudes de incidentes. Las respuestas de sesiones anteriores se descartan.
Los errores se muestran mediante los estados existentes y los HTTP 401 siguen
utilizando la invalidación y navegación de #94/#95. La ubicación temporal de
Sprint 1 sigue fuera del alcance de la sustitución de identidad.

Pruebas locales (sin servidor de producción), desde `mobile/`:

```sh
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug
```

Las integraciones remotas son optativas mediante `RUN_API_INTEGRATION_TESTS` y
credenciales proporcionadas por el entorno; la prueba que crea incidentes exige
además `RUN_WRITE_INTEGRATION_TESTS=true`.

## Roles de la sesión autenticada (#97)

`RemoteAuthRepository` utiliza `usuario.roles` de la respuesta real de
`POST /api/v1/auth/login`. `LoginMapper` copia esos valores a la sesión;
`AuthenticatedSession.roleState` centraliza su interpretación mediante
`UserRole` y `SessionRoles`. El backend admite varios roles por usuario:

- `CIUDADANO` y `ADMIN` se representan en `SessionRoles.Known.values`.
- Un valor desconocido, incluso mezclado con valores conocidos, produce
  `SessionRoles.Unknown`; se conservan los valores originales sin asignar roles.
- Una respuesta sin roles, con `null` o con una lista vacía produce
  `SessionRoles.Missing`. La identidad puede seguir autenticada, pero no tiene
  un rol conocido. Nunca se asigna Ciudadano por defecto.

Los futuros ViewModels pueden observar `SessionManager.state` y consultar
`state.session.roleState` cuando el estado sea `SessionState.Authenticated`.
No es necesario depender de pantallas ni leer el almacenamiento directamente.

Se conserva el almacenamiento cifrado AES-GCM con Android Keystore y el esquema
v1 existente: persiste la lista original y deriva el modelo tipado al leerla.
Las sesiones v1 sin el campo `roles` se recuperan como `Missing`; los datos
malformados se descartan. Cambiar de cuenta reemplaza la sesión completa y
cerrarla elimina también sus roles. La restauración es local: no actualiza los
roles con `/usuarios/me`, aunque ese endpoint ya existe en el backend (#118).
Los cambios posteriores de rol en el servidor se reflejan al volver a iniciar
sesión. La autorización sigue siendo responsabilidad del backend.

La #97 se apoya en #94, #95 y #96, ya integradas. No añade navegación por roles
ni pantallas administrativas y no interpreta los roles decodificando el JWT.

Pruebas de roles sin servicios remotos:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cl.uct.ojociudadano.data.session.EncryptedSharedPreferencesSessionStorageTest
```

