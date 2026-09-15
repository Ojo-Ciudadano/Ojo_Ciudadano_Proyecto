# Decisión Técnica: ORM Prisma — Ojo Ciudadano

## 1. Contexto

El backend de Ojo Ciudadano se construye con NestJS + TypeScript, sobre una base de datos PostgreSQL alojada en Supabase, con una arquitectura dividida en microservicios (Auth/Usuarios, Notificaciones, Incidentes), donde la base de datos se segmenta lógicamente por microservicio. Se necesitaba definir qué ORM usar para el acceso a datos, evaluando su compatibilidad con este escenario: TypeScript fuerte, PostgreSQL, Supabase y múltiples servicios con datos segmentados.

## 2. Criterios de evaluación

1. Compatibilidad con TypeScript (tipado fuerte, autocompletado, contratos claros con la API).
2. Compatibilidad con NestJS (facilidad de integración, comunidad, documentación).
3. Compatibilidad con PostgreSQL / Supabase (soporte de conexión, pooling, extensiones).
4. Soporte para segmentación por microservicio (poder aislar tablas/entidades por servicio).
5. Manejo de migraciones (control de versiones del esquema, trazabilidad — relevante porque el proyecto exige historial auditable).
6. Madurez y comunidad (soporte a largo plazo, documentación, adopción).

## 3. Alternativas evaluadas

| ORM | TypeScript | Integración NestJS | Migraciones |
|---|---|---|---|
| **Prisma** | Nativo, tipos autogenerados | Buena, muy usada en proyectos NestJS | Prisma Migrate, versionadas y declarativas |
| TypeORM | Soporta TS, tipado más débil en relaciones complejas | ORM "oficial" sugerido por NestJS | Menos robustas, manuales o autogeneradas |
| Sequelize | Pensado para JS, tipado forzado | Requiere adaptadores adicionales | Vía CLI, poco integradas |
| Drizzle ORM | Tipado fuerte, muy ligero | Integración menos estandarizada | Más manuales, herramienta más nueva |

## 4. Prisma y la segmentación por microservicio

Cada microservicio (Auth/Usuarios, Notificaciones, Incidentes) tiene su propio archivo `schema.prisma`, conteniendo únicamente los modelos que le corresponden según la segmentación ya definida por el equipo. A diferencia de un esquema compartido, cada microservicio utiliza su **propia base de datos PostgreSQL**, físicamente separada: durante el Sprint 1 esto se implementa de forma local con un contenedor Postgres independiente por microservicio (validado en la prueba técnica reproducible con Docker realizada por el equipo), y en producción cada microservicio apuntará a su propia base de datos en Supabase. El cambio de entorno local a remoto no altera la lógica de la aplicación: solo requiere modificar la variable `DATABASE_URL` de cada servicio para apuntar a la instancia remota correspondiente. Esto logra un aislamiento **físico** real entre microservicios, más estricto que una simple separación lógica por schema dentro de una misma base de datos.

## 5. Decisión final

**Se adopta Prisma como ORM del proyecto**, por las siguientes razones:

1. **Tipado fuerte end-to-end**: genera un cliente TypeScript totalmente tipado, reduciendo errores en los contratos entre backend, app móvil y web.
2. **Soporte de múltiples schemas de Prisma**: cada microservicio mantiene su propio archivo `schema.prisma`, apuntando a su propia base de datos, lo que refleja fielmente la segmentación física del proyecto.
3. **Migraciones versionadas** (`prisma migrate`): entrega trazabilidad y control de versiones del esquema, alineado con el requisito de historial auditable del proyecto.
4. **Compatibilidad directa con PostgreSQL/Supabase**, usando el connection pooler (PgBouncer) para runtime y la conexión directa solo para migraciones.
5. **Prisma Studio** como herramienta visual de apoyo durante el desarrollo, útil para un equipo de 6 personas trabajando en paralelo.

**Alternativa descartada — TypeORM**: aunque es la opción "oficial" sugerida por NestJS, se descarta por tener un tipado más débil en relaciones complejas y migraciones menos robustas, factor relevante dado el número de entidades relacionadas del proyecto (usuario, rol, incidente, categoría, estado, ubicación, evidencia, comentario, asignación, historial).