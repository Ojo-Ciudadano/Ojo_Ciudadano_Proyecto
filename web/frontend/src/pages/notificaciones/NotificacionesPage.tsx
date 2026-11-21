import { EstadoVistaMensaje } from '../../components/ui/EstadoVistaMensaje'
import { useAuth } from '../../contexts/AuthContext'
import { useNotificaciones } from '../../hooks/useNotificaciones'
import {
  interpretarFecha,
  mostrarFecha,
  texto,
} from '../../utils/notificacionesFormato'
import './NotificacionesPage.css'

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
            Consulta los avisos y las actualizaciones asociados a tu cuenta.
          </p>
        </div>
      </header>

      <div aria-busy={consultando}>
        {consultando ? (
          <EstadoVistaMensaje
            tipo="loading"
            mensaje="Cargando notificaciones…"
          />
        ) : !isAuthenticated ? (
          <EstadoVistaMensaje
            tipo="empty"
            mensaje="Inicia sesión para consultar tus notificaciones."
          />
        ) : error ? (
          <EstadoVistaMensaje
            tipo="error"
            titulo="No se pudieron cargar las notificaciones"
            mensaje={error}
          >
            <button
              type="button"
              className="ui-btn ui-btn--danger"
              onClick={reintentar}
            >
              Reintentar
            </button>
          </EstadoVistaMensaje>
        ) : notificaciones.length === 0 ? (
          <EstadoVistaMensaje
            tipo="empty"
            titulo="No tienes notificaciones"
            mensaje="Cuando haya avisos para tu cuenta, aparecerán aquí."
          />
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