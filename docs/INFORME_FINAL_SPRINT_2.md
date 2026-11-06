# Informe Final - Sprint 2 (Taller II)

## Portada

**Proyecto:** Ojo Ciudadano  
**Asignatura / Taller:** Taller II  
**Issue de referencia:** [#160](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/160)  
**Fecha del informe:** 5 de noviembre de 2026  
**Versión:** 1.0  
**Repositorio:** [Ojo-Ciudadano/Ojo_Ciudadano_Proyecto](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto)  
**Fuentes editables:** Este documento y el resto de los documentos relacionados se encuentran en [`docs/`](./).

### Integrantes

# Taller II:
- Bárbara Sáez
- Cristóbal Santibáñez
- Martín Maier
- Bernardo Sandoval
- Alejandra Marilao
- Franchesca Marcial

# Taller IV:
- Oscar Barra
- Máximo Mora
- Diego Álvarez
---

## 1. Alcance comprometido vs. resultado alcanzado

Durante el Sprint 2, Taller II se comprometió a entregar la integración del flujo web para los roles Ciudadano y Administrador, incluyendo la gestión de evidencias, notificaciones, despliegue y automatización de pruebas.

**Resultado real:** los flujos visuales y la integración con el backend quedaron implementados a nivel de código, con PRs fusionados. No obstante, el cierre integral quedó parcialmente verificado, sin verificación visual ni remota completa. No se dispone actualmente de capturas remitidas al repositorio ni se confirmó una ejecución completa del ciclo E2E contra Kubernetes UCT. La Issue [#129](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/129) se cerró el 2 de noviembre de 2026 a las 02:23 UTC (23:23 del 1 de noviembre, UTC-3); el E2E se ejecutó 1 h 17 min después, a las 00:40 UTC-3 del 2 de noviembre. El HTTP 404 recibido desde Nginx indica que el servidor respondió, pero el Ingress o el enrutamiento no resolvían la ruta de autenticación esperada.

## 2. Entornos de ejecución

| Entorno | Uso previsto | Estado observado |
| :--- | :--- | :--- |
| **Docker Compose local** | Desarrollo, bases de datos y pruebas manuales/locales. | Disponible como alternativa local. No reemplaza la verificación remota. |
| **Kubernetes UCT (remoto)** | Despliegue final, integración y E2E. | Despliegue cerrado mediante [#129](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/129); el E2E de [#158](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/158) recibió HTTP 404 de Nginx en `/auth/login`. |

**URL remota usada por el E2E:** `https://api.ojociudadano.dev.censei.cl/api/v1`  
**URL del frontend documentada:** `https://ojociudadano.dev.censei.cl`

No se incluyen capturas en esta versión. Las que se incorporen indicarán su entorno (**Local - Docker Compose** o **Remoto - Kubernetes UCT**), sin presentar evidencias locales como remotas.

## 3. Evidencias de flujos web implementados

**Estado de evidencia:** No se incluyen capturas en esta versión. Cuando se incorporen, cada una identificará la funcionalidad, el rol y el entorno (**Local - Docker Compose** o **Remoto - Kubernetes UCT**).

### 3.1 Flujo Ciudadano

| Funcionalidad pendiente de captura | Rol | Entorno esperado | Issues / PRs de respaldo | Estado |
| :--- | :--- | :--- | :--- | :--- |
| Mis reportes | Ciudadano | Local o remoto, según origen real de la captura | [PR #55](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/55), [PR #245](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/245); Issue no localizada | Implementación por confirmar visualmente; captura pendiente |
| Crear incidente con ubicación y fotografías | Ciudadano | Local o remoto, según origen real de la captura | Fotografía: [PR #257](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/257); GPS: [PR #258](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/258) | Captura pendiente |
| Mapa de incidentes | Ciudadano | Local o remoto, según origen real de la captura | [PR #259](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/259) | Captura pendiente |
| Notificaciones | Ciudadano | Local o remoto, según origen real de la captura | [#113](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/113), [#114](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/114); [PR #216](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/216), [PR #223](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/223), [PR #244](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/244) | Captura pendiente |

### 3.2 Flujo Administrador

| Funcionalidad pendiente de captura | Rol | Entorno esperado | Issues / PRs de respaldo | Estado |
| :--- | :--- | :--- | :--- | :--- |
| Tabla de incidentes y filtros | Administrador | Local o remoto, según origen real de la captura | [PR #212](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/212), [PR #222](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/222); Issue no localizada | Captura pendiente |
| Detalle de incidente y evidencias | Administrador | Local o remoto, según origen real de la captura | [PR #229](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/229), [PR #264](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/264); Issue no localizada | Captura pendiente |
| Cambio de estado, asignación y prioridad | Administrador | Local o remoto, según origen real de la captura | [PR #231](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/231), [PR #232](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/232), [PR #255](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/255) | Captura pendiente |
| Historial del incidente | Administrador | Local o remoto, según origen real de la captura | [PR #249](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/249); Issue no localizada | Captura pendiente |

### 3.3 Gestión de usuarios y roles

| Funcionalidad pendiente de captura | Rol | Entorno esperado | Issues / PRs de respaldo | Estado |
| :--- | :--- | :--- | :--- | :--- |
| Lista de usuarios y cambio de rol | Administrador | Local o remoto, según origen real de la captura | [PR #251](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/251) | Captura pendiente |

## 4. Cobertura contrastada con el plan #155

La matriz definida en [`Plan_Pruebas_Web_Roles.md`](./Plan_Pruebas_Web_Roles.md) contiene los casos previstos por rol. La siguiente tabla resume lo observado en esta entrega; “implementación confirmada” significa revisión estática de código, no ejecución exitosa.

| Rol | Casos previstos en #155 | Ejecutados | Entorno real | No ejecutados / pendientes | Evidencia disponible |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Ciudadano | Registro e inicio de sesión (M-01 y M-02); perfil M-03; listado, filtro y creación (M-10, M-12 y CP-WEB-04); GPS/fotos, mapa y notificaciones (M-17 a M-22 y CP-WEB-09) | Ningún caso de interfaz confirmado por captura o ejecución remota; revisión de implementación disponible | Ninguno | Casos de interfaz, restricciones y E2E remotos pendientes | PRs #55, #245, #257, #258, #259 y #244; capturas pendientes |
| Administrador | Usuarios y roles (M-04 a M-09); listado y filtros (M-10); detalle y evidencias (M-11, M-17 a M-22); gestión, asignación, prioridad e historial (M-13 a M-16) | Ningún caso confirmado por captura o ejecución remota | Ninguno | Casos web y remotos pendientes | PRs #212, #222, #229, #231, #232, #249, #255 y #264; capturas pendientes |
| Administrador (usuarios) | Listado de usuarios y cambio de rol (M-04 y M-05) | Ningún caso confirmado por captura o ejecución remota | Ninguno | Prueba funcional y verificación de restricciones | PR #251; captura pendiente |
| Restringidos / sin sesión | Permisos de usuarios, incidentes, evidencias y notificaciones (M-01 a M-22); casos negativos CP-NEG-01 a CP-NEG-07 | No ejecutados | Ninguno | Verificación HTTP y de interfaz | Matriz prevista en #155 |

### Observaciones verificables

- El endpoint de notificaciones `PATCH /notificaciones/:id/leida` (M-20) quedó **implementado según revisión de código** en la fuente backend. Esta afirmación no constituye una prueba ejecutada.
- Las inconsistencias documentadas en Sprint 1, como problemas de rutas o dependencia de Prisma, quedaron **en revisión de código como corregidas o mitigadas**, pero no existen resultados nuevos suficientes para declararlas probadas de extremo a extremo.
- Las pruebas restrictivas, de roles y de concurrencia quedaron pendientes de ejecución remota. El plan conserva estos casos para una campaña posterior.

## 5. Automatización E2E #158

- **Fecha:** 2 de noviembre de 2026.
- **Hora registrada:** 03:40:22.966 UTC (00:40:22.966 UTC-3).
- **Issue:** [#158](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/158).
- **PR:** [#263](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/263).
- **URL objetivo:** `https://api.ojociudadano.dev.censei.cl/api/v1/auth/login`.
- **Comando reportado:** `npm run test:e2e:lifecycle`.
- **Resultado real:** `FAILED`; en el primer paso se recibió HTTP 404 de Nginx en lugar del HTTP 200 esperado.
- **Evidencia estructurada:** [`REPORTE_EJECUCION_E2E_158.json`](./REPORTE_EJECUCION_E2E_158.json).
- **Captura disponible:** No existe en el repositorio una captura de este resultado.

El JSON registra 1 comprobación ejecutada, 0 aprobadas y 1 fallida. El flujo completo de creación, cambio de estado y consulta del historial no llegó a ejecutarse. El resultado corresponde a un enrutamiento incorrecto del entorno remoto (HTTP 404 de Nginx), no a una aprobación del E2E.

No se encuentra en la evidencia disponible una ejecución alternativa documentada contra Docker Compose. Si se realiza, debe registrarse por separado con fecha, URL local, resultado y evidencia; no sustituirá la prueba requerida en Kubernetes UCT.

## 6. Trazabilidad funcionalidad–Issue–PR

| Funcionalidad | Issue | PR | Estado real | Entorno |
| :--- | :--- | :--- | :--- | :--- |
| Mis reportes | Issue no localizada | [#55](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/55), [#245](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/245) | PR fusionado; ejecución visual/remota pendiente | Local/remoto por confirmar |
| Tabla y filtros administrativos | Issue no localizada | [#212](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/212), [#222](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/222) | PR fusionado; ejecución pendiente | Local/remoto por confirmar |
| Detalle administrativo | Issue no localizada | [#229](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/229), [#264](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/264) | PR fusionado; ejecución pendiente | Local/remoto por confirmar |
| Historial del incidente | Issue no localizada | [#249](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/249) | PR fusionado; ejecución pendiente | Local/remoto por confirmar |
| Cambio de estado administrativo | Issue no localizada | [#231](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/231) | PR fusionado; ejecución pendiente | Local/remoto por confirmar |
| Asignación y prioridad administrativa | Issue no localizada | [#232](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/232), [#255](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/255) | PR fusionado; ejecución pendiente | Local/remoto por confirmar |
| Crear incidente con ubicación y fotos | Issue no localizada | [#257](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/257), [#258](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/258) | PR fusionado; funcionalidad implementada a nivel de código; ejecución visual/remota pendiente | Local/remoto por confirmar |
| Notificaciones | [#113](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/113), [#114](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/114) | [#216](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/216), [#223](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/223), [#244](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/244) | Issues cerradas; verificación visual/remota pendiente | Local/remoto por confirmar |
| Evidencias | [#80](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/80), [#85](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/85), [#86](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/86), [#106](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/106) | [#180](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/180), [#184](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/184), [#189](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/189), [#264](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/264) | Issues cerradas; verificación remota pendiente | Local/remoto por confirmar |
| Autorización por rol | [#100](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/100) | [#183](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/183) | Issue cerrada; prueba de permisos pendiente | Local/remoto por confirmar |
| Mapa de incidentes | Issue no localizada | [#259](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/259) | PR fusionado; ejecución visual pendiente | Local/remoto por confirmar |
| Cambio de roles | Issue no localizada | [#251](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/251) | PR fusionado; ejecución pendiente | Local/remoto por confirmar |
| Automatización de ciclo de incidente | [#158](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/158) | [#263](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/263) | PR fusionado; Issue abierta por fallas pendientes | Kubernetes UCT fallido |
| Despliegue Web | [#129](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/129) | [#207](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/207), [#226](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/pull/226) | Issue cerrada el 2 de noviembre; E2E posterior recibido por el Nginx remoto pero con enrutamiento incorrecto | Kubernetes UCT |

## 7. Estado de las siete Issues de Taller II

Estas Issues fueron incorporadas a Taller II tras la actualización de responsables y no constituyen trabajo duplicado.

| Issue | Funcionalidad | Estado GitHub al 5 de noviembre | Estado de verificación en este informe |
| :--- | :--- | :--- | :--- |
| [#80](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/80) | Modelo persistente de evidencias con Prisma | Cerrada | Implementación cerrada; verificación remota pendiente |
| [#85](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/85) | Evidencias mediante Supabase Storage | Cerrada | Implementación cerrada; verificación remota pendiente |
| [#86](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/86) | API de subir y consultar evidencias | Cerrada | Implementación cerrada; verificación remota pendiente |
| [#100](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/100) | Autorización para Ciudadano y Administrador | Cerrada | Implementación cerrada; prueba funcional de permisos pendiente |
| [#106](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/106) | Evidencias en respuestas de incidentes | Cerrada | Implementación cerrada; verificación remota pendiente |
| [#113](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/113) | Generación y persistencia de notificaciones | Cerrada | Implementación cerrada; verificación visual/remota pendiente |
| [#114](https://github.com/Ojo-Ciudadano/Ojo_Ciudadano_Proyecto/issues/114) | API de consulta de notificaciones | Cerrada | Implementación cerrada; verificación remota pendiente |

## 8. Limitaciones, defectos conocidos y trabajo incompleto

### Limitaciones verificadas

- Kubernetes UCT respondió con HTTP 404 de Nginx para `/api/v1/auth/login` durante el intento documentado de #158; por ello, el flujo completo quedó bloqueado.
- No existe evidencia suficiente para determinar si el problema de enrutamiento persistió después del intento documentado.
- Las capturas visuales solicitadas no están versionadas en el repositorio.

### Defectos conocidos

No se identificaron otros defectos de aplicación en la evidencia disponible. El defecto documentado corresponde al entorno remoto: la ruta probada `/api/v1/auth/login` respondió con HTTP 404 desde Nginx. El origen probable es el despliegue o el enrutamiento del Ingress; este bloqueo impidió continuar con el ciclo E2E.

### Trabajo incompleto

- Registro y evidencia suficiente del rol Registrador.
- Pruebas de concurrencia.
- Prueba exhaustiva de restricciones por rol y sin sesión.
- Capturas de los flujos Ciudadano, Administrador y gestión de roles.
- Varias funcionalidades web no tienen Issue localizada; solo se pudo vincular el PR correspondiente.
- Ejecución alternativa documentada de E2E local con Docker Compose, si corresponde.

## 9. Conclusión

Taller II dejó implementaciones cerradas en varias áreas funcionales y despliegue Kubernetes documentado. Sin embargo, el nivel real de cierre verificable es parcial: el entorno remoto no respondió con el contrato esperado para el E2E, las capturas solicitadas no están disponibles y varias pruebas no fueron ejecutadas. El informe distingue explícitamente implementación cerrada, revisión de código y ejecución probada para evitar presentar trabajo pendiente como completado. Antes de cerrar la entrega, deben incorporarse las capturas reales, completar la trazabilidad y repetir el E2E remoto.
