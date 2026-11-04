import { Link, useParams } from 'react-router-dom'
import { useIncidenteDetalle } from '../../hooks/useIncidenteDetalle'
import type { IncidenteDetalle } from '../../types/incidenteDetalle'
import { AsignacionIncidente } from './AsignacionIncidente'
import { CambioPrioridadIncidente } from './CambioPrioridadIncidente'
import { CambioEstadoIncidente } from './CambioEstadoIncidente'
import './IncidenteDetallePage.css'

function texto(valor?: string | null): string {
  return valor?.trim() || 'Sin información'
}

function fecha(valor?: string | null): string {
  if (!valor) return 'Sin información'

  const fechaInterpretada = new Date(valor)

  if (Number.isNaN(fechaInterpretada.getTime())) {
    return 'Sin información'
  }

  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fechaInterpretada)
}

function coordenada(valor?: number | null): string {
  return typeof valor === 'number' && Number.isFinite(valor)
    ? String(valor)
    : 'Sin información'
}

function DatosIncidente({ incidente }: { incidente: IncidenteDetalle }) {
  const ubicacion = incidente.ubicacion
  const asignacion = incidente.asignacionActiva

  return (
    <div
      className="detalle-contenido"
      data-incidente-id={incidente.id}
      data-estado-id={incidente.estadoId}
    >
      <header className="detalle-encabezado">
        <p className="detalle-etiqueta">Detalle del incidente</p>
        <h1>{texto(incidente.titulo)}</h1>
        <p className="detalle-folio">Folio: {incidente.id}</p>
      </header>

      <div className="detalle-cuadricula">
        <section
          className="detalle-tarjeta"
          aria-labelledby="detalle-datos"
        >
          <h2 id="detalle-datos">Información general</h2>

          <dl className="detalle-datos">
            <div>
              <dt>Categoría</dt>
              <dd>{texto(incidente.categoria?.nombre)}</dd>
            </div>

            <div>
              <dt>Estado</dt>
              <dd>{texto(incidente.estado?.nombre)}</dd>
            </div>

            <div>
              <dt>Prioridad</dt>
              <dd>
                {incidente.nivelPrioridad == null
                  ? 'Sin prioridad'
                  : `Nivel ${incidente.nivelPrioridad}`}
              </dd>
            </div>

            <div>
              <dt>Fecha de creación</dt>
              <dd>{fecha(incidente.fechaCreacion)}</dd>
            </div>

            <div>
              <dt>Última actualización</dt>
              <dd>{fecha(incidente.fechaActualizacion)}</dd>
            </div>
          </dl>
        </section>

        <section
          className="detalle-tarjeta"
          aria-labelledby="detalle-ubicacion"
        >
          <h2 id="detalle-ubicacion">Ubicación</h2>

          <dl className="detalle-datos">
            <div>
              <dt>Región</dt>
              <dd>{texto(ubicacion?.region)}</dd>
            </div>

            <div>
              <dt>Comuna</dt>
              <dd>{texto(ubicacion?.comuna)}</dd>
            </div>

            <div>
              <dt>Sector</dt>
              <dd>{texto(ubicacion?.sector)}</dd>
            </div>

            <div>
              <dt>Dirección de referencia</dt>
              <dd>{texto(ubicacion?.direccionReferencia)}</dd>
            </div>

            <div>
              <dt>Latitud</dt>
              <dd>{coordenada(ubicacion?.latitud)}</dd>
            </div>

            <div>
              <dt>Longitud</dt>
              <dd>{coordenada(ubicacion?.longitud)}</dd>
            </div>
          </dl>
        </section>

        <section
          className="detalle-tarjeta detalle-ancho-completo"
          aria-labelledby="detalle-descripcion"
        >
          <h2 id="detalle-descripcion">Descripción</h2>
          <p className="detalle-descripcion">
            {texto(incidente.descripcion)}
          </p>
        </section>

        <section
          className="detalle-tarjeta"
          aria-labelledby="detalle-reportante"
        >
          <h2 id="detalle-reportante">Reportante</h2>

          <dl className="detalle-datos">
            <div>
              <dt>Identificador del reportante</dt>
              <dd>{texto(incidente.usuarioReportaId)}</dd>
            </div>
          </dl>
        </section>

        <section
          className="detalle-tarjeta"
          aria-labelledby="detalle-asignacion"
        >
          <h2 id="detalle-asignacion">Asignación activa</h2>

          {asignacion?.activa ? (
            <dl className="detalle-datos">
              <div>
                <dt>Identificador del responsable</dt>
                <dd>{texto(asignacion.usuarioAsignadoId)}</dd>
              </div>

              <div>
                <dt>Asignado por</dt>
                <dd>{texto(asignacion.asignadoPorId)}</dd>
              </div>

              <div>
                <dt>Fecha de asignación</dt>
                <dd>{fecha(asignacion.fechaAsignacion)}</dd>
              </div>
            </dl>
          ) : (
            <p className="detalle-sin-asignacion">
              Sin responsable asignado.
            </p>
          )}
        </section>
      </div>

      {/* Evidencias e historial se integrarán en sus respectivas issues. */}
    </div>
  )
}

export function IncidenteDetallePage() {
  const { id } = useParams<{ id: string }>()

  const {
    incidente,
    cargando,
    error,
    noEncontrado,
    reintentar,
    actualizarEstadoConfirmado,
    actualizarAsignacionConfirmada,
    actualizarPrioridadConfirmada,
    recargarDetalle,
  } = useIncidenteDetalle(id)

  return (
    <section
      className="detalle-pagina"
      aria-label="Detalle del incidente"
    >
      <Link className="detalle-volver" to="/incidentes">
        ← Volver a la tabla de incidentes
      </Link>

      <div aria-busy={cargando}>
        {cargando ? (
          <div className="detalle-mensaje" role="status">
            Cargando incidente…
          </div>
        ) : noEncontrado ? (
          <div className="detalle-mensaje" role="status">
            <h1>Incidente no encontrado</h1>
            <p>
              El incidente solicitado no existe o ya no está disponible.
            </p>
          </div>
        ) : error ? (
          <div
            className="detalle-mensaje detalle-error"
            role="alert"
          >
            <h1>No se pudo consultar el incidente</h1>
            <p>{error.mensaje}</p>

            {error.permiteReintentar && (
              <button type="button" onClick={reintentar}>
                Reintentar
              </button>
            )}
          </div>
        ) : incidente ? (
          <>
            <DatosIncidente incidente={incidente} />

            <div className="detalle-gestion-cuadricula">
              <AsignacionIncidente
                key={`asignacion-${incidente.id}`}
                incidente={incidente}
                alConfirmar={actualizarAsignacionConfirmada}
              />

              <CambioPrioridadIncidente
                key={`prioridad-${incidente.id}`}
                incidente={incidente}
                alConfirmar={actualizarPrioridadConfirmada}
              />
            </div>

            <CambioEstadoIncidente
              key={incidente.id}
              incidente={incidente}
              alConfirmar={actualizarEstadoConfirmado}
              alConflicto={recargarDetalle}
            />
          </>
        ) : null}
      </div>
    </section>
  )
}