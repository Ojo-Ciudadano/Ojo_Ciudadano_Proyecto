import { Link } from 'react-router-dom'
import { EstadoIncidenteBadge } from '../../components/ui/EstadoIncidenteBadge'
import { EstadoVistaMensaje } from '../../components/ui/EstadoVistaMensaje'
import { obtenerVarianteEstado } from '../../components/ui/incidenteIndicadores.utils'
import { PrioridadIncidenteBadge } from '../../components/ui/PrioridadIncidenteBadge'
import { useIncidentes } from '../../hooks/useIncidentes'
import type { Incidente } from '../../types/incidente'
import './IncidentesPage.css'

function texto(valor?: string | null): string {
  return valor?.trim() || 'Sin información'
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
                <PrioridadIncidenteBadge nivel={incidente.nivelPrioridad} />
              </td>

              <td>
                <EstadoIncidenteBadge nombre={incidente.estado?.nombre} />
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
      switch (obtenerVarianteEstado(incidente.estado?.nombre)) {
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
          <EstadoVistaMensaje
            tipo="loading"
            mensaje="Cargando incidentes…"
          />
        ) : error ? (
          <EstadoVistaMensaje tipo="error" mensaje={error.mensaje}>
            {error.permiteReintentar && (
              <button
                type="button"
                className="ui-btn ui-btn--danger"
                onClick={reintentar}
              >
                Reintentar
              </button>
            )}
          </EstadoVistaMensaje>
        ) : totalResultados === 0 ? (
          <EstadoVistaMensaje
            tipo="empty"
            mensaje={
              hayFiltros
                ? 'No se encontraron incidentes con los filtros seleccionados.'
                : 'No hay incidentes registrados.'
            }
          />
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