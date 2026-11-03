# Colección Postman — Ojo Ciudadano (Sprint 2)

Colección versionada de Postman y plantilla de entorno para validar los contratos del API Gateway (`/api/v1`) y los microservicios de `auth-usuarios`, `incidentes` y `notificaciones` correspondientes al Sprint 2 (#157).

## 1. ⚠️ Instrucción OBLIGATORIA para Entorno Remoto (Kubernetes UCT)

Para interactuar con el entorno remoto desplegado en el clúster **Kubernetes UCT** (`https://ojociudadano.dev.censei.cl/api/v1`) y evitar fallos de conexión por certificados TLS/SSL autofirmados, es **OBLIGATORIO** desactivar la verificación de certificados SSL en Postman antes de ejecutar cualquier solicitud:

1. Abrir **Postman Settings** (ícono de engranaje en la barra superior).
2. En la pestaña **General**, localizar la opción **SSL certificate verification**.
3. Cambiar el interruptor a **OFF**.

---

## 2. Prerrequisitos y Configuración del Entorno

1. **Importar los archivos en Postman**:
   - `postman/Ojo_Ciudadano_Sprint_2.postman_collection.json`
   - `postman/Ojo_Ciudadano_Sprint_2.postman_environment.json`
2. **Seleccionar el entorno**: Activar `Ojo Ciudadano - Sprint 2 (Template)` en el selector de entornos de Postman.
3. **Configurar variables en `Current Value` (sin modificar ni comitear secretos en el repositorio)**:
   - `baseUrl`:
     - Entorno local: `http://localhost:3000/api/v1`
     - Entorno remoto Kubernetes UCT: `https://ojociudadano.dev.censei.cl/api/v1`
   - `adminPassword`: Contraseña de la cuenta de prueba `admin@ojociudadano.cl` definida en el seed local (por defecto en entorno local: `Admin123!`).
   - `ciudadanoPassword`: Contraseña de la cuenta demo `ciudadano.demo1@ojociudadano.cl` definida en `DEMO_CITIZEN_PASSWORD` o su fallback local (`CiudadanoDemo123!`).
   - `usuarioObjetivoId`: Por defecto apunta al segundo ciudadano demo del seed (`a1b2c3d4-0002-4000-8000-000000000002`).
4. **Archivo de prueba para Evidencias (#86)**:
   - En la solicitud **`3.1 Subir Evidencia Asociada al Incidente (201 Created)`**, abrir la pestaña **Body -> form-data** y seleccionar en el campo `archivo` una imagen local válida (`.jpg`, `.png` o `.webp` de máximo 10 MB).

---

## 3. Orden de Ejecución Recomendado

Las solicitudes están numeradas y encadenadas mediante scripts en la pestaña **Tests** que almacenan automáticamente los tokens (`adminToken`, `ciudadanoToken`) y los UUIDs generados (`categoriaId`, `incidenteId`, `evidenciaId`):

1. **1. Auth & Sesión**
   - `1.1 Login Administrador (200 OK)` — Obtiene y guarda `adminToken` y `adminId`.
   - `1.2 Login Ciudadano (200 OK)` — Obtiene y guarda `ciudadanoToken` y `ciudadanoId`.
   - `1.3 Consultar Perfil Propio Ciudadano (/usuarios/me)` — Verifica identidad y rol vigente.
2. **2. Incidentes & Transiciones de Estado (#116, #117)**
   - `2.1 Obtener Categorías y Guardar categoriaId (200 OK)` — Selecciona automáticamente una categoría válida.
   - `2.2 Crear Incidente como Ciudadano (201 Created)` — Crea un incidente en estado `"Nuevo"` y guarda `incidenteId`.
   - `2.3 Cambiar Estado - Transición Permitida Nuevo -> En revisión (200 OK)` — Ejecutado como `ADMIN`; actualiza el estado, registra historial y dispara notificación.
   - `2.4 Cambiar Estado - Transición Prohibida En revisión -> Resuelto (409 Conflict)` — Comprueba el rechazo `409` ante una transición inválida según la matriz #116.
   - `2.5 Consultar Historial del Incidente (200 OK)` — Verifica la trazabilidad de estados.
3. **3. Evidencias (#86)**
   - `3.1 Subir Evidencia Asociada al Incidente (201 Created)` — Sube fotografía `multipart/form-data` y guarda `evidenciaId`.
   - `3.2 Consultar Evidencias del Incidente (200 OK)` — Lista las evidencias del incidente.
   - `3.3 Consultar Metadata de Evidencia Individual (200 OK)` — Verifica metadata pública sin exponer `claveArchivo`.
   - `3.4 Recuperar Archivo Binario de Evidencia (200 OK)` — Descarga el stream binario verificando su `Content-Type`.
4. **4. Notificaciones (#113, #114)**
   - `4.1 Consultar Notificaciones Propias tras Cambio Relevante (200 OK)` — Comprueba como `CIUDADANO` que el cambio a `"En revisión"` generó la notificación persistida correspondiente a `incidenteId`.
5. **5. Gestión de Roles de Usuario (#122)**
   - `5.1 Cambiar Rol de Usuario a ADMIN como Administrador (200 OK)` — Promueve temporalmente a `usuarioObjetivoId`.
   - `5.2 Restaurar Rol de Usuario a CIUDADANO como Administrador (200 OK)` — Restaura el rol original del usuario demo.
   - `5.3 Rechazo de Auto-degradación de Administrador (403 Forbidden)` — Comprueba que el `ADMIN` autenticado no puede quitarse su propio rol (`adminId`).
6. **6. Casos Negativos de Autorización 401 y 403 (#100)**
   - `6.1 Rechazo 401 Sin Token Bearer (401 Unauthorized)`
   - `6.2 Rechazo 401 con Token Bearer Inválido (401 Unauthorized)`
   - `6.3 Rechazo 403 al Cambiar Estado de Incidente como Ciudadano (403 Forbidden)`
   - `6.4 Rechazo 403 al Cambiar Rol de Usuario como Ciudadano (403 Forbidden)`

---

## 4. Efectos sobre los Datos

- **Creación de registros**: Cada ejecución completa inserta un nuevo registro en `incidente`, `ubicacion`, `historial_estado` (2 entradas: creación y transición a `"En revisión"`), `evidencia` (junto con el objeto en Supabase Storage) y `notificacion`.
- **Modificación de roles**: El paso `5.1` cambia el rol del ciudadano demo 2 (`a1b2c3d4-0002-4000-8000-000000000002`) a `ADMIN` y el paso `5.2` lo devuelve inmediatamente a `CIUDADANO`, manteniendo el estado final consistente.
- **Seguridad**: Los archivos exportados en este directorio no contienen credenciales reales, JWT activos ni datos personales.