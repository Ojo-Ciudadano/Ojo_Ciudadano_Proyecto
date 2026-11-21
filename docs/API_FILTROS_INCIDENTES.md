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
| `ciudad` | Texto no vacío | Filtra por la ciudad elegida en la barra de filtros. Se resuelve contra el catálogo `CIUDADES_ARAUCANIA` de `domain/` y se traduce a una o más `ubicacion.comuna`. Nombre acordado con MTN-01 (#315) para el Sprint 3. |
| `comuna` | Texto no vacío | Filtra por `ubicacion.comuna`. |
| `usuarioReportaId` | UUID | Conserva el filtro existente por usuario que reporta. |

### Criterio de ciudad

El parámetro se llama `ciudad` y es el que la barra de filtros envía y el Gateway reenvía (`GET /api/v1/incidentes`) según lo acordado con MTN-01 (#315). No se crea un campo nuevo ni se cambia el esquema de Prisma: `ciudad` es una regla de dominio que se resuelve en `domain/ubicacion/ciudades-araucania.constants.ts`.

Cada comuna de La Araucanía es su propia ciudad, salvo dos agrupaciones acordadas:

| Ciudad | Comunas que incluye |
|---|---|
| `Temuco` | `Temuco`, `Padre Las Casas` |
| `Villarrica` | `Villarrica`, `Pucón` |

Así, `ciudad=Temuco` devuelve incidentes de `Temuco` **o** `Padre Las Casas`, y `ciudad=Villarrica` devuelve los de `Villarrica` **o** `Pucón`. `ciudad=Pucón` sigue devolviendo solo `Pucón`.

La resolución de la ciudad es:

- Exacta contra el nombre del catálogo, ignorando mayúsculas, tildes y espacios al inicio/final. Ejemplo: `ciudad=villarrica` y `ciudad=  Villarrica  ` son equivalentes.
- No es una búsqueda parcial: `ciudad=Temu` no coincide con `Temuco`.
- Si la ciudad no existe en el catálogo, se devuelve `[]` sin consultar la base de datos.

Cuando se envían `ciudad` y `comuna` juntos, se intersectan sobre la misma ubicación: el incidente debe estar en una comuna de esa ciudad. Por ejemplo, `ciudad=Villarrica&comuna=Pucón` es válido, mientras que `ciudad=Villarrica&comuna=Temuco` devuelve `[]`.

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
GET /api/v1/incidentes?usuarioReportaId=UUID&estadoId=UUID&categoriaId=UUID&ciudad=Temuco&comuna=Padre%20Las%20Casas
```

equivale a exigir que el incidente cumpla todos estos criterios:

- `usuarioReportaId` sea el UUID indicado.
- `estadoId` corresponda al estado actual.
- `categoriaId` corresponda a la categoría asociada.
- `ubicacion.comuna` esté dentro de las comunas de `Temuco` (`Temuco` o `Padre Las Casas`).
- `ubicacion.comuna` sea además `Padre Las Casas`, por el filtro de comuna.

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
| `400` | `ciudad` o `comuna` están vacías o contienen solo espacios. |
| `400` | Se envía un parámetro de consulta no permitido. |
| `401` | Falta el token o es inválido/expirado. |
| `403` | El token es válido pero no tiene un rol permitido. |

## API Gateway

El API Gateway reenvía los parámetros de consulta al microservicio de incidentes sin transformarlos. No se requiere una ruta nueva: el catch-all de `/incidentes` ya propaga el query string.

## Compatibilidad

- `GET /api/v1/incidentes` sin parámetros mantiene su respuesta anterior.
- `usuarioReportaId` sigue funcionando y puede combinarse con los nuevos filtros.
- `ciudad` es opcional: al omitirla no se agrega ninguna condición sobre la ubicación.
- El nombre `region` ya no se usa en el contrato de filtros; `Ubicacion.region` sigue existiendo en el modelo pero no es un parámetro de consulta.
- Los filtros solo restringen resultados; no amplían los permisos del solicitante.
- La autenticación existente se conserva.
- La estructura de respuesta y el ordenamiento no cambian.

