import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { IncidentMap } from '../../components/map/IncidentMap'
import { mapearIncidentesAMarcadores } from '../../components/map/incidentMarkers'
import { useInicioCiudadano } from '../../hooks/useInicioCiudadano'
import './HomePage.css'

function texto(valor?: string | null): string {
  return valor?.trim() || 'Sin información'
}

function fecha(valor?: string | null): string {
  if (!valor) return 'Sin información'

  const interpretada = new Date(valor)

  if (Number.isNaN(interpretada.getTime())) {
    return 'Sin información'
  }

  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(interpretada)
}

export function HomePage() {
  const {
    sesionDisponible,
    vista,
    estadoId,
    busqueda,
    estados,
    incidentesFiltrados,
    incidentesPagina,
    cargando,
    error,
    cambiarVista,
    cambiarEstado,
    cambiarBusqueda,
    restablecerEstado,
    reintentar,
    paginaActual,
    totalPaginas,
    totalResultados,
    desde,
    hasta,
    puedeRetroceder,
    puedeAvanzar,
    paginaAnterior,
    paginaSiguiente,
  } = useInicioCiudadano()

  const marcadores = useMemo(
    () => mapearIncidentesAMarcadores(incidentesFiltrados),
    [incidentesFiltrados],
  )

  const totalSinCoordenadas = Math.max(
    0,
    totalResultados - marcadores.length,
  )

  return (
    <section
      className="inicio-ciudadano"
      aria-labelledby="inicio-ciudadano-titulo"
    >
      <header className="inicio-ciudadano-encabezado">
        <div>
          <h1 id="inicio-ciudadano-titulo">Inicio</h1>
          <p>
            Consulta los incidentes disponibles y tus propios reportes.
          </p>
        </div>

        {sesionDisponible && (
          <Link
            className="inicio-ciudadano-nuevo"
            to="/reportes/nuevo"
          >
            + Nuevo reporte
          </Link>
        )}
      </header>

      {sesionDisponible && (
        <div className="inicio-ciudadano-filtros">
          <div
            className="inicio-ciudadano-vistas"
            role="group"
            aria-label="Reportes que deseas consultar"
          >
            <button
              type="button"
              aria-pressed={vista === 'todos'}
              onClick={() => cambiarVista('todos')}
            >
              Todos
            </button>

            <button
              type="button"
              aria-pressed={vista === 'mis-reportes'}
              onClick={() => cambiarVista('mis-reportes')}
            >
              Mis reportes
            </button>
          </div>

          <div className="inicio-ciudadano-buscador">
            <label htmlFor="inicio-ciudadano-busqueda">
              Categoría o comuna
            </label>

            <input
              id="inicio-ciudadano-busqueda"
              type="search"
              placeholder="Ej.: incendio, Lautaro…"
              value={busqueda}
              onChange={(event) =>
                cambiarBusqueda(event.target.value)
              }
            />
          </div>

          <div className="inicio-ciudadano-filtro-estado">
            <label htmlFor="inicio-ciudadano-estado">
              Estado
            </label>

            <select
              id="inicio-ciudadano-estado"
              value={estadoId}
              onChange={(event) => cambiarEstado(event.target.value)}
            >
              <option value="">Todos los estados</option>

              {estadoId &&
                !estados.some((estado) => estado.id === estadoId) && (
                  <option value={estadoId}>
                    Estado seleccionado
                  </option>
                )}

              {estados.map((estado) => (
                <option key={estado.id} value={estado.id}>
                  {estado.nombre}
                </option>
              ))}
            </select>
          </div>

          <button
            type="button"
            className="inicio-ciudadano-restablecer"
            onClick={restablecerEstado}
            disabled={!estadoId}
          >
            Restablecer estado
          </button>
        </div>
      )}

      <div className="inicio-ciudadano-distribucion">
        <div className="inicio-ciudadano-resultados">
          <div aria-busy={cargando}>
            {cargando ? (
              <div className="inicio-ciudadano-mensaje" role="status">
                Cargando incidentes…
              </div>
            ) : !sesionDisponible ? (
              <div className="inicio-ciudadano-mensaje" role="status">
                Inicia sesión como Ciudadano para consultar los reportes.
              </div>
            ) : error ? (
              <div
                className="inicio-ciudadano-mensaje inicio-ciudadano-error"
                role="alert"
              >
                <p>{error.mensaje}</p>

                {error.permiteReintentar && (
                  <button type="button" onClick={reintentar}>
                    Reintentar
                  </button>
                )}
              </div>
            ) : totalResultados === 0 ? (
              <div className="inicio-ciudadano-mensaje" role="status">
                {estadoId || busqueda.trim()
                  ? 'No se encontraron reportes con los filtros seleccionados.'
                  : vista === 'mis-reportes'
                    ? 'Todavía no tienes reportes registrados.'
                    : 'No hay incidentes disponibles.'}
              </div>
            ) : (
              <ul className="inicio-ciudadano-lista">
                {incidentesPagina.map((incidente) => (
                  <li
                    key={incidente.id}
                    className="inicio-ciudadano-tarjeta"
                    data-incidente-id={incidente.id}
                    data-estado={incidente.estado?.nombre ?? ''}
                  >
                    <article
                      aria-labelledby={`incidente-titulo-${incidente.id}`}
                    >
                      <div className="inicio-ciudadano-tarjeta-encabezado">
                        <h2 id={`incidente-titulo-${incidente.id}`}>
                          {texto(incidente.titulo)}
                        </h2>

                        <span className="inicio-ciudadano-estado">
                          {texto(incidente.estado?.nombre)}
                        </span>
                      </div>

                      <p className="inicio-ciudadano-folio">
                        Folio: {incidente.id}
                      </p>

                      <dl className="inicio-ciudadano-datos">
                        <div>
                          <dt>Categoría</dt>
                          <dd>{texto(incidente.categoria?.nombre)}</dd>
                        </div>

                        <div>
                          <dt>Comuna</dt>
                          <dd>{texto(incidente.ubicacion?.comuna)}</dd>
                        </div>

                        <div>
                          <dt>Fecha de creación</dt>
                          <dd>{fecha(incidente.fechaCreacion)}</dd>
                        </div>
                      </dl>
                    </article>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {sesionDisponible && (
            <nav
              className="inicio-ciudadano-paginacion"
              aria-label="Paginación de reportes"
            >
              <p role="status">
                {cargando
                  ? 'Consultando resultados…'
                  : error
                    ? 'Resultados no disponibles.'
                    : totalResultados === 0
                      ? '0 resultados'
                      : `Mostrando ${desde}–${hasta} de ${totalResultados} reportes`}
              </p>

              <div className="inicio-ciudadano-paginacion-controles">
                <button
                  type="button"
                  onClick={paginaAnterior}
                  disabled={!puedeRetroceder}
                >
                  Anterior
                </button>

                <span>
                  {cargando || error || totalPaginas === 0
                    ? 'Página —'
                    : `Página ${paginaActual} de ${totalPaginas}`}
                </span>

                <button
                  type="button"
                  onClick={paginaSiguiente}
                  disabled={!puedeAvanzar}
                >
                  Siguiente
                </button>
              </div>
            </nav>
          )}
        </div>

        {sesionDisponible && (
          <aside
            className="inicio-ciudadano-panel-mapa"
            aria-labelledby="inicio-ciudadano-mapa-titulo"
          >
            <h2 id="inicio-ciudadano-mapa-titulo">
              Mapa territorial
            </h2>

            <p className="inicio-ciudadano-mapa-descripcion">
              Ubicación de los reportes que coinciden con tu selección.
            </p>

            {cargando ? (
              <p role="status">Actualizando el mapa…</p>
            ) : error ? (
              <p>
                El mapa estará disponible cuando se puedan consultar
                los reportes.
              </p>
            ) : (
              <>
                <IncidentMap
                  markers={marcadores}
                  className="inicio-ciudadano-mapa"
                />

                <p className="inicio-ciudadano-mapa-resumen" role="status">
                  {marcadores.length === 0
                    ? 'No hay reportes con coordenadas válidas para mostrar.'
                    : `${marcadores.length} reporte(s) con ubicación.`}
                </p>

                {totalSinCoordenadas > 0 && (
                  <p className="inicio-ciudadano-mapa-nota">
                    {totalSinCoordenadas} reporte(s) sin coordenadas
                    válidas permanecen disponibles en el listado.
                  </p>
                )}

                <p className="inicio-ciudadano-mapa-nota">
                  El mapa incluye todas las coincidencias,
                  no solo la página visible.
                </p>
              </>
            )}
          </aside>
        )}
      </div>
    </section>
  )
}