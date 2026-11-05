import { useRef, useState, type FormEvent } from 'react'
import {
  cambiarPrioridadIncidente,
  esPrioridadIncidente,
  PRIORIDADES_INCIDENTE,
} from '../../services/incidentes.service'
import type { Incidente } from '../../types/incidente'
import type { IncidenteDetalle } from '../../types/incidenteDetalle'

interface Props {
  incidente: IncidenteDetalle
  alConfirmar: (incidente: Incidente) => void
}

function mensajeError(error: unknown): string {
  const codigo =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  switch (codigo) {
    case 400:
      return 'La prioridad debe ser un número entero entre 1 y 5.'
    case 401:
      return 'Tu sesión no es válida. Inicia sesión nuevamente.'
    case 403:
      return 'No tienes permisos para modificar la prioridad.'
    case 404:
      return 'El incidente ya no está disponible.'
    default:
      return 'No se pudo confirmar la prioridad. Consulta el incidente antes de reintentar. Se conserva la última prioridad confirmada.'
  }
}

export function CambioPrioridadIncidente({
  incidente,
  alConfirmar,
}: Props) {
  const bloqueoEnvio = useRef(false)
  const [seleccion, setSeleccion] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)

  const prioridadSeleccionada = Number(seleccion)

  const puedeEnviar =
    esPrioridadIncidente(prioridadSeleccionada) &&
    prioridadSeleccionada !== incidente.nivelPrioridad

  async function manejarEnvio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (bloqueoEnvio.current) return

    setError(null)
    setExito(null)

    if (!esPrioridadIncidente(prioridadSeleccionada)) {
      setError('Selecciona una prioridad entre 1 y 5.')
      return
    }

    if (prioridadSeleccionada === incidente.nivelPrioridad) {
      setError('Selecciona una prioridad diferente de la actual.')
      return
    }

    bloqueoEnvio.current = true
    setEnviando(true)

    try {
      const confirmado = await cambiarPrioridadIncidente(
        incidente.id,
        prioridadSeleccionada,
      )

      alConfirmar(confirmado)
      setSeleccion('')
      setExito('La prioridad se actualizó correctamente.')
    } catch (errorConsulta: unknown) {
      setError(mensajeError(errorConsulta))
    } finally {
      bloqueoEnvio.current = false
      setEnviando(false)
    }
  }

  return (
    <section
      className="detalle-tarjeta detalle-gestion"
      aria-labelledby="cambio-prioridad-titulo"
    >
      <h2 id="cambio-prioridad-titulo">Modificar prioridad</h2>

      <p>
        Prioridad actual confirmada:{' '}
        <strong>
          {incidente.nivelPrioridad == null
            ? 'Sin prioridad'
            : `Nivel ${incidente.nivelPrioridad}`}
        </strong>
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
          <legend>Datos del cambio de prioridad</legend>

          <div className="gestion-campo">
            <label htmlFor="incidente-prioridad-destino">
              Nueva prioridad
            </label>

            <select
              id="incidente-prioridad-destino"
              value={seleccion}
              onChange={(event) => {
                setSeleccion(event.target.value)
                setError(null)
                setExito(null)
              }}
              aria-describedby="incidente-prioridad-ayuda"
              required
            >
              <option value="">Selecciona una prioridad</option>

              {PRIORIDADES_INCIDENTE.map((prioridad) => (
                <option
                  key={prioridad}
                  value={prioridad}
                  disabled={prioridad === incidente.nivelPrioridad}
                >
                  Nivel {prioridad}
                  {prioridad === incidente.nivelPrioridad
                    ? ' (actual)'
                    : ''}
                </option>
              ))}
            </select>

            <p id="incidente-prioridad-ayuda">
              La escala va de 1, prioridad mínima, a 5, prioridad crítica.
            </p>
          </div>

          <button
            type="submit"
            disabled={enviando || !puedeEnviar}
          >
            {enviando ? 'Guardando prioridad…' : 'Guardar prioridad'}
          </button>
        </fieldset>
      </form>
    </section>
  )
}