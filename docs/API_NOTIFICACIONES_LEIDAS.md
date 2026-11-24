# Marcar notificaciones como leídas

| Campo | Valor |
|---|---|
| Issue | #327 |
| Endpoint individual | `PATCH /api/v1/notificaciones/:id/leida` |
| Endpoint masivo | `PATCH /api/v1/notificaciones/leidas` |
| Autorización | JWT válido con rol `ADMIN` o `CIUDADANO` |
| Validación | UUID de notificación en la ruta; usuario tomado del JWT |
| Dependencias | #113 (persistencia) y #114 (consulta de notificaciones propias) |

## Operaciones

Ambas operaciones actúan **solo** sobre las notificaciones del usuario autenticado. El usuario se obtiene del claim `sub` del JWT; no se envía `usuarioId` en la petición.

### Individual

```http
PATCH /api/v1/notificaciones/11eebc99-9c0b-4ef8-bb6d-6bb9bd380a77/leida
Authorization: Bearer <token>
```

#### Respuesta exitosa

```json
{
  "id": "11eebc99-9c0b-4ef8-bb6d-6bb9bd380a77",
  "usuarioId": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12",
  "incidenteId": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13",
  "mensaje": "El incidente cambió de estado a \"En revisión\".",
  "leido": true,
  "fecha": "2026-10-29T10:05:00.000Z"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador de la notificación. |
| `usuarioId` | UUID | Destinatario de la notificación. |
| `incidenteId` | UUID | Incidente relacionado. |
| `mensaje` | string | Contenido de la notificación. |
| `leido` | boolean | `true` después de la operación. |
| `fecha` | string (ISO 8601) | Fecha de emisión; no cambia. |

### Masiva

```http
PATCH /api/v1/notificaciones/leidas
Authorization: Bearer <token>
```

No requiere body.

#### Respuesta exitosa

```json
{
  "marcadas": 2
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `marcadas` | integer ≥ 0 | Notificaciones pendientes del usuario que pasaron a `leido: true`. Es `0` si ya estaban todas leídas. |

## Comportamiento

- **Propiedad:** una notificación ajena se rechaza con `403` y no se modifica.
- **Idempotencia:** repetir la operación individual devuelve `200` con la notificación ya leída; repetir la masiva devuelve `{"marcadas": 0}`.
- **Aislamiento:** la operación masiva filtra por `usuarioId` y `leido: false`; no afecta notificaciones de otras cuentas ni notificaciones ya leídas.
- **Sin cambios de esquema:** se usa el campo `leido` existente; no hay migración nueva.

## Errores

| Código | Caso |
|---|---|
| `400` | El `id` de la ruta no es un UUID válido. |
| `401` | Falta el token o está inválido/expirado. |
| `403` | La notificación no pertenece al usuario autenticado (solo operación individual). |
| `500` | Error al persistir el cambio. |

## API Gateway

El Gateway expone ambas rutas bajo `/api/v1` con `JwtAuthGuard` y reenvía la petición al microservicio de notificaciones, sin transformar el body. Ambas están documentadas en `api/docs` bajo el tag **Notificaciones**.

## Contrato para el equipo móvil (#281 / #284)

Los mismos contratos aplican para Android:

```bash
curl -X PATCH \
  -H "Authorization: Bearer $TOKEN" \
  "$API_URL/api/v1/notificaciones/11eebc99-9c0b-4ef8-bb6d-6bb9bd380a77/leida"

curl -X PATCH \
  -H "Authorization: Bearer $TOKEN" \
  "$API_URL/api/v1/notificaciones/leidas"
```

La respuesta individual es la notificación con `leido: true`; la masiva es `{ "marcadas": N }`. No se envía `usuarioId`: el servidor lo obtiene del JWT.

## Compatibilidad

- El listado de #114 (`GET /api/v1/notificaciones`) refleja el estado actualizado en la siguiente consulta.
- No se modifica la estructura de `NotificacionResponseDto`.
- Fuera de alcance: contador, polling y avisos (#327 excluye FRN-04/FRN-05/FRN-07/MRA-07).
