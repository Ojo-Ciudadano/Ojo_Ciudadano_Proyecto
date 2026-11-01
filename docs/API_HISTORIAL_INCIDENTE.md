# API de historial de estados de un incidente

| Campo | Valor |
|---|---|
| Issue | #120 |
| Endpoint | `GET /api/v1/incidentes/:id/historial` |
| Autorización | JWT válido con rol `ADMIN` o `CIUDADANO` |
| Depende de | #117 (persistencia del historial) |
| Consumidores previstos | MRA-08, BER-09, BER-04 |

## Petición

```http
GET /api/v1/incidentes/c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13/historial
Authorization: Bearer <token>
```

- `id`: UUID del incidente. Si el formato no es válido, responde `400`.
- El endpoint no recibe body ni parámetros de consulta.
- Requiere el mismo acceso que `GET /api/v1/incidentes/:id`: token JWT válido con rol `ADMIN` o `CIUDADANO`.

## Respuesta exitosa

`200 OK` con un arreglo de registros:

```json
[
  {
    "id": "e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02",
    "incidenteId": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13",
    "estado": {
      "id": "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14",
      "nombre": "Nuevo",
      "orden": 1
    },
    "usuarioId": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12",
    "observacion": "Reporte creado",
    "fecha": "2026-10-29T10:00:00.000Z"
  }
]
```

### Campos

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador del registro de historial. |
| `incidenteId` | UUID | Incidente al que pertenece el registro. |
| `estado` | Objeto | Estado resultante del cambio, con `id`, `nombre` y `orden`. |
| `usuarioId` | UUID | Identidad del actor que realizó el cambio, tomada de `historial_estado.usuario_id`. No corresponde necesariamente al usuario que consulta. |
| `observacion` | `string` o `null` | Observación persistida del cambio. |
| `fecha` | ISO 8601 | Fecha y hora del registro. |

## Orden de los registros

Los registros se devuelven en **orden cronológico ascendente**:

1. `fecha` ascendente: el registro más antiguo aparece primero.
2. Si dos registros tienen exactamente la misma `fecha`, se ordenan por `id` ascendente como desempate determinista.

La consulta aplica:

```ts
orderBy: [{ fecha: 'asc' }, { id: 'asc' }]
```

Este orden permite construir un timeline estable de la evolución del incidente.

## Observaciones ausentes

El campo `observacion` siempre está presente en la respuesta.

- Si el registro persistido tiene una observación, se devuelve como texto.
- Si el registro persistido no tiene observación (`null`), se devuelve `null`.
- No se omite el campo ni se inventa un texto sustituto.

## Incidente sin historial

Si el incidente existe pero no tiene registros asociados, responde `200 OK` con:

```json
[]
```

## Incidente inexistente

Si el incidente no existe, responde `404 Not Found` antes de consultar el historial. No se devuelve una colección vacía en este caso, porque el recurso solicitado no existe.

## Alcance de la consulta

- El servicio valida primero que el incidente exista.
- La consulta filtra explícitamente por `incidenteId`.
- No se incluyen registros de otros incidentes.
- La operación es de solo lectura: no modifica el incidente ni sus registros de historial.

## Errores

| Código | Caso |
|---|---|
| `400` | El `id` del incidente no es un UUID válido. |
| `401` | Falta el token, es inválido o está expirado. |
| `403` | El token es válido pero no tiene un rol permitido. |
| `404` | El incidente no existe. |
| `500` | Error interno al consultar la base de datos. |

## API Gateway

No se requiere una ruta nueva en el API Gateway. El catch-all de `incidentes/*` ya reenvía `GET /api/v1/incidentes/:id/historial` al microservicio de incidentes, incluyendo la validación JWT existente.

## Reutilización interna para MRA-08

La lógica está encapsulada en el método público:

```ts
IncidentesService.findHistorial(id: string)
```

MRA-08 puede reutilizar este método directamente desde el servicio para incorporar el historial al detalle del incidente, sin realizar llamadas HTTP internas. El mapper `toHistorialEstadoResponse` también puede reutilizarse si el detalle necesita el mismo contrato de historial.

## Relación con #117

#117 es responsable de crear los registros en `historial_estado` al crear un incidente y al cambiar su estado. #120 solo consulta esos registros persistidos. Esta Issue no crea, modifica ni elimina historial.
