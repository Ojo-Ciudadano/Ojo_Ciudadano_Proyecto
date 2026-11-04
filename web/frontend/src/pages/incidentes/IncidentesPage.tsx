import { Link } from 'react-router-dom'
import { useIncidentes } from '../../hooks/useIncidentes'
import type { Incidente } from '../../types/incidente'
import './IncidentesPage.css'

function texto(valor?: string | null): string {
  return valor?.trim() || 'Sin información'
}

function TablaIncidentes({ incidentes }: { incidentes: Incidente[] }) {
  return (
    <div className="incidentes-tabla-contenedor">
      <table className="incidentes-tabla">
        <caption>Listado administrativo de incidentes</caption>

        <thead>
          <tr>
            <th scope="col">Folio</th>
            <th scope="col">Categoría</th>
            <th scope="col">Comuna</th>
            <th scope="col">Prioridad</th>
            <th scope="col">Estado</th>
          </tr>
        </thead>

        <tbody>
          {incidentes.map((incidente) => (
            <tr key={incidente.id} data-incidente-id={incidente.id}>
              <td className="incidente-folio">
                <Link
                  to={`/incidentes/${encodeURIComponent(incidente.id)}`}
                  aria-label={`Ver detalle del incidente ${incidente.id}`}
                >
                  {incidente.id}
                </Link>
              </td>

              <td>{texto(incidente.categoria?.nombre)}</td>

              <td>{texto(incidente.ubicacion?.comuna)}</td>

              <td>
                <span className="incidente-prioridad">
                  <span
                    className="incidente-prioridad-punto"
                    aria-hidden="true"
                  />
                  {incidente.nivelPrioridad == null
                    ? 'Sin prioridad'
                    : `Nivel ${incidente.nivelPrioridad}`}
                </span>
              </td>

              <td>
                <span className="incidente-estado">
                  {texto(incidente.estado?.nombre)}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function IncidentesPage() {
  const {
    incidentesPagina,
    cargando,
    error,
    reintentar,
    filtros,
    estados,
    categorias,
    comunas,
    hayFiltros,
    cambiarFiltro,
    restablecerFiltros,
    paginaActual,
    totalPaginas,
    totalResultados,
    desde,
    hasta,
    puedeRetroceder,
    puedeAvanzar,
    paginaAnterior,
    paginaSiguiente,
  } = useIncidentes()

  return (
    <section
      className="incidentes-pagina"
      aria-labelledby="incidentes-titulo"
    >
      <header className="incidentes-encabezado">
        <h1 id="incidentes-titulo">Gestión de incidentes</h1>
        <p>
          Consulta los reportes ciudadanos, su prioridad y su estado actual.
        </p>
      </header>

      <div
        className="incidentes-filtros"
        role="group"
        aria-label="Filtros de incidentes"
      >
        <div className="incidentes-filtro">
          <label htmlFor="filtro-estado">Estado</label>

          <select
            id="filtro-estado"
            value={filtros.estadoId}
            onChange={(event) =>
              cambiarFiltro('estadoId', event.target.value)
            }
          >
            <option value="">Todos los estados</option>

            {estados.map((estado) => (
              <option key={estado.id} value={estado.id}>
                {estado.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="incidentes-filtro">
          <label htmlFor="filtro-comuna">Comuna</label>

          <select
            id="filtro-comuna"
            value={filtros.comuna}
            onChange={(event) =>
              cambiarFiltro('comuna', event.target.value)
            }
          >
            <option value="">Todas las comunas</option>

            {comunas.map((comuna) => (
              <option key={comuna} value={comuna}>
                {comuna}
              </option>
            ))}
          </select>
        </div>

        <div className="incidentes-filtro">
          <label htmlFor="filtro-categoria">Categoría</label>

          <select
            id="filtro-categoria"
            value={filtros.categoriaId}
            onChange={(event) =>
              cambiarFiltro('categoriaId', event.target.value)
            }
          >
            <option value="">Todas las categorías</option>

            {categorias.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nombre}
              </option>
            ))}
          </select>
        </div>

        <button
          type="button"
          className="incidentes-restablecer"
          onClick={restablecerFiltros}
          disabled={!hayFiltros}
        >
          Restablecer filtros
        </button>
      </div>

      <div aria-busy={cargando}>
        {cargando ? (
          <div className="incidentes-mensaje" role="status">
            Cargando incidentes…
          </div>
        ) : error ? (
          <div className="incidentes-mensaje incidentes-error" role="alert">
            <p>{error.mensaje}</p>

            {error.permiteReintentar && (
              <button type="button" onClick={reintentar}>
                Reintentar
              </button>
            )}
          </div>
        ) : totalResultados === 0 ? (
          <div className="incidentes-mensaje" role="status">
            {hayFiltros
              ? 'No se encontraron incidentes con los filtros seleccionados.'
              : 'No hay incidentes registrados.'}
          </div>
        ) : (
          <TablaIncidentes incidentes={incidentesPagina} />
        )}
      </div>

      <nav
        className="incidentes-paginacion"
        aria-label="Paginación de incidentes"
      >
        <p className="incidentes-resumen" role="status">
          {cargando
            ? 'Consultando resultados…'
            : error
              ? 'Resultados no disponibles.'
              : totalResultados === 0
                ? '0 resultados'
                : `Mostrando ${desde}–${hasta} de ${totalResultados} incidentes`}
        </p>

        <div className="incidentes-paginacion-controles">
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
    </section>
  )
}