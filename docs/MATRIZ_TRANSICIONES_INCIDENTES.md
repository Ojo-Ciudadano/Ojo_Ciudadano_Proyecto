# Matriz de transiciones de estados de incidentes

| Campo | Valor |
|---|---|
| Versión | 1.0 |
| Fecha | 2026-10-26 |
| Issue | #116 |
| Consumidores | MRA-02 (backend), BAR-05 (Web), #105 (Android) |

Este documento es la **única referencia** de las reglas de cambio de estado de un incidente. Backend, Web y Android deben ajustarse a él. Cualquier cambio de reglas se modifica primero aquí, con nueva versión, y después en cada plataforma.

## 1. Catálogo de estados

Corresponde al catálogo sembrado en `web/backend/apps/incidentes/prisma/seed.ts` (modelo `Estado`). No se agregan, eliminan ni renombran estados.

| Orden | Nombre canónico |
|---|---|
| 1 | Nuevo |
| 2 | En revisión |
| 3 | En atención |
| 4 | Resuelto |
| 5 | Cerrado |

- Los identificadores estables son `nombre` (único en la tabla `estado`) y `orden`.
- `estado.id` es un UUID generado al sembrar cada base de datos y **puede diferir entre ambientes**. Ningún consumidor debe fijar UUID en el código; debe resolverlos por `nombre`.
- Los nombres se escriben exactamente como en la tabla, con tildes y mayúscula inicial (por ejemplo, `En revisión`, no `En revision`).

## 2. Matriz

Filas: estado actual (origen). Columnas: estado solicitado (destino).

| Origen \ Destino | Nuevo | En revisión | En atención | Resuelto | Cerrado |
|---|---|---|---|---|---|
| **Nuevo** | Prohibida | **Permitida** | Prohibida | Prohibida | Prohibida |
| **En revisión** | Prohibida | Prohibida | **Permitida** | Prohibida | Prohibida |
| **En atención** | Prohibida | Prohibida | Prohibida | **Permitida** | Prohibida |
| **Resuelto** | Prohibida | Prohibida | **Permitida** | Prohibida | **Permitida** |
| **Cerrado** | Prohibida | Prohibida | Prohibida | Prohibida | Prohibida |

La diagonal (mismo estado) es siempre **Prohibida**; ver sección 4.

### Transiciones permitidas (lista completa)

| # | Origen | Destino | Motivo |
|---|---|---|---|
| 1 | Nuevo | En revisión | Un administrador comienza a evaluar el reporte |
| 2 | En revisión | En atención | El reporte es válido y se inicia su atención |
| 3 | En atención | Resuelto | Se completó la atención |
| 4 | Resuelto | Cerrado | Se confirma la resolución y finaliza el ciclo |
| 5 | Resuelto | En atención | Reapertura: la solución no fue satisfactoria |

Cualquier combinación que no esté en esta lista está prohibida.

## 3. Reglas

1. **Flujo lineal:** el avance sigue el orden Nuevo → En revisión → En atención → Resuelto → Cerrado, sin saltar estados.
2. **Sin retrocesos**, salvo la reapertura de la transición 5 (Resuelto → En atención).
3. **Resuelto** admite dos salidas: Cerrado o En atención (reapertura).
4. **Cerrado es estado terminal:** no admite ninguna transición posterior. Si el problema persiste, se registra un incidente nuevo.
5. No se puede pasar de En atención a Cerrado directamente; debe pasar por Resuelto.

## 4. Solicitud al mismo estado

Si el estado solicitado es igual al actual, la solicitud se trata como **transición prohibida**: responde `409 Conflict`, no modifica el incidente y no registra un cambio en el historial.

## 5. Autorización

Solo el rol **Administrador** puede solicitar cambios de estado. La autorización efectiva se implementa en #100 y no se duplica aquí.

Orden de validación recomendado para MRA-02:

1. Autenticación y rol (401 / 403, según #100).
2. Existencia del incidente y del estado destino (404).
3. Validez de la transición según este documento (409).

## 6. Comportamiento requerido de `PATCH /incidentes/:id/estado` (MRA-02)

Ante una transición prohibida (incluido el mismo estado), el backend debe:

- Responder `409 Conflict`.
- **No** modificar el estado del incidente.
- **No** registrar un cambio exitoso en `historial_estado`.

La verificación del estado actual y la escritura deben ocurrir dentro de la misma transacción, para evitar condiciones de carrera entre dos solicitudes simultáneas.

## 7. Reglas para Web (BAR-05) y Android (#105)

- Mostrar como opciones solo los destinos **permitidos** desde el estado actual (sección 2). El backend sigue siendo la autoridad: si responde 409, la interfaz muestra el error y no actualiza el estado en pantalla.
- Desde **Cerrado** no se ofrece ninguna acción de cambio de estado.
- Usar los nombres canónicos de la sección 1 y no fijar UUID.
- No definir reglas propias ni distintas de este documento.

## 8. Representación para implementación

Referencia en formato de datos (clave: estado origen, valor: destinos permitidos):

```json
{
  "Nuevo": ["En revisión"],
  "En revisión": ["En atención"],
  "En atención": ["Resuelto"],
  "Resuelto": ["En atención", "Cerrado"],
  "Cerrado": []
}
```

## 9. Historial de versiones

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | 2026-10-26 | Versión inicial (#116) |