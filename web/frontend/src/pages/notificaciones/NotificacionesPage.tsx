import { useAuth } from '../../contexts/AuthContext'
import { useNotificaciones } from '../../hooks/useNotificaciones'
import './NotificacionesPage.css'

function texto(valor: unknown): string {
  return typeof valor === 'string' ? valor.trim() : ''
}

function interpretarFecha(valor: unknown): Date | null {
  const contenido = texto(valor)

  if (!contenido) return null

  const fecha = new Date(contenido)

  return Number.isNaN(fecha.getTime()) ? null : fecha
}

function mostrarFecha(fecha: Date): string {
  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha)
}

export function NotificacionesPage() {
  const { isAuthenticated, isLoading } = useAuth()

  const {
    notificaciones,
    cargando,
    error,
    reintentar,
  } = useNotificaciones()

  const consultando = isLoading || cargando

  return (
    <section
      className="notificaciones-pagina"
      aria-labelledby="notificaciones-titulo"
    >
      <header className="notificaciones-encabezado">
        <div
          className="notificaciones-icono"
          aria-hidden="true"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
            <path d="M10 21h4" />
          </svg>
        </div>

        <div>
          <h1 id="notificaciones-titulo">Notificaciones</h1>
          <p>
            Consulta los avisos y las actualizaciones
            asociados a tu cuenta.
          </p>
        </div>
      </header>

      <div aria-busy={consultando}>
        {consultando ? (
          <div
            className="notificaciones-mensaje"
            role="status"
          >
            Cargando notificaciones…
          </div>
        ) : !isAuthenticated ? (
          <div
            className="notificaciones-mensaje"
            role="status"
          >
            Inicia sesión para consultar tus notificaciones.
          </div>
        ) : error ? (
          <div
            className="notificaciones-mensaje notificaciones-error"
            role="alert"
          >
            <h2>No se pudieron cargar las notificaciones</h2>
            <p>{error}</p>

            <button type="button" onClick={reintentar}>
              Reintentar
            </button>
          </div>
        ) : notificaciones.length === 0 ? (
          <div
            className="notificaciones-mensaje notificaciones-vacio"
            role="status"
          >
            <h2>No tienes notificaciones</h2>
            <p>
              Cuando haya avisos para tu cuenta, aparecerán aquí.
            </p>
          </div>
        ) : (
          <ol
            className="notificaciones-lista"
            aria-label="Notificaciones de tu cuenta"
          >
            {notificaciones.map((notificacion) => {
              const fecha = interpretarFecha(notificacion.fecha)
              const referencia = texto(notificacion.incidenteId)

              return (
                <li
                  key={notificacion.id}
                  className="notificaciones-tarjeta"
                  data-notificacion-id={notificacion.id}
                >
                  <article>
                    <h2>
                      {texto(notificacion.mensaje) ||
                        'Mensaje no disponible.'}
                    </h2>

                    <dl className="notificaciones-datos">
                      <div>
                        <dt>Fecha</dt>
                        <dd>
                          {fecha ? (
                            <time dateTime={fecha.toISOString()}>
                              {mostrarFecha(fecha)}
                            </time>
                          ) : (
                            'Fecha no disponible'
                          )}
                        </dd>
                      </div>

                      {referencia && (
                        <div>
                          <dt>Incidente de referencia</dt>
                          <dd className="notificaciones-referencia">
                            {referencia}
                          </dd>
                        </div>
                      )}
                    </dl>
                  </article>
                </li>
              )
            })}
          </ol>
        )}
      </div>
    </section>
  )
}