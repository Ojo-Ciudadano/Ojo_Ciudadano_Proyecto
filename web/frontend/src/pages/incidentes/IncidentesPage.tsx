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
                {incidente.id}
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
  const { incidentes, cargando, error, reintentar } = useIncidentes()

  return (
    <section className="incidentes-pagina" aria-labelledby="incidentes-titulo">
      <header className="incidentes-encabezado">
        <h1 id="incidentes-titulo">Gestión de incidentes</h1>
        <p>Consulta los reportes ciudadanos, su prioridad y su estado actual.</p>
      </header>

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
      ) : incidentes.length === 0 ? (
        <div className="incidentes-mensaje" role="status">
          No hay incidentes registrados.
        </div>
      ) : (
        <TablaIncidentes incidentes={incidentes} />
      )}
    </section>
  )
}