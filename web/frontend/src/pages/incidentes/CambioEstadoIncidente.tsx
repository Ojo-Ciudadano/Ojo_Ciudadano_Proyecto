import { useState, type FormEvent } from 'react'
import {
  esEstadoIncidente,
  obtenerTransicionesPermitidas,
} from '../../config/transicionesIncidente'
import { useCambioEstadoIncidente } from '../../hooks/useCambioEstadoIncidente'
import type { Incidente } from '../../types/incidente'
import type { IncidenteDetalle } from '../../types/incidenteDetalle'

interface Props {
  incidente: IncidenteDetalle
  alConfirmar: (incidente: Incidente) => void
  alConflicto: () => Promise<void>
}

export function CambioEstadoIncidente({
  incidente,
  alConfirmar,
  alConflicto,
}: Props) {
  const [estadoSeleccionado, setEstadoSeleccionado] = useState('')
  const [observacion, setObservacion] = useState('')
  const [errorFormulario, setErrorFormulario] = useState<string | null>(
    null,
  )

  const { enviando, error, exito, enviarCambio } =
    useCambioEstadoIncidente()

  const estadoActual = incidente.estado?.nombre
  const destinos = obtenerTransicionesPermitidas(estadoActual)

  const destinoValido =
    esEstadoIncidente(estadoSeleccionado) &&
    destinos.includes(estadoSeleccionado)

  async function manejarEnvio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (enviando) return

    setErrorFormulario(null)

    if (
      !esEstadoIncidente(estadoSeleccionado) ||
      !destinos.includes(estadoSeleccionado)
    ) {
      setErrorFormulario('Selecciona un estado destino permitido.')
      return
    }

    if (!observacion.trim()) {
      setErrorFormulario('Debes ingresar una observación.')
      return
    }

    const confirmado = await enviarCambio(
      incidente.id,
      {
        estado: estadoSeleccionado,
        observacion,
      },
      alConfirmar,
      alConflicto,
    )

    if (confirmado) {
      setObservacion('')
      setEstadoSeleccionado('')
    }
  }

  return (
    <section
      className="detalle-tarjeta detalle-cambio-estado"
      aria-labelledby="cambio-estado-titulo"
    >
      <h2 id="cambio-estado-titulo">Cambiar estado</h2>

      <p>
        Estado actual confirmado:{' '}
        <strong>{estadoActual || 'Sin información'}</strong>
      </p>

      {exito && (
        <p className="cambio-estado-exito" role="status">
          {exito}
        </p>
      )}

      {error && (
        <p className="cambio-estado-error" role="alert">
          {error}
        </p>
      )}

      {destinos.length === 0 ? (
        <p role="status">
          {esEstadoIncidente(estadoActual)
            ? 'Este estado no tiene transiciones disponibles.'
            : 'No se pudo identificar el estado actual. Vuelve a consultar el incidente para comprobar las transiciones disponibles.'}
        </p>
      ) : (
        <form onSubmit={manejarEnvio} aria-busy={enviando}>
          <fieldset disabled={enviando}>
            <legend>Datos del cambio de estado</legend>

            <div className="cambio-estado-campo">
              <label htmlFor="incidente-estado-destino">
                Nuevo estado
              </label>

              <select
                id="incidente-estado-destino"
                value={destinoValido ? estadoSeleccionado : ''}
                onChange={(event) => {
                  setEstadoSeleccionado(event.target.value)
                  setErrorFormulario(null)
                }}
                required
              >
                <option value="">Selecciona un estado</option>

                {destinos.map((estado) => (
                  <option key={estado} value={estado}>
                    {estado}
                  </option>
                ))}
              </select>
            </div>

            <div className="cambio-estado-campo">
              <label htmlFor="incidente-observacion">
                Observación
              </label>

              <textarea
                id="incidente-observacion"
                value={observacion}
                onChange={(event) => {
                  setObservacion(event.target.value)
                  setErrorFormulario(null)
                }}
                aria-describedby="incidente-observacion-ayuda"
                rows={4}
                required
              />

              <p id="incidente-observacion-ayuda">
                Explica el motivo del cambio. La observación es
                obligatoria y no puede contener solamente espacios.
              </p>
            </div>

            {errorFormulario && (
              <p className="cambio-estado-error" role="alert">
                {errorFormulario}
              </p>
            )}

            <button
              type="submit"
              disabled={
                enviando || !destinoValido || !observacion.trim()
              }
            >
              {enviando ? 'Guardando cambio…' : 'Confirmar cambio de estado'}
            </button>
          </fieldset>
        </form>
      )}
    </section>
  )
}
