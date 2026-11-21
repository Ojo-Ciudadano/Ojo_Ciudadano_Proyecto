# Filtros de consulta de incidentes

| Campo | Valor |
|---|---|
| Issue | #119, #314 |
| Endpoint | `GET /api/v1/incidentes` |
| Autorización | JWT válido con rol `ADMIN` o `CIUDADANO` |
| Validación | `FindIncidentesQueryDto` |

## Parámetros de consulta

Todos los parámetros son opcionales y se combinan mediante `AND`.

| Parámetro | Formato | Regla |
|---|---|---|
| `estadoId` | UUID | Filtra por el estado actual del incidente (`Incidente.estadoId`). |
| `categoriaId` | UUID | Filtra por la categoría asociada (`Incidente.categoriaId`). |
| `region` | Texto no vacío | Filtra por la región/ciudad de la ubicación (`Ubicacion.region`). Nombre acordado con MTN-01 para el filtro de ciudad del Sprint 3. |
| `comuna` | Texto no vacío | Filtra por `ubicacion.comuna`. |
| `usuarioReportaId` | UUID | Conserva el filtro existente por usuario que reporta. |

### Criterio de región (ciudad)

El parámetro se llama `region` porque así se representa la ciudad en el modelo de ubicación (`Ubicacion.region`, tipo `String`). MTN-01 reenvía este mismo nombre en el Gateway (`GET /api/v1/incidentes`), según la planificación del Sprint 3 (MRA-02 / MTN-01). No se crea un campo nuevo ni se cambia el esquema de Prisma.

La comparación de `region` es:

- Exacta sobre el valor completo de `ubicacion.region`.
- Sin distinguir mayúsculas/minúsculas.
- Ignorando espacios al inicio y al final del valor recibido.

No es una búsqueda parcial ni normaliza tildes. Ejemplo: `region=La Araucanía` coincide con `la araucanía` o `LA ARAUCANÍA`, pero no con `Araucanía`.

`region` y `comuna` se aplican juntos sobre la misma ubicación: un incidente debe cumplir ambos criterios cuando se envían los dos.

### Criterio de comuna

La comparación de `comuna` es:

- Exacta sobre el valor completo de `ubicacion.comuna`.
- Sin distinguir mayúsculas/minúsculas.
- Ignorando espacios al inicio y al final del valor recibido.

No es una búsqueda parcial (`contains`), no ignora tildes y no aplica coincidencia por prefijo.

Ejemplo: `comuna=Temuco` coincide con `Temuco`, `temuco` o `TEMUCO`, pero no con `Temuco Centro`.

## Combinación de filtros

Los filtros se aplican simultáneamente. Por ejemplo:

```http
GET /api/v1/incidentes?usuarioReportaId=UUID&estadoId=UUID&categoriaId=UUID&region=La%20Araucan%C3%ADa&comuna=Temuco
```

equivale a exigir que el incidente cumpla todos estos criterios:

- `usuarioReportaId` sea el UUID indicado.
- `estadoId` corresponda al estado actual.
- `categoriaId` corresponda a la categoría asociada.
- `ubicacion.region` coincida con `La Araucanía` según el criterio definido.
- `ubicacion.comuna` coincida con `Temuco` según el criterio definido.

## Comportamiento sin filtros

Si no se envía ningún parámetro:

```http
GET /api/v1/incidentes
```

la consulta conserva el comportamiento anterior:

- No se aplica filtro en `where`.
- Se mantiene el orden por `fechaCreacion` descendente.
- La respuesta sigue siendo un arreglo JSON con la misma estructura anterior.

Si no existen coincidencias, se devuelve un arreglo vacío:

```json
[]
```

No se agrega paginación nueva ni se modifica el contrato de respuesta.

## Validación y errores

| Código | Caso |
|---|---|
| `400` | `estadoId`, `categoriaId` o `usuarioReportaId` no son UUID válidos. |
| `400` | `region` o `comuna` están vacías o contienen solo espacios. |
| `400` | Se envía un parámetro de consulta no permitido. |
| `401` | Falta el token o es inválido/expirado. |
| `403` | El token es válido pero no tiene un rol permitido. |

## API Gateway

El API Gateway reenvía los parámetros de consulta al microservicio de incidentes sin transformarlos. No se requiere una ruta nueva: el catch-all de `/incidentes` ya propaga el query string.

## Compatibilidad

- `GET /api/v1/incidentes` sin parámetros mantiene su respuesta anterior.
- `usuarioReportaId` sigue funcionando y puede combinarse con los nuevos filtros.
- `region` es opcional: al omitirlo no se agrega ninguna condición sobre la ubicación.
- Los filtros solo restringen resultados; no amplían los permisos del solicitante.
- La autenticación existente se conserva.
- La estructura de respuesta y el ordenamiento no cambian.

