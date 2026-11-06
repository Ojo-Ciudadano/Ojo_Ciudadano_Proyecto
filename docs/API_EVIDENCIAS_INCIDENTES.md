# Contrato de la API de Evidencias Fotográficas de Incidentes (#86)

Este documento especifica los endpoints expuestos por el microservicio de **Incidentes** y el **API Gateway** (`/api/v1`) para subir, consultar y descargar evidencias fotográficas asociadas a un incidente, manteniendo el acceso a **Supabase Storage** completamente encapsulado en el backend.

---

## 1. Principios de Diseño y Límites

* **Encapsulación de Supabase Storage**: Los clientes (**Android** y **Web**) nunca interactúan directamente con Supabase Storage ni reciben credenciales administrativas (`SUPABASE_SERVICE_ROLE_KEY`) ni rutas internas del bucket (`claveArchivo`).
* **Formatos permitidos**: `image/jpeg` (`.jpg`), `image/png` (`.png`) e `image/webp` (`.webp`).
* **Límite funcional por fotografía**: **10 MB** (`10,485,760` bytes). Si el archivo supera 10 MB se rechaza con `413 Payload Too Large`.
* **Límite técnico de payload (BodyParser / Multer / Ingress / Proxy)**: **12 MB** (`12,582,912` bytes), permitiendo absorber la sobrecarga de cabeceras `multipart/form-data` y entregar errores de negocio controlados.
* **Autenticación**: Todas las rutas públicas en el API Gateway (`/api/v1/incidentes/*` y `/api/v1/evidencias/*`) requieren cabecera `Authorization: Bearer <JWT>`.

---

## 2. Endpoints Expuestos (vía API Gateway `/api/v1`)

### 2.1. Subir una evidencia fotográfica asociada a un incidente

* **Ruta principal**: `POST /api/v1/incidentes/:incidenteId/evidencias`
* **Ruta alternativa**: `POST /api/v1/evidencias` (enviando `incidenteId` como campo del `multipart/form-data` o query `?incidenteId=...`)
* **Content-Type**: `multipart/form-data`
* **Campo de archivo**: Se acepta el archivo binario adjunto en el formulario multipart (ej. campo `archivo` o `file`, máximo 1 archivo por petición).

#### Respuesta exitosa (`201 Created`)

```json
{
  "id": "8f14e45f-ceea-467a-9a38-7b23d28f9102",
  "incidenteId": "550e8400-e29b-41d4-a716-446655440000",
  "tipoMime": "image/jpeg",
  "tamanoArchivo": 245812,
  "fechaCreacion": "2026-10-23T18:30:00.000Z",
  "url": "/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo"
}
```

#### Errores controlados

| Código HTTP | Escenario |
| :--- | :--- |
| `400 Bad Request` | No se adjuntó ningún archivo, se adjuntó más de un archivo, el archivo está vacío o el `tipoMime` no es `image/jpeg`, `image/png` ni `image/webp`. |
| `401 Unauthorized` | Token JWT ausente, inválido o expirado. |
| `404 Not Found` | El incidente `:incidenteId` no existe en PostgreSQL. |
| `413 Payload Too Large` | El archivo supera el límite funcional de **10 MB** (o la carga útil supera el límite técnico de **12 MB**). |
| `502 Bad Gateway` | Error de comunicación con el almacenamiento de objetos (Supabase Storage). |

---

### 2.2. Consultar las evidencias asociadas a un incidente

* **Ruta principal**: `GET /api/v1/incidentes/:incidenteId/evidencias`
* **Ruta alternativa**: `GET /api/v1/evidencias?incidenteId=:incidenteId`

#### Respuesta exitosa (`200 OK`)

Retorna un arreglo ordenado cronológicamente (`fechaCreacion` ascendente). Si el incidente existe pero no tiene evidencias, retorna `[]`.

```json
[
  {
    "id": "8f14e45f-ceea-467a-9a38-7b23d28f9102",
    "incidenteId": "550e8400-e29b-41d4-a716-446655440000",
    "tipoMime": "image/jpeg",
    "tamanoArchivo": 245812,
    "fechaCreacion": "2026-10-23T18:30:00.000Z",
    "url": "/evidencias/8f14e45f-ceea-467a-9a38-7b23d28f9102/archivo"
  }
]
```

#### Errores controlados

* `404 Not Found`: Si el incidente `:incidenteId` no existe.

---

### 2.3. Consultar metadata de una evidencia individual

* **Ruta**: `GET /api/v1/evidencias/:id`
* **Respuesta (`200 OK`)**: Objeto JSON con la metadata pública de la evidencia (`id`, `incidenteId`, `tipoMime`, `tamanoArchivo`, `fechaCreacion`, `url`).
* **Errores**: `404 Not Found` si la evidencia no existe.

---

### 2.4. Descargar / Obtener el archivo binario de una evidencia

* **Ruta principal**: `GET /api/v1/evidencias/:id/archivo`
* **Ruta anidada compatible**: `GET /api/v1/incidentes/:incidenteId/evidencias/:id/archivo`
* **Respuesta (`200 OK`)**:
  * Cuerpo binario (`Buffer` / stream de la imagen).
  * `Content-Type`: Tipo MIME original registrado (`image/jpeg`, `image/png` o `image/webp`).
  * `Content-Length`: Tamaño exacto en bytes.
  * `Cache-Control: private, max-age=3600`.
* **Errores controlados**:
  * `404 Not Found`: Si el registro de la evidencia no existe, no corresponde al `:incidenteId` indicado o el objeto no se encuentra disponible en el bucket.
  * `502 Bad Gateway`: Si ocurre un fallo interno al descargar desde Supabase Storage.
