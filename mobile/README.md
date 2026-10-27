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

