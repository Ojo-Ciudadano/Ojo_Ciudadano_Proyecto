import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useIncidentes } from '../../hooks/useIncidentes'
import { IncidentMap } from '../../components/map/IncidentMap'
import { mapearIncidentesAMarcadores } from '../../components/map/incidentMarkers'
import './DashboardPage.css'

const POR_PAGINA = 10

function texto(valor?: string | null): string {
  return valor?.trim() || 'Sin información'
}

function normalizar(valor?: string | null): string {
  return (valor ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

function claseEstado(nombre?: string | null): string {
  const estado = normalizar(nombre).replace(/[_-]/g, ' ')

  if (estado === 'nuevo' || estado === 'en revision') return 'nuevo'
  if (estado === 'validado' || estado === 'asignado') return 'asignado'
  if (estado === 'en atencion') return 'atencion'
  if (estado === 'resuelto' || estado === 'cerrado') return 'cerrado'
  if (estado === 'rechazado') return 'rechazado'

  return 'otro'
}

function fecha(valor?: string | null): string {
  if (!valor) return 'Fecha no disponible'

  const resultado = new Date(valor)

  if (Number.isNaN(resultado.getTime())) {
    return 'Fecha no disponible'
  }

  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(resultado)
}

function ContenidoInicioAdministrador() {
  const { user } = useAuth()

  const {
    incidentes,
    cargando,
    error,
    estados,
    filtros,
    cambiarFiltro,
    reintentar,
  } = useIncidentes()

  const [busqueda, setBusqueda] = useState('')
  const [pagina, setPagina] = useState(1)

  const termino = normalizar(busqueda)

  const resultados = useMemo(() => {
    if (cargando || error) return []

    return incidentes.filter((incidente) => {
      if (!termino) return true

      return [
        incidente.titulo,
        incidente.categoria?.nombre,
        incidente.ubicacion?.comuna,
        incidente.id,
      ].some((valor) => normalizar(valor).includes(termino))
    })
  }, [incidentes, cargando, error, termino])

  const marcadores = useMemo(
    () => mapearIncidentesAMarcadores(resultados),
    [resultados],
  )

  const totalPaginas = Math.ceil(resultados.length / POR_PAGINA)
  const paginaActual = Math.min(pagina, Math.max(1, totalPaginas))
  const inicio = (paginaActual - 1) * POR_PAGINA
  const visibles = resultados.slice(inicio, inicio + POR_PAGINA)
  const sinCoordenadas = resultados.length - marcadores.length

  function seleccionarEstado(valor: string) {
    setPagina(1)
    cambiarFiltro('estadoId', valor)
  }

  function restablecer() {
    setBusqueda('')
    seleccionarEstado('')
  }

  return (
    <section
      className="inicio-admin"
      aria-labelledby="inicio-admin-titulo"
    >
      <header className="inicio-admin-encabezado">
        <div>
          <p className="inicio-admin-etiqueta">Panel administrativo</p>
          <h1 id="inicio-admin-titulo">Inicio</h1>
          <p>
            Bienvenido, {user?.nombre || 'Administrador'}. Consulta los
            reportes y su ubicación en La Araucanía.
          </p>
        </div>

        <Link className="inicio-admin-boton-principal" to="/incidentes">
          Gestionar incidentes
        </Link>
      </header>

      <div
        className="inicio-admin-filtros"
        role="group"
        aria-label="Filtros del inicio"
      >
        <div className="inicio-admin-buscador">
          <label htmlFor="inicio-admin-busqueda">Buscar incidentes</label>
          <input
            id="inicio-admin-busqueda"
            type="search"
            placeholder="Buscar por título, folio, categoría o comuna…"
            value={busqueda}
            onChange={(event) => {
              setBusqueda(event.target.value)
              setPagina(1)
            }}
          />
        </div>

        <div className="inicio-admin-selector">
          <label htmlFor="inicio-admin-estado">Estado</label>
          <select
            id="inicio-admin-estado"
            value={filtros.estadoId}
            onChange={(event) => seleccionarEstado(event.target.value)}
          >
            <option value="">Todos los estados</option>
            {estados.map((estado) => (
              <option key={estado.id} value={estado.id}>
                {estado.nombre}
              </option>
            ))}
          </select>
        </div>

        <button
          type="button"
          className="inicio-admin-restablecer"
          onClick={restablecer}
          disabled={!busqueda && !filtros.estadoId}
        >
          Restablecer
        </button>
      </div>

      <div className="inicio-admin-distribucion">
        <div className="inicio-admin-resultados" aria-busy={cargando}>
          {cargando ? (
            <div className="inicio-admin-mensaje" role="status">
              Cargando incidentes…
            </div>
          ) : error ? (
            <div
              className="inicio-admin-mensaje inicio-admin-error"
              role="alert"
            >
              <h2>No se pudieron cargar los incidentes</h2>
              <p>{error.mensaje}</p>
              {error.permiteReintentar && (
                <button type="button" onClick={reintentar}>
                  Reintentar
                </button>
              )}
            </div>
          ) : resultados.length === 0 ? (
            <div className="inicio-admin-mensaje" role="status">
              <h2>No hay incidentes para mostrar</h2>
              <p>
                {busqueda || filtros.estadoId
                  ? 'Prueba con otra búsqueda o restablece los filtros.'
                  : 'Los reportes disponibles aparecerán aquí.'}
              </p>
            </div>
          ) : (
            <ul className="inicio-admin-lista">
              {visibles.map((incidente) => (
                <li
                  key={incidente.id}
                  className={`inicio-admin-tarjeta inicio-admin-tarjeta--${claseEstado(incidente.estado?.nombre)}`}
                >
                  <article>
                    <div className="inicio-admin-tarjeta-cabecera">
                      <h2>{texto(incidente.titulo)}</h2>
                      <span
                        className={`inicio-admin-estado inicio-admin-estado--${claseEstado(incidente.estado?.nombre)}`}
                      >
                        {texto(incidente.estado?.nombre)}
                      </span>
                    </div>

                    <p className="inicio-admin-meta">
                      {texto(incidente.ubicacion?.comuna)}
                      {' · '}
                      {fecha(incidente.fechaCreacion)}
                    </p>

                    {incidente.descripcion?.trim() && (
                      <p className="inicio-admin-descripcion">
                        {incidente.descripcion}
                      </p>
                    )}

                    <dl className="inicio-admin-datos">
                      <div>
                        <dt>Categoría</dt>
                        <dd>{texto(incidente.categoria?.nombre)}</dd>
                      </div>
                      <div>
                        <dt>Prioridad</dt>
                        <dd>
                          {incidente.nivelPrioridad == null
                            ? 'Sin prioridad'
                            : `Nivel ${incidente.nivelPrioridad}`}
                        </dd>
                      </div>
                    </dl>

                    <footer className="inicio-admin-tarjeta-pie">
                      <p>Folio: {incidente.id}</p>
                      <Link
                        to={`/incidentes/${encodeURIComponent(incidente.id)}`}
                        aria-label={`Ver detalle de ${texto(incidente.titulo)}, folio ${incidente.id}`}
                      >
                        Ver detalle
                      </Link>
                    </footer>
                  </article>
                </li>
              ))}
            </ul>
          )}

          {!cargando && !error && resultados.length > 0 && (
            <nav
              className="inicio-admin-paginacion"
              aria-label="Paginación del inicio"
            >
              <p role="status">
                Mostrando {inicio + 1}–
                {Math.min(inicio + POR_PAGINA, resultados.length)} de{' '}
                {resultados.length} incidentes
              </p>

              <div>
                <button
                  type="button"
                  disabled={paginaActual <= 1}
                  onClick={() => setPagina(paginaActual - 1)}
                >
                  Anterior
                </button>
                <span>
                  {paginaActual} de {totalPaginas}
                </span>
                <button
                  type="button"
                  disabled={paginaActual >= totalPaginas}
                  onClick={() => setPagina(paginaActual + 1)}
                >
                  Siguiente
                </button>
              </div>
            </nav>
          )}
        </div>

        <aside
          className="inicio-admin-panel-mapa"
          aria-labelledby="inicio-admin-mapa-titulo"
          aria-busy={cargando}
        >
          <div className="inicio-admin-mapa-encabezado">
            <span className="inicio-admin-mapa-distintivo">
              La Araucanía
            </span>
            <h2 id="inicio-admin-mapa-titulo">Mapa territorial</h2>
            <p>Ubicación de los incidentes de la selección actual.</p>
          </div>

          {cargando ? (
            <p className="inicio-admin-mapa-aviso" role="status">
              Actualizando el mapa…
            </p>
          ) : error ? (
            <p className="inicio-admin-mapa-aviso">
              El mapa estará disponible cuando se recuperen los incidentes.
            </p>
          ) : (
            <>
              <IncidentMap
                markers={marcadores}
                className="inicio-admin-mapa"
              />

              <p className="inicio-admin-mapa-aviso" role="status">
                {marcadores.length === 0
                  ? 'No hay incidentes con coordenadas válidas.'
                  : `${marcadores.length} incidente(s) con ubicación.`}
              </p>

              {sinCoordenadas > 0 && (
                <p className="inicio-admin-mapa-aviso">
                  {sinCoordenadas} incidente(s) sin coordenadas válidas
                  permanecen disponibles en el listado.
                </p>
              )}

              <p className="inicio-admin-mapa-aviso">
                El mapa incluye todas las coincidencias de la búsqueda.
              </p>
            </>
          )}

          <Link className="inicio-admin-enlace-mapa" to="/mapa">
            Abrir mapa de incidentes →
          </Link>
        </aside>
      </div>
    </section>
  )
}

export function DashboardPage() {
  const { user, isAuthenticated, isLoading, isAdmin } = useAuth()

  if (isLoading) {
    return <p role="status">Cargando sesión…</p>
  }

  if (!isAuthenticated || !isAdmin || !user) {
    return <p role="status">Se requiere una sesión de Administrador.</p>
  }

  return <ContenidoInicioAdministrador key={user.id} />
}