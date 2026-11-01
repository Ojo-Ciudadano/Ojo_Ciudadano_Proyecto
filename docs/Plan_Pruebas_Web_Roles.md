# Plan de Pruebas Web por Rol y Matriz de Permisos (Sprint 2)

**Proyecto:** Ojo Ciudadano  
**Versión:** 1.0.0 — Sprint 2  
**Fecha:** 31 de octubre de 2026  
**Responsable:** Bernardo Sandoval  
**Tarea / Issue:** #155 — Elaborar el plan de pruebas Web por rol y la matriz de permisos  
**Estado del documento:** Planificado (Casos definidos; ejecución completa sujeta a campaña de pruebas e informe `BER-08`)

---

## Control de Versiones

| Versión | Fecha | Autor | Descripción del cambio |
| :--- | :--- | :--- | :--- |
| `1.0.0` | 2026-10-31 | Bernardo Sandoval | Versión inicial del plan de pruebas Web por rol y matriz de permisos para Sprint 2 (#155). |

---

## 1. Objetivo y Alcance

### 1.1 Objetivo
Establecer un plan de pruebas versionado y auditable para los flujos de la plataforma Web de **Ojo Ciudadano** durante el Sprint 2, junto con una matriz de permisos por endpoint y operación que permita verificar de forma consistente:
1. El control de acceso para los roles vigentes (**Ciudadano** y **Administrador**).
2. La distinción entre solicitudes sin autenticación válida (`401 Unauthorized`) y solicitudes autenticadas sin permisos suficientes (`403 Forbidden`).
3. El cumplimiento de las restricciones de propiedad sobre recursos del usuario autenticado (perfil propio y notificaciones).
4. La separación entre la protección de navegación en el cliente Web (guards de rutas) y la autorización efectiva en el backend (API Gateway y microservicios).

### 1.2 Dependencias y Trazabilidad de Issues
| Referencia | Descripción | Relación con este Plan |
| :--- | :--- | :--- |
| **Issue #100** | Implementar autorización de endpoints protegidos para los roles Ciudadano y Administrador | Define las reglas de negocio y autorización real en el backend reflejadas en la matriz. |
| **Issue #118** | Consulta de perfil real y rol vigente desde `/api/v1/usuarios/me` | Base para verificar la identidad y roles reales al iniciar sesión o recargar la Web. |
| **Issue #130** | Navegación y protección de rutas Web según rol (`ADMIN` / `CIUDADANO`) | Define el comportamiento de los guards en el frontend y el rechazo de roles desconocidos. |
| **Issue #150** | Manejar errores `401` y `403` de forma visible en la Web | Define la invalidación de sesión ante `401` y la conservación de sesión/estado ante `403`. |
| **Issue #86** | API para subir, consultar y descargar evidencias fotográficas | Define las rutas, códigos y límites de tamaño de evidencias (M-17, M-18, M-21, M-22). |
| **Issue #106** | Evidencias en las respuestas de incidentes | Define el arreglo `evidencias` en las respuestas de incidentes (M-10, M-11). |
| **Issue #116** | Matriz de transiciones de estados de incidentes | Fuente única de las transiciones permitidas y del rechazo `409` (`docs/MATRIZ_TRANSICIONES_INCIDENTES.md`). |
| **ADR-001 (#126)** | Continuidad del versionado `/api/v1` | Reglas de compatibilidad de contratos durante el Sprint 2. |
| **BER-03** | Colección Postman de pruebas de API | Insumo complementario para pruebas de contrato en backend (fuera de alcance de este documento). |
| **BER-04** | Automatización del flujo E2E | Automatizará los escenarios críticos descritos en este plan. |
| **BER-08** | Informe final de pruebas del Sprint 2 | Utilizará este plan y sus tablas de registro para contrastar la cobertura y evidencias obtenidas. |

---

## 2. Diferenciación entre Acceso a Rutas Web y Autorización Backend

En la arquitectura de **Ojo Ciudadano**, la seguridad se evalúa en dos capas independientes que **no se sustituyen entre sí**:

1. **Capa de Presentación Web (React Router + Guards de Cliente - Issues #118, #130, #150):**
   * Controla qué vistas, menús (`Navbar`) y formularios se renderizan en el navegador.
   * Verifica la existencia de sesión activa mediante `useAuth` / `useSession` y valida que el usuario posea un rol reconocido (`CIUDADANO` o `ADMIN`) confirmado por `/api/v1/usuarios/me`.
   * **Propósito:** Experiencia de usuario (UX) y prevención de navegación accidental hacia pantallas fuera del rol.
2. **Capa de Integración y Servicios Backend (API Gateway + Microservicios - Issue #100):**
   * Valida criptográficamente el token JWT (`JwtAuthGuard`), extrae la identidad (`sub`) y los roles del token, y aplica el control de acceso por rol (`RolesGuard`) y propiedad antes de ejecutar cualquier operación en base de datos.
   * **Propósito:** Seguridad efectiva. Aunque un usuario manipule el estado local del navegador para mostrar un botón administrativo, el backend rechazará la petición con `403 Forbidden` (o `401 Unauthorized` si el token no es válido).

### 2.1 Matriz de Acceso a Rutas Web (Frontend)

> Las rutas de esta tabla deben contrastarse con la configuración real del router (`config/navigation` y `App.tsx`) antes de la campaña de pruebas (ver sección 8).

| Ruta Web | Vista / Funcionalidad | Sin Sesión (Anónimo) | Rol `CIUDADANO` | Rol `ADMIN` | Rol Desconocido / Sin Rol (#130) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/login` | Inicio de sesión | Permitido (Renderiza `LoginPage`) | Redirige a la ruta inicial del rol (`getInitialRouteForRoles`, #130) | Redirige a la ruta inicial del rol (`getInitialRouteForRoles`, #130) | Limpia sesión y permanece en `/login` |
| `/` | Inicio / Feed general de incidentes | Redirige a `/login` (`ProtectedRoute`) | Permitido | Permitido | Limpia sesión y redirige a `/login` |
| `/incidentes` | Consulta y filtrado de reportes | Redirige a `/login` | Permitido (Consulta general y "Mis reportes") | Permitido (Vista de gestión completa) | Limpia sesión y redirige a `/login` |
| `/incidentes/nuevo` | Formulario de creación de reporte | Redirige a `/login` | Permitido | Permitido | Limpia sesión y redirige a `/login` |
| `/incidentes/:id` | Detalle de incidente e historial | Redirige a `/login` | Permitido (Solo lectura de detalle público/propio) | Permitido (Incluye controles de estado, prioridad y asignación) | Limpia sesión y redirige a `/login` |
| `/mapa` | Mapa territorial georreferenciado | Redirige a `/login` | Permitido | Permitido | Limpia sesión y redirige a `/login` |
| `/notificaciones` | Bandeja de notificaciones propias | Redirige a `/login` | Permitido (Solo notificaciones propias) | Permitido (Solo notificaciones propias) | Limpia sesión y redirige a `/login` |
| `/perfil` | Perfil del usuario autenticado | Redirige a `/login` | Permitido (Datos propios) | Permitido (Datos propios) | Limpia sesión y redirige a `/login` |
| `/admin` / `/admin/usuarios` | Panel de administración y gestión de roles | Redirige a `/login` | **Bloqueado por Guard de Rol** (Redirige o muestra acceso denegado sin cerrar sesión) | Permitido | Limpia sesión y redirige a `/login` |

---

## 3. Matriz de Permisos por Endpoint y Operación (Backend #100 + Web #150)

### 3.1 Criterio de Códigos HTTP de Seguridad
- **`401 Unauthorized` (Autenticación inválida):** Se produce cuando la solicitud no incluye cabecera `Authorization: Bearer <token>`, el token está mal formado, su firma no coincide o ha expirado.
  * *Efecto en la Web (#150):* Limpia `access_token` y `user` en `localStorage` (si pertenece a la sesión actual), actualiza `AuthContext`, evita alertas repetidas en ráfagas simultáneas y redirige a `/login` informando que la sesión expiró. Excepción: `POST /api/v1/auth/login` muestra error de credenciales sin redirigir.
- **`403 Forbidden` (Permisos insuficientes):** Se produce cuando el token JWT es válido, pero el rol del usuario no está autorizado para el endpoint o intenta acceder/modificar un recurso ajeno.
  * *Efecto en la Web (#150):* **Conserva la sesión activa**, muestra una notificación visible de falta de permisos y rechaza la promesa para que la pantalla mantenga el último dato confirmado sin reintentar escrituras.
- **`409 Conflict` (Transición de estado prohibida):** Se produce cuando un `ADMIN` solicita un cambio de estado que la matriz de `docs/MATRIZ_TRANSICIONES_INCIDENTES.md` (#116) prohíbe, incluido pasar al mismo estado. El incidente no se modifica ni se registra un cambio en el historial.

### 3.2 Matriz Detallada de Endpoints (`/api/v1`)

| ID | Servicio | Método y Ruta (`/api/v1/...`) | Operación | Sin Sesión (Anónimo) | Rol `CIUDADANO` | Rol `ADMIN` | Restricción de Propiedad / Regla de Negocio | Issue / Cobertura |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **M-01** | Auth | `POST /auth/register` | Registro de cuenta ciudadana | `201 Created` (`400` inválido, `409` duplicado) | `201 Created` | `201 Created` | Asigna rol `CIUDADANO` por defecto; no expone `passwordHash`. | Sprint 1 / #100 |
| **M-02** | Auth | `POST /auth/login` | Inicio de sesión y emisión de JWT | `200 OK` (`401` credenciales inválidas) | `200 OK` | `200 OK` | En `401` la Web mantiene al usuario en `/login` sin bucle (#150). | Sprint 1 / #150 |
| **M-03** | Usuarios | `GET /usuarios/me` | Obtener perfil y roles vigentes | `401 Unauthorized` | `200 OK` (Propio) | `200 OK` (Propio) | **Propiedad estricta:** Resuelve exclusivamente el usuario del `sub` del JWT (#118). | #118, #130 |
| **M-04** | Usuarios | `GET /usuarios` | Listar usuarios (paginado, filtro opcional `rol`) | `401 Unauthorized` | `403 Forbidden` | `200 OK` (`400` parámetros inválidos) | Exclusivo para administración de cuentas; respuesta `{ data, meta }` sin `passwordHash`. | #121, #100 |
| **M-05** | Usuarios | `PATCH /usuarios/:id/rol` | Modificar rol de un usuario (`CIUDADANO` / `ADMIN`) | `401 Unauthorized` | `403 Forbidden` | `200 OK` (auto-degradación: **por definir en #122**) | Cambio de rol restringido al `ADMIN`. | #122 |
| **M-06** | Roles | `GET /roles` | Consultar catálogo de roles | `401 Unauthorized` | `403 Forbidden` | `200 OK` | Catálogo consolidado: `ADMIN` y `CIUDADANO`. | #100 |
| **M-07** | Categorías | `GET /categorias` | Listar categorías de incidentes | `401 Unauthorized` | `200 OK` | `200 OK` | Requerido para formulario de creación y filtros en Web. | #100 |
| **M-08** | Categorías | `POST /categorias` | Crear nueva categoría | `401 Unauthorized` | `403 Forbidden` | `201 Created` | Solo `ADMIN` puede administrar el catálogo de categorías. | #100 |
| **M-09** | Categorías | `PATCH /categorias/:id` | Actualizar categoría existente | `401 Unauthorized` | `403 Forbidden` | `200 OK` | Solo `ADMIN`. | #100 |
| **M-10** | Incidentes | `GET /incidentes` | Consultar listado de incidentes (general y filtrado) | `401 Unauthorized` | `200 OK` | `200 OK` | Soporta filtro `?usuarioReportaId=<uuid>` para vista "Mis reportes". Cada incidente incluye `evidencias` (arreglo vacío si no tiene). | #100, #106 |
| **M-11** | Incidentes | `GET /incidentes/:id` | Consultar detalle de un incidente | `401 Unauthorized` | `200 OK` (`404` inexistente) | `200 OK` (`404` inexistente) | Incluye categoría, estado actual, ubicación y `evidencias` asociadas (metadata y `url` relativa, sin datos de Storage). | #100, #106 |
| **M-12** | Incidentes | `POST /incidentes` | Crear un nuevo reporte de incidente | `401 Unauthorized` | `201 Created` (`400` DTO inválido) | `201 Created` (`400` DTO inválido) | **Propiedad:** el reporte debe quedar vinculado al usuario autenticado y con estado inicial `Nuevo` (`orden: 1`). Verificación específica en `CP-NEG-07`. | #100 |
| **M-13** | Incidentes | `PATCH /incidentes/:id/estado` | Cambiar estado del incidente | `401 Unauthorized` | `403 Forbidden` | `200 OK` (`409` transición prohibida) | **Regla de negocio:** solo transiciones de `docs/MATRIZ_TRANSICIONES_INCIDENTES.md` (#116); el cambio exitoso registra historial y el rechazo `409` no. | #116, #117 |
| **M-14** | Incidentes | `PATCH /incidentes/:id/prioridad` | Modificar prioridad de un incidente | `401 Unauthorized` | `403 Forbidden` | `200 OK` (`400` fuera de rango) | Solo `ADMIN` puede recalificar la prioridad operativa. | #124 |
| **M-15** | Incidentes | `PATCH /incidentes/:id/asignacion` | Asignar o reasignar responsable | `401 Unauthorized` | `403 Forbidden` | `200 OK` (responsable inválido: **por definir en #123**) | El responsable asignado debe existir y ser válido para gestión. | #123 |
| **M-16** | Incidentes | `GET /incidentes/:id/historial` | Consultar historial de cambios de estado/asignación | `401 Unauthorized` | **Por definir en #120** | `200 OK` | Permite auditar quién realizó cada transición y cuándo. | #120 |
| **M-17** | Evidencias | `POST /incidentes/:incidenteId/evidencias` (alias `POST /evidencias`) | Subir fotografía de evidencia (`multipart/form-data`) | `401 Unauthorized` (\*) | `201 Created` (`400` tipo no permitido, `404` incidente inexistente, `413` > 10 MB) | `201 Created` (`400`, `404`, `413`) | **Regla negativa:** máximo funcional **10 MB**; formatos `image/jpeg`, `image/png`, `image/webp`; el transporte admite hasta 12 MB. | #86 |
| **M-18** | Evidencias | `GET /incidentes/:incidenteId/evidencias` (alias `GET /evidencias?incidenteId=`) | Listar evidencias de un incidente | `401 Unauthorized` (\*) | `200 OK` (`404` incidente inexistente) | `200 OK` (`404` incidente inexistente) | Retorna solo metadata (`id`, `incidenteId`, `tipoMime`, `tamanoArchivo`, `fechaCreacion`) y `url` relativa a la API, ordenada por `fechaCreacion`. **No** incluye `claveArchivo` ni URLs de Supabase Storage. | #86, #106 |
| **M-19** | Notificaciones | `GET /notificaciones` | Consultar notificaciones del usuario | `401 Unauthorized` | `200 OK` (Solo propias) | `200 OK` (Solo propias) | **Propiedad estricta:** solo retorna notificaciones cuyo destinatario sea el usuario autenticado. | #114 |
| **M-20** | Notificaciones | `PATCH /notificaciones/:id/leida` | Marcar notificación como leída | `401 Unauthorized` | `200 OK` (Propia) / `403 Forbidden` (Ajena) | `200 OK` (Propia) / `403 Forbidden` (Ajena) | **Propiedad estricta.** **Por confirmar:** este endpoint no figura en las Issues del Sprint 2 consultadas; si no existe, los casos asociados quedan `BLOQUEADO`. | Por confirmar |
| **M-21** | Evidencias | `GET /evidencias/:id/archivo` | Descargar el archivo de una evidencia | `401 Unauthorized` (\*) | `200 OK` (binario con su `Content-Type`; `404` evidencia o archivo inexistente; `502` fallo de Storage) | Igual que `CIUDADANO` | La API transmite el archivo; el cliente nunca accede a Supabase Storage. | #86 |
| **M-22** | Evidencias | `GET /evidencias/:id` | Consultar la metadata de una evidencia | `401 Unauthorized` (\*) | `200 OK` (`404` inexistente) | `200 OK` (`404` inexistente) | Mismo formato público que M-18. | #86 |

(\*) Pendiente de confirmar que #86 exige autenticación en los endpoints de evidencias (ver sección 8).

---

## 4. Restricciones de Propiedad de Recursos

Para evitar vulnerabilidades de tipo *Broken Object Level Authorization (BOLA / IDOR)*, el plan verifica las siguientes reglas de propiedad:

1. **Perfil Propio (`GET /api/v1/usuarios/me` - Issue #118):**
   * La identidad nunca se recibe por parámetro de query o body; el backend la obtiene exclusivamente del claim `sub` del token JWT verificado.
   * Si el usuario cambia de rol en base de datos, al recargar la Web o invocar `refreshProfile()` se refleja el rol vigente.
2. **Notificaciones Personales (`GET /api/v1/notificaciones` y, si existe, `PATCH /api/v1/notificaciones/:id/leida`):**
   * Tanto `CIUDADANO` como `ADMIN` solo pueden visualizar y marcar como leídas las notificaciones dirigidas a su propio `usuarioId`.
   * Cualquier intento de modificar por ID una notificación perteneciente a otro usuario debe responder `403 Forbidden`.
3. **Autoría de Reportes (`POST /api/v1/incidentes` y filtro `Mis reportes`):**
   * Cuando un `CIUDADANO` crea un incidente desde la Web, el `usuarioReportaId` debe corresponder a su sesión autenticada (se verifica en `CP-NEG-07`).
   * En la pestaña/chip **"Mis reportes"**, la consulta filtra por el ID del usuario en sesión, diferenciándola del listado público comunal.

---

## 5. Catálogo de Casos de Prueba Web por Flujo y Rol

> **Nota sobre el estado de los casos:** Todos los casos de esta sección se encuentran en estado **`DISEÑADO / PENDIENTE DE EJECUCIÓN MANUAL`** o **`PENDIENTE DE IMPLEMENTACIÓN DE PANTALLA`**, salvo aquellos cuya lógica base de cliente HTTP ya cuenta con cobertura unitaria automatizada (`#150`). Ningún caso debe reportarse como ejecutado sin adjuntar su evidencia real en el informe `BER-08`.

### 5.1 Flujo A: Autenticación, Sesión, Perfil Propio y Manejo 401/403

#### Caso CP-WEB-01: Inicio de sesión exitoso y carga de perfil real (`/usuarios/me`)
- **Rol:** `CIUDADANO` y `ADMIN`
- **Issues vinculadas:** #118, #130
- **Cobertura automatizada:** Parcial en `verify-login.ts` y unitarias de `AuthService`.
- **Estado:** Diseñado (Pendiente de ejecución integrada)
- **Prerrequisitos:** Servicios `api-gateway` (`3000`) y `auth-usuarios` (`3001`) activos; cuentas semilla `ciudadano@ojociudadano.cl` y `admin@ojociudadano.cl` creadas.
- **Datos de prueba:** Credenciales válidas de Ciudadano y Administrador.
- **Pasos:**
  1. Abrir `/login` en el navegador sin sesión previa.
  2. Ingresar correo y contraseña válidos y enviar el formulario.
  3. Observar las peticiones de red en DevTools (`POST /api/v1/auth/login` seguido de `GET /api/v1/usuarios/me`).
- **Resultado esperado:**
  * `POST /api/v1/auth/login` retorna `200 OK` con `access_token`.
  * `GET /api/v1/usuarios/me` retorna `200 OK` con `id`, `nombre`, `email` y `roles` vigentes.
  * El usuario es redirigido a la ruta inicial acorde a su rol y la barra lateral (`Navbar`) muestra su nombre, rol y opciones permitidas.

#### Caso CP-WEB-02: Credenciales inválidas en login (`401`) sin bucle de redirección
- **Rol:** Sin sesión (Anónimo)
- **Issues vinculadas:** #150
- **Cobertura automatizada:** **Cubierto** en `web/frontend/src/tests/auth-permission-errors.test.ts` (*"diferencia un 401 de /auth/login manteniendo el mensaje de credenciales sin disparar cierre de sesión"*).
- **Estado:** Automatizado a nivel de cliente HTTP / Pendiente verificación visual E2E
- **Prerrequisitos:** Estar en la vista `/login`.
- **Datos de prueba:** `email: "admin@ojociudadano.cl"`, `password: "ClaveIncorrecta999"`.
- **Pasos:**
  1. Ingresar las credenciales incorrectas en `/login` y presionar **Iniciar sesión**.
- **Resultado esperado:**
  * El backend responde `401 Unauthorized`.
  * El interceptor de `apiClient` no dispara el evento `onSessionInvalidated` ni recarga la ruta `/login`.
  * Se muestra en pantalla la alerta de error *"Credenciales inválidas"* sin perder el correo escrito en el formulario.

#### Caso CP-WEB-03: Expiración o invalidación de sesión (`401`) en ruta protegida y deduplicación simultánea
- **Rol:** `CIUDADANO` / `ADMIN`
- **Issues vinculadas:** #150
- **Cobertura automatizada:** **Cubierto** en `web/frontend/src/tests/auth-permission-errors.test.ts` (casos de `401` individual, `401` simultáneos y aislamiento entre sesión anterior y nueva).
- **Estado:** Automatizado a nivel de cliente HTTP / Pendiente verificación visual E2E
- **Prerrequisitos:** Sesión iniciada en la Web; token local adulterado o expirado.
- **Datos de prueba:** Token inválido en `localStorage.setItem('access_token', 'token-expirado')`.
- **Pasos:**
  1. Con la sesión abierta en `/`, reemplazar el `access_token` en `localStorage` por un valor inválido.
  2. Disparar una acción que ejecute una o más solicitudes a endpoints protegidos (`/api/v1/incidentes`, `/api/v1/categorias`).
- **Resultado esperado:**
  * Las peticiones reciben `401 Unauthorized`.
  * El cliente HTTP limpia `access_token` y `user` de `localStorage` **una sola vez**, sin emitir notificaciones ni redirecciones duplicadas.
  * `ProtectedRoute` bloquea el contenido protegido y redirige a `/login` mostrando el mensaje comprensible de sesión expirada.

---

### 5.2 Flujo B: Consulta, Creación de Reportes, Ubicación y Fotografías (Rol Ciudadano)

#### Caso CP-WEB-04: Consulta de incidentes generales y filtro "Mis reportes"
- **Rol:** `CIUDADANO`
- **Issues vinculadas:** #100, #106
- **Cobertura automatizada:** Previsto en `BER-04`.
- **Estado:** Diseñado (Pendiente de ejecución)
- **Prerrequisitos:** Sesión iniciada como `CIUDADANO`; al menos 2 incidentes en BD (uno creado por el ciudadano actual y otro por un usuario distinto).
- **Datos de prueba:** Filtro `Todos` vs filtro `Mis reportes` (`usuarioReportaId`).
- **Pasos:**
  1. Navegar a la vista de inicio `/` o `/incidentes`.
  2. Verificar que en el filtro `Todos` se listan los incidentes de la comuna.
  3. Seleccionar el filtro **`Mis reportes`**.
- **Resultado esperado:**
  * `GET /api/v1/incidentes` retorna `200 OK` y cada incidente incluye el arreglo `evidencias` (vacío si no tiene).
  * Al activar `Mis reportes`, se muestran únicamente los incidentes cuyo `usuarioReportaId` coincide con el `id` del ciudadano autenticado.

#### Caso CP-WEB-05: Creación exitosa de reporte con ubicación válida y fotografía (`<= 10 MB`)
- **Rol:** `CIUDADANO`
- **Issues vinculadas:** #100, #86, #106
- **Cobertura automatizada:** Previsto en `BER-04`.
- **Estado:** Diseñado (Pendiente de integración completa de formulario Web)
- **Prerrequisitos:** Sesión activa como `CIUDADANO`; categorías cargadas desde `GET /api/v1/categorias`; permisos de geolocalización concedidos en el navegador (o selección manual en el mapa).
- **Datos de prueba:**
  * `titulo`: `"Luminaria apagada en Av. Alemania"`
  * `descripcion`: `"Poste sin luz frente al campus universitario desde hace dos noches."`
  * `categoria`: `"Alumbrado público"`
  * `ubicacion`: `latitud: -38.7359`, `longitud: -72.5904`, `comuna: "Temuco"`, `region: "La Araucanía"`, `sector: "Poniente"`
  * `archivo`: `evidencia_poste.jpg` (`2.4 MB`, formato `image/jpeg`)
- **Pasos:**
  1. Hacer clic en **+ Nuevo reporte**.
  2. Completar título, descripción y seleccionar categoría.
  3. Obtener ubicación actual del navegador o fijar el pin en el mapa.
  4. Adjuntar la fotografía `evidencia_poste.jpg` (`2.4 MB`) y enviar el formulario.
- **Resultado esperado:**
  * `POST /api/v1/incidentes` responde `201 Created` asignando estado inicial `Nuevo` (`orden: 1`).
  * `POST /api/v1/incidentes/:incidenteId/evidencias` responde `201 Created`; la imagen queda en Supabase Storage y su metadata queda asociada al incidente.
  * La respuesta de la evidencia contiene metadata y la `url` relativa `/evidencias/:id/archivo`, sin `claveArchivo` ni URLs de Storage.
  * La Web muestra notificación de éxito y el reporte aparece en el listado.

#### Caso CP-WEB-06: Visualización de incidentes en el Mapa Territorial
- **Rol:** `CIUDADANO` y `ADMIN`
- **Issues vinculadas:** #100
- **Cobertura automatizada:** Previsto en `BER-04`.
- **Estado:** Diseñado (Pendiente de integración cartográfica React-Leaflet)
- **Prerrequisitos:** Sesión iniciada; incidentes con coordenadas válidas registrados.
- **Pasos:**
  1. Ingresar a `/mapa` desde el menú lateral.
  2. Filtrar por categoría o estado y seleccionar un marcador (pin) en el mapa.
- **Resultado esperado:**
  * Se renderiza la capa base de OpenStreetMap con los marcadores en las coordenadas `(latitud, longitud)` de cada incidente.
  * Al hacer clic en un marcador, se despliega el resumen del incidente (categoría, título, estado y comuna).

---

### 5.3 Flujo C: Gestión Administrativa de Incidentes y Usuarios (Rol Administrador)

#### Caso CP-WEB-07: Transición válida de estado, cambio de prioridad y asignación de responsable
- **Rol:** `ADMIN`
- **Issues vinculadas:** #116, #117, #123, #124
- **Cobertura automatizada:** Previsto en `BER-04`.
- **Estado:** Diseñado (Pendiente de pantallas de detalle administrativo Web)
- **Prerrequisitos:** Sesión iniciada con rol `ADMIN`; incidente existente en estado `Nuevo`.
- **Datos de prueba:**
  * Transición de estado: `Nuevo` ➔ `En revisión` (única transición permitida desde `Nuevo`, según #116).
  * Nueva prioridad: `4` (Alta).
  * Responsable asignado: UUID de un usuario administrador válido.
- **Pasos:**
  1. Abrir el detalle del incidente en la vista administrativa.
  2. Cambiar el estado a `En revisión`, actualizar la prioridad a `4` y asignar al responsable válido.
  3. Consultar la sección de **Historial de cambios** del incidente.
- **Resultado esperado:**
  * Las peticiones `PATCH` a `/api/v1/incidentes/:id/estado`, `/prioridad` y `/asignacion` responden `200 OK`.
  * La interfaz actualiza los datos confirmados y el historial (`GET /api/v1/incidentes/:id/historial`) registra el cambio con fecha y usuario ejecutor.

#### Caso CP-WEB-08: Consulta de usuarios y cambio de rol de un tercero por Administrador
- **Rol:** `ADMIN`
- **Issues vinculadas:** #121, #122, #130
- **Cobertura automatizada:** Unitarias en `roles.service.spec.ts` / `usuarios.service.spec.ts`.
- **Estado:** Diseñado (Pendiente de vista `/admin` en Web)
- **Prerrequisitos:** Sesión iniciada como `ADMIN`; existencia de un usuario secundario con rol `CIUDADANO`.
- **Datos de prueba:** `usuarioId` de la cuenta ciudadana secundaria; nuevo rol: `ADMIN`.
- **Pasos:**
  1. Acceder desde el menú lateral a **Administración ➔ Usuarios y reglas** (`/admin`).
  2. Localizar al usuario secundario y cambiar su rol de `CIUDADANO` a `ADMIN`.
- **Resultado esperado:**
  * `GET /api/v1/usuarios` retorna `200 OK` con `{ data, meta }`, sin `passwordHash`.
  * `PATCH /api/v1/usuarios/:id/rol` retorna `200 OK` actualizando el rol del usuario seleccionado.

---

### 5.4 Flujo D: Notificaciones y Restricciones de Propiedad

#### Caso CP-WEB-09: Consulta y lectura de notificaciones propias del usuario autenticado
- **Rol:** `CIUDADANO` / `ADMIN`
- **Issues vinculadas:** #114
- **Cobertura automatizada:** Unitarias del microservicio `notificaciones`.
- **Estado:** Diseñado. La consulta (pasos 1) está pendiente de integración en la bandeja Web; el marcado como leída (pasos 2 y 3) queda `BLOQUEADO` hasta confirmar que el endpoint existe (M-20).
- **Prerrequisitos:** Usuario autenticado con al menos una notificación no leída asociada a su `usuarioId`, y otra notificación en BD perteneciente a un usuario distinto (`usuarioAjenoId`).
- **Datos de prueba:** `notificacionPropiaId` y `notificacionAjenaId`.
- **Pasos:**
  1. Abrir el panel de notificaciones en la Web (`GET /api/v1/notificaciones`).
  2. *(Si M-20 existe)* Marcar como leída `notificacionPropiaId` (`PATCH /api/v1/notificaciones/:id/leida`).
  3. *(Si M-20 existe)* Intentar marcar como leída `notificacionAjenaId` mediante solicitud autenticada con el token actual.
- **Resultado esperado:**
  * El listado contiene únicamente las notificaciones del usuario autenticado (`200 OK`).
  * Marcar `notificacionPropiaId` responde `200 OK` y cambia su estado a `leida: true`.
  * Intentar modificar `notificacionAjenaId` responde **`403 Forbidden`** por violación de propiedad, manteniendo la sesión activa (#150).

---

## 6. Casos de Prueba Negativos y de Borde (Obligatorios)

#### Caso CP-NEG-01: Rechazo `403 Forbidden` cuando un Ciudadano intenta ejecutar una operación de Administrador (conservando sesión)
- **Rol:** `CIUDADANO`
- **Issues vinculadas:** #100, #150
- **Cobertura automatizada:** **Parcial** en `web/frontend/src/tests/auth-permission-errors.test.ts` (*"conserva la sesión activa ante un 403..."*). La prueba verifica que `access_token` y `user` permanecen intactos, que se emite un solo aviso de permisos y que la escritura no se reintenta. La conservación del estado visual de la pantalla se verifica manualmente.
- **Estado:** Automatizado en cliente HTTP / Pendiente prueba integrada
- **Prerrequisitos:** Sesión válida con rol `CIUDADANO`; incidente en estado `Nuevo`.
- **Datos de prueba:** `PATCH /api/v1/incidentes/<id>/estado` con `{ "estado": "En revisión" }`.
- **Pasos:**
  1. Con sesión de `CIUDADANO`, disparar la solicitud de cambio de estado (o acceso a endpoint administrativo).
- **Resultado esperado:**
  * El backend rechaza la solicitud con **`403 Forbidden`**.
  * El cliente Web **no cierra la sesión** (`access_token` y `user` permanecen intactos en `localStorage` y `AuthContext`).
  * Se muestra una notificación visible de error indicando falta de permisos.
  * No se reintenta la escritura automáticamente y la pantalla conserva el estado confirmado anterior (`Nuevo`).

#### Caso CP-NEG-02: Transición prohibida de estado en un incidente
- **Rol:** `ADMIN`
- **Issues vinculadas:** #116, #117
- **Cobertura automatizada:** Previsto en pruebas de servicio de `incidentes`.
- **Estado:** Diseñado (Pendiente de ejecución integrada; depende de #117)
- **Prerrequisitos:** Sesión iniciada como `ADMIN`; un incidente en estado `Nuevo` y otro en estado `Cerrado`.
- **Datos de prueba:**
  * Variante A: `Nuevo` ➔ `Resuelto` (salto de estados).
  * Variante B: `Cerrado` ➔ `En revisión` (`Cerrado` es terminal).
  * Variante C: `Nuevo` ➔ `Nuevo` (mismo estado).
- **Pasos:**
  1. Enviar `PATCH /api/v1/incidentes/:id/estado` con la transición de cada variante.
  2. Consultar `GET /api/v1/incidentes/:id/historial` del incidente.
- **Resultado esperado:**
  * El backend responde **`409 Conflict`** en las tres variantes (matriz de #116).
  * El incidente conserva su estado en la base de datos y en la interfaz Web.
  * El historial no registra ningún cambio exitoso por la solicitud rechazada.

#### Caso CP-NEG-03: Intento de auto-degradación de rol por un Administrador
- **Rol:** `ADMIN`
- **Issues vinculadas:** #122
- **Cobertura automatizada:** Previsto en pruebas de `usuarios` / `roles`.
- **Estado:** Diseñado (Pendiente de ejecución integrada). Aplica solo si #122 incluye esta regla; de lo contrario se descarta (ver sección 8).
- **Prerrequisitos:** Sesión iniciada como `ADMIN` (`id = adminActualId`).
- **Datos de prueba:** `PATCH /api/v1/usuarios/<adminActualId>/rol` con `{ "rol": "CIUDADANO" }`.
- **Pasos:**
  1. En la vista de administración de usuarios (`/admin`), localizar la fila correspondiente al propio usuario administrador en sesión.
  2. Intentar cambiar su propio rol de `ADMIN` a `CIUDADANO` (en la UI el selector propio debe estar deshabilitado; si se fuerza la petición al backend, evaluar respuesta).
- **Resultado esperado:**
  * El backend rechaza la solicitud (código **por definir en #122**) indicando que un administrador no puede degradar su propio rol.
  * El usuario mantiene su rol `ADMIN` intacto.

#### Caso CP-NEG-04: Asignación de incidente a un responsable no válido
- **Rol:** `ADMIN`
- **Issues vinculadas:** #123
- **Cobertura automatizada:** Previsto en pruebas de `incidentes`.
- **Estado:** Diseñado (Pendiente de ejecución integrada)
- **Prerrequisitos:** Sesión iniciada como `ADMIN`; incidente activo.
- **Datos de prueba:**
  * Variante A: `responsableId: "00000000-0000-0000-0000-000000000000"` (UUID inexistente).
  * Variante B: `responsableId` perteneciente a un usuario con rol exclusivo `CIUDADANO` sin facultades de gestión.
- **Pasos:**
  1. Enviar `PATCH /api/v1/incidentes/:id/asignacion` con el `responsableId` inválido.
- **Resultado esperado:**
  * El backend rechaza la asignación. Códigos **por definir en #123** (propuesta: variante A `404 Not Found`, variante B `400 Bad Request`).
  * El incidente no modifica su asignación previa y la Web informa el motivo del rechazo.

#### Caso CP-NEG-05: Permiso de ubicación denegado en el navegador al crear reporte
- **Rol:** `CIUDADANO`
- **Issues vinculadas:** Flujo de geolocalización Web
- **Cobertura automatizada:** Previsto en pruebas de componente de formulario.
- **Estado:** Diseñado (Pendiente de implementación de captura GPS/Mapa en creación Web)
- **Prerrequisitos:** Sesión iniciada como `CIUDADANO`; bloquear el permiso de ubicación (`Location: Block`) en la configuración del navegador para `localhost:5173`.
- **Datos de prueba:** Formulario con título, descripción y categoría completos, pero sin coordenadas seleccionadas.
- **Pasos:**
  1. Abrir el formulario de creación de reporte y presionar **Usar mi ubicación actual**.
  2. Intentar enviar el reporte sin haber seleccionado un punto manual alternativo en el mapa.
- **Resultado esperado:**
  * Al denegarse `navigator.geolocation`, la interfaz muestra un aviso claro indicando que el permiso de ubicación fue denegado y habilita la selección manual en el mapa/dirección.
  * Si el usuario intenta enviar el formulario sin coordenadas válidas, la validación impide el envío (`POST /api/v1/incidentes` no se dispara o el DTO rechaza con `400 Bad Request`).

#### Caso CP-NEG-06: Carga de fotografía de evidencia superior a 10 MB
- **Rol:** `CIUDADANO` / `ADMIN`
- **Issues vinculadas:** #86
- **Cobertura automatizada:** Previsto en pruebas de `evidencias`.
- **Estado:** Diseñado (Pendiente de ejecución integrada)
- **Prerrequisitos:** Sesión activa en el formulario de creación o detalle de reporte; un incidente existente.
- **Datos de prueba:**
  * Variante A: `foto_pesada_11mb.png` de **11 MB** (supera el límite funcional de 10 MB, pero cabe en el transporte de 12 MB).
  * Variante B: `foto_pesada_4k.png` de **12.5 MB** (supera también el límite de transporte de 12 MB).
- **Pasos:**
  1. Seleccionar el archivo en el campo de carga de fotografías del reporte.
  2. Intentar subir la evidencia.
  3. Repetir enviando el archivo directamente a `POST /api/v1/incidentes/:incidenteId/evidencias`.
- **Resultado esperado:**
  * El validador del formulario Web bloquea la carga antes del envío mostrando un mensaje claro: *"El archivo supera el tamaño máximo permitido de 10 MB"*.
  * Enviado directamente al endpoint, el backend responde **`413 Payload Too Large`** en ambas variantes (la variante A la rechaza el servicio de evidencias; la B, el límite de transporte del gateway o el ingress) y no almacena nada en Supabase Storage ni crea metadata.

#### Caso CP-NEG-07: Creación de un reporte a nombre de otro usuario
- **Rol:** `CIUDADANO`
- **Issues vinculadas:** #100
- **Cobertura automatizada:** Previsto en pruebas de `incidentes`.
- **Estado:** Diseñado (Pendiente de verificación; ver sección 8)
- **Prerrequisitos:** Sesión iniciada como `CIUDADANO` A; existencia de un segundo usuario `CIUDADANO` B.
- **Datos de prueba:** `POST /api/v1/incidentes` con un cuerpo válido, pero con `usuarioReportaId` igual al id del usuario B.
- **Pasos:**
  1. Con el token del usuario A, enviar la solicitud con el id del usuario B.
  2. Consultar `GET /api/v1/incidentes?usuarioReportaId=<id de A>` y `<id de B>`.
- **Resultado esperado:**
  * El incidente **no** debe quedar atribuido al usuario B: el backend toma la autoría del usuario autenticado (token) o rechaza la solicitud. El criterio exacto (ignorar el valor o rechazar con `403`) queda **por definir**.
  * Si el incidente queda a nombre de B, el caso se reprueba y se registra un defecto de severidad `Crítica` (vulnerabilidad de permisos).

---

## 7. Procedimiento de Registro de Resultados, Evidencias y Defectos

Para garantizar trazabilidad en el **Informe Final del Sprint 2 (`BER-08`)** y evitar confundir casos meramente planificados con casos efectivamente probados, toda ejecución deberá registrarse bajo las siguientes reglas:

### 7.1 Estados Permitidos de un Caso de Prueba
- **`PENDIENTE`**: Caso diseñado en este plan pero aún no ejecutado contra el entorno integrado.
- **`BLOQUEADO (FALTA IMPLEMENTACIÓN)`**: La pantalla Web o el endpoint específico aún no ha sido fusionado en `integration`.
- **`BLOQUEADO (ENTORNO)`**: Los contenedores Docker, bases de datos o servicios no están disponibles.
- **`AUTOMATIZADO (UNIT)`**: La lógica del caso está cubierta por una prueba automatizada en el repositorio que se ejecutó con éxito. Verifica el comportamiento del cliente o del servicio con dependencias simuladas, por lo que **no** reemplaza la ejecución integrada contra los servicios reales.
- **`APROBADO`**: Ejecutado contra los servicios reales; tanto la respuesta HTTP como el comportamiento visual en la Web coinciden con el resultado esperado y cuentan con evidencia.
- **`REPROBADO`**: Ejecutado, pero el código HTTP, la persistencia o la interfaz Web difieren del resultado esperado (requiere abrir registro de defecto).

### 7.2 Plantilla de Matriz de Ejecución (para completar en `BER-08`)

| ID Caso | Rol Probado | Estado Actual | Código HTTP Observado | Fecha Ejecución | Evidencia (Captura / Log / Test) | ID Defecto |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `CP-WEB-01` | `CIUDADANO` / `ADMIN` | `PENDIENTE` | — | — | — | — |
| `CP-WEB-02` | Anónimo | `AUTOMATIZADO (UNIT)` | `401` | 2026-10-30 | `auth-permission-errors.test.ts` (Pass) | — |
| `CP-WEB-03` | `CIUDADANO` | `AUTOMATIZADO (UNIT)` | `401` | 2026-10-30 | `auth-permission-errors.test.ts` (Pass) | — |
| `CP-WEB-04` | `CIUDADANO` | `PENDIENTE` | — | — | — | — |
| `CP-WEB-05` | `CIUDADANO` | `BLOQUEADO (FALTA IMPLEMENTACIÓN)` | — | — | — | — |
| `CP-WEB-06` | `CIUDADANO` / `ADMIN` | `BLOQUEADO (FALTA IMPLEMENTACIÓN)` | — | — | — | — |
| `CP-WEB-07` | `ADMIN` | `BLOQUEADO (FALTA IMPLEMENTACIÓN)` | — | — | — | — |
| `CP-WEB-08` | `ADMIN` | `BLOQUEADO (FALTA IMPLEMENTACIÓN)` | — | — | — | — |
| `CP-WEB-09` | `CIUDADANO` / `ADMIN` | `PENDIENTE` (consulta) / `BLOQUEADO (FALTA IMPLEMENTACIÓN)` (marcar leída) | — | — | — | — |
| `CP-NEG-01` | `CIUDADANO` | `AUTOMATIZADO (UNIT)` (parcial) | `403` | 2026-10-30 | `auth-permission-errors.test.ts` (Pass) | — |
| `CP-NEG-02` | `ADMIN` | `PENDIENTE` | — | — | — | — |
| `CP-NEG-03` | `ADMIN` | `PENDIENTE` | — | — | — | — |
| `CP-NEG-04` | `ADMIN` | `PENDIENTE` | — | — | — | — |
| `CP-NEG-05` | `CIUDADANO` | `BLOQUEADO (FALTA IMPLEMENTACIÓN)` | — | — | — | — |
| `CP-NEG-06` | `CIUDADANO` | `PENDIENTE` | — | — | — | — |
| `CP-NEG-07` | `CIUDADANO` | `PENDIENTE` | — | — | — | — |

> Los registros `AUTOMATIZADO (UNIT)` solo son válidos si el archivo de pruebas citado está fusionado en `integration`. Al completar `BER-08`, anotar el commit contra el que se ejecutó.

### 7.3 Plantilla de Reporte de Defectos / Inconsistencias

Cuando un caso resulte **`REPROBADO`**, se documentará en el informe de pruebas con el siguiente formato:

| Campo | Detalle a registrar |
| :--- | :--- |
| **ID Defecto** | `DEF-S2-XX` |
| **Caso Asociado** | Ej. `CP-NEG-03` |
| **Severidad** | `Crítica` (vulnerabilidad de permisos / caída), `Alta` (flujo bloqueado), `Media` (mensaje/UI inconsistente), `Baja` (cosmético). |
| **Ruta Web / Endpoint** | Ej. `/admin` — `PATCH /api/v1/usuarios/:id/rol` |
| **Rol en Sesión** | `CIUDADANO` / `ADMIN` / Sin sesión |
| **Resultado Esperado** | Código HTTP y comportamiento en pantalla definidos en este plan. |
| **Resultado Obtenido** | Código HTTP real recibido, payload JSON de respuesta y estado en que quedó la pantalla. |
| **Evidencia Adjunta** | Captura de pantalla de la interfaz + pestaña *Network* de DevTools o salida de terminal. |

---

## 8. Pendientes de Verificación antes de la Campaña de Pruebas

Los siguientes puntos dependen del estado de `integration` al momento de ejecutar la campaña y deben confirmarse antes de pasar los casos de `PENDIENTE` a su ejecución:

1. **`PATCH /notificaciones/:id/leida` (M-20, CP-WEB-09):** confirmar que el endpoint existe. La Issue #114 cubre solo la consulta. Si no existe, los pasos 2 y 3 de CP-WEB-09 quedan `BLOQUEADO`.
2. **Autoría en `POST /incidentes` (M-12, CP-NEG-07):** confirmar si `usuarioReportaId` se toma del token o del cuerpo de la solicitud. Si proviene del cuerpo, registrar un defecto `Crítica`.
3. **Autenticación en evidencias (M-17, M-18, M-21, M-22):** confirmar que #86 exige token JWT; de lo contrario, ajustar la columna "Sin Sesión".
4. **Resultados por definir:** `#120` (M-16, visibilidad del historial para `CIUDADANO`), `#122` (M-05 y CP-NEG-03; confirmar además si la regla de auto-degradación forma parte de la Issue) y `#123` (M-15 y CP-NEG-04).
5. **Dependencias de implementación:** `#117` (endpoint de estado) para M-13, CP-NEG-01 y CP-NEG-02; fusión de `#150` para citar su archivo de pruebas.
6. **Rutas Web (sección 2.1):** contrastar con `config/navigation` y `App.tsx`.