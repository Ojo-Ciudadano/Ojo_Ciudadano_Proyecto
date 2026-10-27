# ADR-001: Continuidad del Versionado `/api/v1` durante el Sprint 2

---

## 1. Contexto

Durante el Sprint 1 se estableció el **API Gateway** como punto único de entrada HTTP del sistema **Ojo Ciudadano**, configurando globalmente el prefijo de ruta `/api/v1` (`app.setGlobalPrefix('api/v1')` en `web/backend/apps/api-gateway/src/main.ts`).

Actualmente, dos clientes activos consumen este contrato unificado:
1. **Aplicación Móvil Android (4° Año — `mobile/app`)**: Consume `/api/v1/auth/*`, `/api/v1/incidentes`, `/api/v1/categorias`, `/api/v1/evidencias` y `/api/v1/notificaciones` a través de su capa de datos remota configurada sobre la URL base del API Gateway.
2. **Plataforma Web de Gestión (2° Año — `web/frontend`)**: Consume `/api/v1/*` mediante el cliente HTTP centralizado (`web/frontend/src/services/api.ts`), tanto en desarrollo local (`http://localhost:3000/api/v1`) como en el clúster Kubernetes UCT (`https://ojociudadano.dev.censei.cl/api/v1`).

En el **Sprint 2** se incorporan nuevas capacidades funcionales de negocio:
* Subida, consulta y descarga de evidencias fotográficas almacenadas en Supabase Storage (**#86**, **#106**).
* Consulta de notificaciones del usuario autenticado (**#114**).
* Gestión del ciclo de vida de incidentes: cambio de estado con validación de transiciones y registro en historial (**#117**), consulta de historial (**#120**), filtros combinables de consulta (**#119**), asignación/reasignación de responsables (**#123**), prioridad inicial por categoría y su modificación (**#124**), y ampliación del detalle del incidente con historial y asignación activa (**#125**).
* Gestión de perfil y administración de usuarios/roles: consulta de perfil propio `GET /usuarios/me` (**#118**), listado paginado de usuarios con filtro por rol (**#121**) y cambio de rol restringido a `ADMIN` (**#122**).
* Endurecimiento de políticas de **autenticación JWT** y **autorización basada en roles (RBAC)** sobre operaciones protegidas.

Surge la necesidad arquitectónica de definir formalmente si estas incorporaciones del Sprint 2 deben exponerse bajo una nueva versión (`/api/v2`) o mantenerse evolucionando dentro del espacio de nombres `/api/v1`.

---

## 2. Decisión

**Se decide mantener el prefijo `/api/v1` como única versión activa de la API pública en el API Gateway durante todo el Sprint 2**, descartando la migración o duplicación de rutas hacia `/api/v2`.

Todas las nuevas rutas, parámetros de consulta opcionales y campos adicionales de respuesta planificados para el Sprint 2 se integrarán directamente sobre `/api/v1`, respetando las reglas de evolución compatible definidas en este documento.

---

## 3. Justificación Técnica

### 3.1. Naturaleza aditiva de las ampliaciones de contrato
Los cambios de modelo de datos y payloads JSON introducidos en el Sprint 2 responden a un patrón de **evolución aditiva (retrocompatible)**:
* **Nuevos endpoints independientes**: Operaciones como `POST /api/v1/incidentes/:id/evidencias`, `GET /api/v1/evidencias/:id/archivo`, `GET /api/v1/usuarios/me`, `PATCH /api/v1/incidentes/:id/estado` o `GET /api/v1/notificaciones` agregan rutas nuevas que no colisionan con las rutas consumidas en el Sprint 1.
* **Ampliación no destructiva de respuestas existentes**: La incorporación de colecciones o propiedades nuevas en respuestas existentes (por ejemplo, el arreglo `evidencias` en las respuestas de incidentes de **#106**, o `historial`, `asignacionActiva` y `prioridad` en **#124** y **#125**) conserva intactos todos los campos previos (`id`, `titulo`, `descripcion`, `fechaCreacion`, `categoria`, `estado`, `ubicacion`). Los deserializadores JSON de Android (Kotlin/Gson/Moshi) y Web (TypeScript) ignoran propiedades adicionales no mapeadas o las consumen progresivamente sin romper vistas existentes.
* **Filtros opcionales en consultas**: Los nuevos parámetros de consulta (`?estadoId=`, `?comuna=`, `?categoriaId=` en **#119** o paginación/rol en **#121**) son opcionales; invocar el endpoint sin dichos parámetros preserva el comportamiento base esperado por los clientes actuales.

### 3.2. Distinción entre cambios aditivos de datos y restricciones de Autenticación / Autorización
Es fundamental explicitar que **no todas las modificaciones del Sprint 2 son puramente aditivas en términos de acceso**. El cierre de brechas de seguridad mediante `JwtAuthGuard` y `RolesGuard` introduce restricciones de protocolo que sí impactan a los clientes:

| Tipo de Cambio en Sprint 2 | Clasificación | Impacto sobre Clientes (Web y Android) | ¿Requiere `/api/v2`? |
| :--- | :--- | :--- | :---: |
| Nuevos endpoints (`POST /incidentes/:id/evidencias`, `GET /usuarios/me`, `PATCH /incidentes/:id/estado`, etc.) | **Aditivo** | Ningún impacto en flujos existentes; habilita nuevas pantallas y casos de uso. | **No** |
| Nuevos campos en respuestas JSON (`evidencias`, `prioridad`, `historial`, `asignacionActiva`) | **Aditivo** | Retrocompatible siempre que se mantengan los campos previos y se representen colecciones vacías como `[]`. | **No** |
| Nuevos query params opcionales (`estado`, `comuna`, `categoria`, paginación) | **Aditivo** | Sin impacto cuando se omiten los parámetros en la petición. | **No** |
| Exigencia de token JWT (`Authorization: Bearer <token>`) en rutas protegidas | **Restricción de Seguridad (No aditivo)** | Los clientes **deben** autenticarse previamente en `/api/v1/auth/login` y adjuntar el header `Bearer` (no se aceptan peticiones anónimas ni credenciales hardcoded de desarrollo). | **No** (es el cumplimiento del contrato de seguridad original de `v1`) |
| Restricción por rol (`ADMIN`, `OPERADOR`, `CIUDADANO`) en operaciones administrativas | **Restricción de Autorización (No aditivo)** | Un cliente autenticado sin el rol requerido recibirá `403 Forbidden` (ej. un `CIUDADANO` intentando ejecutar `PATCH /usuarios/:id/rol` en **#122**). | **No** (aplica reglas de negocio del dominio sobre `v1`) |

Exigir autenticación JWT y autorización por roles corresponde a la consolidación de seguridad prevista desde el diseño de `/api/v1`, por lo que versionar hacia `/api/v2` dejando `/api/v1` desprotegida constituiría una vulnerabilidad de seguridad inaceptable.

---

## 4. Condiciones Obligatorias para Conservar la Compatibilidad en `/api/v1`

Para que cualquier Pull Request del Sprint 2 pueda fusionarse bajo `/api/v1`, debe cumplir estrictamente las siguientes invariantes de contrato:

1. **Invariabilidad de campos existentes**:
   * No eliminar ni renombrar propiedades ya presentes en los DTOs de respuesta de `auth`, `usuarios`, `roles`, `incidentes`, `categorias` ni `notificaciones`.
   * No cambiar el tipo de dato primitivo ni la estructura anidada de los campos existentes (por ejemplo, `id` sigue siendo `string` UUID, `ubicacion.latitud` y `ubicacion.longitud` siguen siendo `number`).
2. **Representación determinista de colecciones y valores opcionales**:
   * Toda colección incorporada a una respuesta (como `evidencias: []` en **#106** o `historial: []` en **#125**) debe devolverse como un arreglo vacío `[]` cuando el recurso no posea registros asociados, evitando retornar `undefined` o alterar la estructura raíz del objeto.
3. **Conservación del comportamiento por defecto en consultas (`GET`)**:
   * En endpoints que incorporen nuevos filtros o parámetros de consulta (**#119**, **#121**), todos los nuevos parámetros deben ser **opcionales** (`@IsOptional()`). Una llamada sin los nuevos parámetros debe seguir siendo válida sin lanzar `400 Bad Request`.
4. **Consistencia en el estándar de errores HTTP**:
   * Mantener el formato uniforme de excepción (`statusCode`, `timestamp`, `path`, `error`, `message`) provisto por `HttpExceptionFilter` tanto en el API Gateway como en los microservicios.
5. **Encapsulación de almacenamiento externo**:
   * Las respuestas de evidencias (**#86**, **#106**) solo deben exponer metadata pública y referencias relativas a la API (`/evidencias/:id/archivo`), sin filtrar claves internas de Supabase Storage (`claveArchivo`) ni credenciales administrativas.

---

## 5. Trazabilidad con las Issues de Contratos del Sprint 2

La siguiente tabla vincula cada Issue de contrato del Sprint 2 con su estrategia de compatibilidad dentro de `/api/v1`:

| Issue | Título / Contrato | Operaciones en `/api/v1` | Garantía de Compatibilidad en `/api/v1` |
| :---: | :--- | :--- | :--- |
| **#86** | Implementar la API para subir y consultar evidencias asociadas a un incidente | `POST /incidentes/:incidenteId/evidencias`<br>`GET /incidentes/:incidenteId/evidencias`<br>`GET /evidencias/:id`<br>`GET /evidencias/:id/archivo` | Agrega rutas nuevas para transferencia `multipart/form-data` y descarga binaria sin alterar la creación básica `POST /incidentes`. |
| **#106** | Incorporar las evidencias disponibles a las respuestas de incidentes requeridas por la aplicación móvil | `GET /incidentes`<br>`GET /incidentes/:id`<br>`POST /incidentes` | Agrega la propiedad aditiva `evidencias: EvidenciaResponseDto[]` (devolviendo `[]` si no existen evidencias) y preserva todos los campos de `Incidente`. |
| **#114** | Implementar la API para consultar las notificaciones del usuario autenticado | `GET /notificaciones` (filtradas por usuario autenticado) | Extiende la consulta de notificaciones usando la identidad del JWT sin romper el esquema base de `Notificacion`. |
| **#117** | Implementar `PATCH /incidentes/:id/estado` con transiciones válidas y registro en historial | `PATCH /incidentes/:id/estado` | Nuevo endpoint específico para transición de estados; mantiene intactos `GET /incidentes` y `POST /incidentes`. |
| **#118** | Implementar `GET /usuarios/me` para consultar el perfil y rol del usuario autenticado | `GET /usuarios/me` | Nueva ruta dedicada a recuperar el perfil y roles del portador del JWT actual sin modificar `/auth/login` ni `/usuarios/:id`. |
| **#119** | Agregar filtros por estado, comuna y categoría a `GET /incidentes` | `GET /incidentes?estadoId=...&comuna=...&categoriaId=...` | Parámetros de consulta 100% opcionales; `GET /incidentes` sin query params o solo con `usuarioReportaId` mantiene su respuesta original. |
| **#120** | Implementar `GET /incidentes/:id/historial` | `GET /incidentes/:id/historial` | Nuevo sub-recurso de solo lectura que expone la trazabilidad de cambios de estado de un incidente. |
| **#121** | Implementar `GET /usuarios` paginado con filtro por rol | `GET /usuarios?page=...&limit=...&rol=...` | Parámetros opcionales con valores por defecto razonables para administración de usuarios. |
| **#122** | Implementar `PATCH /usuarios/:id/rol` restringido al Administrador | `PATCH /usuarios/:id/rol` | Nuevo endpoint administrativo protegido por rol `ADMIN`; no altera el registro ni el inicio de sesión. |
| **#123** | Implementar asignación y reasignación de responsables de incidentes | `PATCH /incidentes/:id/asignacion` (o sub-recurso de asignación) | Operación aditiva para vincular personal responsable a un incidente y registrar su trazabilidad. |
| **#124** | Asignar la prioridad inicial por categoría y permitir su modificación | `POST /incidentes` (cálculo automático)<br>`PATCH /incidentes/:id/prioridad` | La prioridad inicial se deriva internamente de la categoría al crear el incidente sin exigir campos obligatorios nuevos en el payload de creación de Android/Web. |
| **#125** | Ampliar el detalle de incidentes con historial y asignación activa | `GET /incidentes/:id` | Enriquecimiento aditivo del detalle de un incidente con su historial de estados y asignación vigente, conservando los atributos base. |

---

## 6. Alternativas Consideradas

### Alternativa A (Seleccionada): Mantener `/api/v1` con evolución aditiva y control de acceso RBAC
* **Ventajas**:
  * Cero fricción de integración entre los equipos de 2° año (Web/Backend) y 4° año (Android).
  * No requiere duplicar controladores en el API Gateway ni mantener dos despliegues simultáneos en el clúster Kubernetes UCT (donde existe una cuota estricta de recursos `student-provisioner-quota`).
  * La configuración de red (`PUBLIC_API_URL`, Ingress `/api(/|$)(.*)` y `BuildConfig` en Android) permanece estable.
* **Desventajas**:
  * Exige disciplina en las revisiones de Pull Requests para impedir que se introduzcan cambios destructivos en los DTOs existentes.

### Alternativa B (Descartada): Crear `/api/v2` para los endpoints del Sprint 2
* **Razones de descarte**:
  * Fragmentaría la API obligando a Android y Web a consumir simultáneamente `/api/v1` (para login o creación de incidentes) y `/api/v2` (para evidencias, filtros o estados), o bien a duplicar todo el enrutamiento del Gateway.
  * Ninguno de los requerimientos del Sprint 2 invalida la semántica ni la estructura de datos fundamental creada en el Sprint 1.

### Alternativa C (Descartada): Versionado por cabecera HTTP (`Accept-Version: v2`)
* **Razones de descarte**:
  * Añade complejidad innecesaria en el proxy del API Gateway, dificulta las pruebas manuales/Swagger y complica la caché e inspección de tráfico en NGINX Ingress.

---

## 7. Consecuencias y Criterios para Evaluar una Futura `/api/v2`

### Consecuencias Positivas
* Los clientes Android y Web continúan utilizando la base `/api/v1` sin cambios de infraestructura ni de configuración de entornos.
* La documentación interactiva y las pruebas E2E se concentran en una única superficie de contrato coherente.

### Criterios de Cambio Incompatible (Cuándo migrar a `/api/v2`)
Se deberá evaluar la apertura de un prefijo `/api/v2` únicamente si en futuros sprints se presenta alguno de los siguientes **cambios incompatibles (*breaking changes*)**:
1. **Eliminación o renombrado de campos existentes** en las respuestas JSON consumidas por producción (ej. reemplazar `ubicacion: { latitud, longitud }` por un formato GeoJSON `coordinates: [lon, lat]` sin mantener el objeto `ubicacion`).
2. **Adición de campos obligatorios sin valor por defecto en payloads de entrada existentes** (ej. exigir un nuevo campo requerido en `POST /api/v1/incidentes` que haga fallar con `400 Bad Request` a las versiones ya compiladas de la app Android).
3. **Cambio radical en el tipo de respuesta raíz** de un endpoint existente (ej. cambiar un endpoint que devuelve un arreglo plano `Incidente[]` por un objeto envolvente `{ items: Incidente[], meta: ... }` cuando no se envían parámetros de paginación explícitos).
4. **Cambio incompatible en el esquema de autenticación** que invalide el flujo actual de portador `Authorization: Bearer <JWT>`.
