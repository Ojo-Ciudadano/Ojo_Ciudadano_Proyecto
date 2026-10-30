# API de cambio de estado de incidentes

| Campo | Valor |
|---|---|
| Issue | #117 |
| Endpoint | `PATCH /api/v1/incidentes/:id/estado` |
| Autorización | Rol `ADMIN` (JWT) |
| Reglas de transición | [MATRIZ_TRANSICIONES_INCIDENTES.md](MATRIZ_TRANSICIONES_INCIDENTES.md) (#116) |

## Petición

```http
PATCH /api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/estado
Authorization: Bearer <token-admin>
Content-Type: application/json

{
  "estado": "En revisión",
  "observacion": "Se inicia la evaluación del reporte"
}
```

- `estado`: nombre canónico del estado destino (ver catálogo de #116). No se aceptan UUID ni variantes sin tilde.
- `observacion`: texto obligatorio que se registra en el historial.
- El cliente **no** debe enviar el identificador del usuario: la identidad se obtiene del JWT verificado. Campos adicionales son rechazados con `400`.

## Respuesta exitosa

`200 OK` con el incidente actualizado, incluyendo su estado confirmado:

```json
{
  "id": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13",
  "titulo": "Fuga de agua",
  "estado": {
    "id": "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15",
    "nombre": "En revisión",
    "orden": 2
  }
}
```

La respuesta conserva la estructura del resto de endpoints de incidentes.

## Errores

| Código | Caso |
|---|---|
| `400` | `id` no es UUID, estado desconocido/no canónico, observación ausente o vacía, o campos no permitidos. |
| `401` | Token ausente, inválido o expirado. |
| `403` | Usuario autenticado sin rol `ADMIN`. |
| `404` | Incidente o estado destino inexistente. |
| `409` | Transición prohibida según #116, incluida la solicitud al mismo estado. |

Ante `409` no se modifica el incidente ni se registra historial.

## Persistencia

El estado y el registro en `historial_estado` se escriben en la misma transacción. El historial guarda el estado resultante, el usuario autenticado, la observación y la fecha. Si falla la escritura del historial, se revierte el cambio de estado.
