import { Link } from 'react-router-dom'
import { useIncidentes } from '../../hooks/useIncidentes'
import type { Incidente } from '../../types/incidente'
import './IncidentesPage.css'

function texto(valor?: string | null): string {
  return valor?.trim() || 'Sin información'
}

function normalizar(valor?: string | null): string {
  return (valor ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLowerCase()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
}

function tipoEstado(nombre?: string | null): string {
  switch (normalizar(nombre)) {
    case 'nuevo':
    case 'en revision':
      return 'nuevo'
    case 'validado':
    case 'asignado':
      return 'asignado'
    case 'en atencion':
      return 'atencion'
    case 'resuelto':
    case 'cerrado':
      return 'cerrado'
    case 'rechazado':
      return 'rechazado'
    default:
      return 'otro'
  }
}

function tipoPrioridad(nivel?: number | null): string {
  switch (nivel) {
    case 5:
      return 'critica'
    case 4:
      return 'alta'
    case 3:
      return 'media'
    case 2:
      return 'moderada'
    case 1:
      return 'baja'
    default:
      return 'desconocida'
  }
}

function mostrarFecha(valor?: string | null): string {
  if (!valor) return 'Sin fecha'

  const fecha = new Date(valor)

  if (Number.isNaN(fecha.getTime())) return 'Sin fecha'

  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(fecha)
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
            <th scope="col">Fecha de creación</th>
            <th scope="col">Acciones</th>
          </tr>
        </thead>

        <tbody>
          {incidentes.map((incidente) => (
            <tr key={incidente.id} data-incidente-id={incidente.id}>
              <td className="incidente-folio">
                <Link
                  to={`/incidentes/${encodeURIComponent(incidente.id)}`}
                  title={incidente.id}
                  aria-label={`Ver detalle del incidente ${incidente.id}`}
                >
                  {incidente.id}
                </Link>
              </td>

              <td>{texto(incidente.categoria?.nombre)}</td>

              <td>{texto(incidente.ubicacion?.comuna)}</td>

              <td>
                <span
                  className={`incidente-prioridad incidente-prioridad--${tipoPrioridad(incidente.nivelPrioridad)}`}
                >
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
                <span
                  className={`incidente-estado incidente-estado--${tipoEstado(incidente.estado?.nombre)}`}
                >
                  {texto(incidente.estado?.nombre)}
                </span>
              </td>

              <td className="incidente-fecha">
                {mostrarFecha(incidente.fechaCreacion)}
              </td>

              <td>
                <Link
                  className="incidente-ver"
                  to={`/incidentes/${encodeURIComponent(incidente.id)}`}
                  aria-label={`Ver detalle del incidente ${incidente.id}`}
                >
                  Ver
                </Link>
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
    incidentes,
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

  const resumen = {
    nuevos: 0,
    asignados: 0,
    atencion: 0,
    cerrados: 0,
  }

  if (!cargando && !error) {
    for (const incidente of incidentes) {
      switch (tipoEstado(incidente.estado?.nombre)) {
        case 'nuevo':
          resumen.nuevos += 1
          break
        case 'asignado':
          resumen.asignados += 1
          break
        case 'atencion':
          resumen.atencion += 1
          break
        case 'cerrado':
          resumen.cerrados += 1
          break
      }
    }
  }

  const tarjetas = [
    {
      clave: 'nuevos',
      cantidad: resumen.nuevos,
      etiqueta: 'Nuevos / en revisión',
    },
    {
      clave: 'asignados',
      cantidad: resumen.asignados,
      etiqueta: 'Validados / asignados',
    },
    {
      clave: 'atencion',
      cantidad: resumen.atencion,
      etiqueta: 'En atención',
    },
    {
      clave: 'cerrados',
      cantidad: resumen.cerrados,
      etiqueta: 'Resueltos / cerrados',
    },
  ]

  return (
    <section
      className="incidentes-pagina"
      aria-labelledby="incidentes-titulo"
    >
      <header className="incidentes-encabezado">
        <p className="incidentes-etiqueta">Panel administrativo</p>
        <h1 id="incidentes-titulo">Gestión de incidentes</h1>
        <p>Revisa y realiza el seguimiento de los reportes ciudadanos.</p>
      </header>

      <div
        className="incidentes-indicadores"
        role="group"
        aria-label="Resumen de los resultados actuales"
        aria-busy={cargando}
      >
        {tarjetas.map((tarjeta) => (
          <div
            key={tarjeta.clave}
            className={`incidentes-indicador incidentes-indicador--${tarjeta.clave}`}
          >
            <strong>
              {cargando || error ? '—' : tarjeta.cantidad}
            </strong>
            <span>{tarjeta.etiqueta}</span>
          </div>
        ))}
      </div>

      <p className="incidentes-nota-resumen">
        Resumen de todos los resultados de la consulta actual, antes de
        paginar. Los estados rechazados u otros no se incluyen en estas
        cuatro tarjetas.
      </p>

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