import { useRef, useState, type FormEvent } from 'react'
import { useCandidatosResponsables } from '../../hooks/useCandidatosResponsables'
import { asignarResponsableIncidente } from '../../services/incidentes.service'
import type {
  AsignacionActiva,
  IncidenteDetalle,
} from '../../types/incidenteDetalle'

interface Props {
  incidente: IncidenteDetalle
  alConfirmar: (asignacion: AsignacionActiva) => void
}

function codigoError(error: unknown): number | undefined {
  return typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
    ? Number(error.statusCode)
    : undefined
}

function mensajeError(error: unknown): string {
  switch (codigoError(error)) {
    case 400:
      return 'El responsable fue rechazado. Puede que ya no tenga rol Administrador. Actualiza los candidatos y vuelve a seleccionar.'
    case 401:
      return 'Tu sesión no es válida. Inicia sesión nuevamente.'
    case 403:
      return 'No tienes permisos para realizar la asignación.'
    case 404:
      return 'El incidente o el usuario seleccionado ya no está disponible. Actualiza los candidatos.'
    case 409:
      return 'No se pudo completar la asignación por un conflicto. Consulta nuevamente el incidente.'
    default:
      return 'No se pudo confirmar la asignación. Consulta el incidente antes de reintentar. Se conserva la última asignación confirmada.'
  }
}

export function AsignacionIncidente({
  incidente,
  alConfirmar,
}: Props) {
  const {
    candidatos,
    cargando,
    error: errorCandidatos,
    pagina,
    totalPaginas,
    totalCandidatos,
    puedeRetroceder,
    puedeAvanzar,
    paginaAnterior,
    paginaSiguiente,
    actualizarCandidatos,
  } = useCandidatosResponsables()

  const bloqueoEnvio = useRef(false)
  const [seleccion, setSeleccion] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)

  const responsableActual = incidente.asignacionActiva?.activa
    ? incidente.asignacionActiva.usuarioAsignadoId
    : null

  const candidatoSeleccionado = candidatos.find(
    (candidato) => candidato.id === seleccion,
  )

  const puedeEnviar =
    !cargando &&
    !errorCandidatos &&
    candidatoSeleccionado !== undefined &&
    seleccion !== responsableActual

  function cambiarPagina(accion: () => void) {
    if (bloqueoEnvio.current) return

    setSeleccion('')
    accion()
  }

  function refrescarCandidatos() {
    if (bloqueoEnvio.current) return

    setSeleccion('')
    actualizarCandidatos()
  }

  async function manejarEnvio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (bloqueoEnvio.current) return

    setError(null)
    setExito(null)

    if (
      cargando ||
      errorCandidatos ||
      !candidatoSeleccionado ||
      seleccion === responsableActual
    ) {
      setError('Selecciona un administrador diferente del responsable actual.')
      return
    }

    bloqueoEnvio.current = true
    setEnviando(true)

    try {
      const confirmado = await asignarResponsableIncidente(
        incidente.id,
        candidatoSeleccionado.id,
      )

      alConfirmar(confirmado)
      setSeleccion('')
      setExito('El responsable se asignó correctamente.')
    } catch (errorConsulta: unknown) {
      setError(mensajeError(errorConsulta))

      const codigo = codigoError(errorConsulta)

      if (codigo === 400 || codigo === 404) {
        setSeleccion('')
        actualizarCandidatos()
      }
    } finally {
      bloqueoEnvio.current = false
      setEnviando(false)
    }
  }

  return (
    <section
      className="detalle-tarjeta detalle-gestion"
      aria-labelledby="asignacion-incidente-titulo"
    >
      <h2 id="asignacion-incidente-titulo">
        {responsableActual ? 'Reasignar responsable' : 'Asignar responsable'}
      </h2>

      <p>
        Responsable actual confirmado:{' '}
        <strong>{responsableActual || 'Sin responsable asignado'}</strong>
      </p>

      {exito && (
        <p className="gestion-exito" role="status">
          {exito}
        </p>
      )}

      {error && (
        <p className="gestion-error" role="alert">
          {error}
        </p>
      )}

      <form onSubmit={manejarEnvio} aria-busy={enviando}>
        <fieldset disabled={enviando}>
          <legend>Selección del responsable</legend>

          {cargando ? (
            <p role="status">Cargando administradores…</p>
          ) : errorCandidatos ? (
            <p className="gestion-error" role="alert">
              {errorCandidatos}
            </p>
          ) : (
            <>
              <div className="gestion-campo">
                <label htmlFor="incidente-responsable">
                  {responsableActual
                    ? 'Nuevo responsable'
                    : 'Responsable'}
                </label>

                <select
                  id="incidente-responsable"
                  value={seleccion}
                  onChange={(event) => {
                    setSeleccion(event.target.value)
                    setError(null)
                    setExito(null)
                  }}
                  disabled={candidatos.length === 0}
                  required
                >
                  <option value="">Selecciona un administrador</option>

                  {candidatos.map((candidato) => (
                    <option
                      key={candidato.id}
                      value={candidato.id}
                      disabled={candidato.id === responsableActual}
                    >
                      {candidato.nombre || candidato.email} —{' '}
                      {candidato.email}
                      {candidato.id === responsableActual
                        ? ' (actual)'
                        : ''}
                    </option>
                  ))}
                </select>
              </div>

              {candidatos.length === 0 && (
                <p role="status">
                  No hay administradores disponibles en esta página.
                </p>
              )}

              <p role="status">
                {totalCandidatos === 0
                  ? '0 candidatos'
                  : `Página ${pagina} de ${totalPaginas} · ${totalCandidatos} candidatos`}
              </p>
            </>
          )}

          <div className="gestion-controles">
            <button
              type="button"
              onClick={() => cambiarPagina(paginaAnterior)}
              disabled={enviando || !puedeRetroceder}
            >
              Anterior
            </button>

            <button
              type="button"
              onClick={() => cambiarPagina(paginaSiguiente)}
              disabled={enviando || !puedeAvanzar}
            >
              Siguiente
            </button>

            <button
              type="button"
              onClick={refrescarCandidatos}
              disabled={enviando || cargando}
            >
              Actualizar candidatos
            </button>
          </div>

          <button
            type="submit"
            disabled={enviando || !puedeEnviar}
          >
            {enviando
              ? 'Guardando asignación…'
              : responsableActual
                ? 'Reasignar responsable'
                : 'Asignar responsable'}
          </button>
        </fieldset>
      </form>
    </section>
  )
}
