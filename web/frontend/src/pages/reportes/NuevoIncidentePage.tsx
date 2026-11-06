import { useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { REGIONES_COMUNAS } from '../../config/regionesComunas'
import { useAuth } from '../../contexts/AuthContext'
import { useCategoriasReporte } from '../../hooks/useCategoriasReporte'
import { useFotografiasReporte } from '../../hooks/useFotografiasReporte'
import { useUbicacionReporte } from '../../hooks/useUbicacionReporte'
import { crearIncidente } from '../../services/nuevoIncidente.service'
import {
  sonCoordenadasValidas,
  type IncidenteCreado,
} from '../../types/nuevoIncidente'
import { FotografiasReporte } from './FotografiasReporte'
import './NuevoIncidentePage.css'

const REGION_REPORTE = 'La Araucanía'

const COMUNAS_DISPONIBLES =
  REGIONES_COMUNAS.find(
    (region) => region.nombre === REGION_REPORTE,
  )?.comunas ?? []

function mensajeErrorEnvio(error: unknown): string {
  const codigo =
    typeof error === 'object' &&
    error !== null &&
    'statusCode' in error
      ? Number(error.statusCode)
      : undefined

  switch (codigo) {
    case 400:
      return 'El servidor rechazó los datos. Revisa los campos obligatorios, la categoría y la ubicación.'
    case 401:
      return 'Tu sesión no es válida. Inicia sesión nuevamente.'
    case 403:
      return 'No tienes permisos para crear este reporte.'
    case 404:
      return 'La categoría seleccionada ya no está disponible. Vuelve a consultar las categorías.'
    default:
      return 'No se pudo confirmar la creación del reporte. Conservamos los datos ingresados. Antes de reintentar, comprueba si el reporte ya se creó para evitar duplicarlo.'
  }
}

export function NuevoIncidentePage() {
  const { user } = useAuth()

  const {
    categorias,
    cargando: cargandoCategorias,
    error: errorCategorias,
    reintentar: reintentarCategorias,
  } = useCategoriasReporte()

  const {
    coordenadas,
    obteniendo: obteniendoUbicacion,
    error: errorUbicacion,
    solicitarUbicacion,
  } = useUbicacionReporte()

  const {
    fotografias,
    erroresSeleccion,
    subiendo,
    agregarFotografias,
    quitarFotografia,
    bloquearSeleccion,
    desbloquearSeleccion,
    subirPendientes,
    limpiarFotografias,
  } = useFotografiasReporte()

  const [titulo, setTitulo] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [categoriaId, setCategoriaId] = useState('')

  const [ubicacionDescriptiva, setUbicacionDescriptiva] = useState({
    direccionReferencia: '',
    comuna: '',
    sector: '',
  })

  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creado, setCreado] = useState<IncidenteCreado | null>(null)

  // null indica que el flujo de fotografías sigue pendiente.
  const [totalFotografiasConfirmadas, setTotalFotografiasConfirmadas] =
    useState<number | null>(null)

  const [totalSinConfirmar, setTotalSinConfirmar] = useState(0)

  const bloqueoEnvio = useRef(false)

  const categoriaValida = categorias.some(
    (categoria) => categoria.id === categoriaId,
  )

  const comunaValida = COMUNAS_DISPONIBLES.includes(
    ubicacionDescriptiva.comuna,
  )

  const formularioCompleto =
    Boolean(titulo.trim()) &&
    Boolean(descripcion.trim()) &&
    Boolean(user?.id) &&
    categoriaValida &&
    comunaValida &&
    Object.values(ubicacionDescriptiva).every(
      (valor) => Boolean(valor.trim()),
    ) &&
    sonCoordenadasValidas(coordenadas)

  const ocupado = enviando || subiendo

  const puedeEnviar =
    formularioCompleto &&
    !ocupado &&
    !obteniendoUbicacion &&
    !cargandoCategorias &&
    !errorCategorias &&
    creado === null

  const hayFallidas = fotografias.some(
    (fotografia) => fotografia.estado === 'fallida',
  )

  const hayPendientes = fotografias.some(
    (fotografia) =>
      fotografia.estado === 'pendiente' ||
      fotografia.estado === 'subiendo',
  )

  async function completarFotografias(incidenteId: string) {
    try {
      const resultado = await subirPendientes(incidenteId)

      if (!resultado) return

      if (
        resultado.fallidas === 0 &&
        resultado.confirmadas === resultado.total
      ) {
        setTotalFotografiasConfirmadas(resultado.confirmadas)
        setTotalSinConfirmar(0)
        limpiarFotografias()
      }
    } catch {
      setError(
        'El incidente ya está creado, pero no se pudo completar la subida de fotografías. Conservamos los archivos pendientes.',
      )
    }
  }

  async function manejarEnvio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (bloqueoEnvio.current || creado) return

    setError(null)

    if (!user?.id || !user.roles.includes('CIUDADANO')) {
      setError(
        'Se requiere una sesión de Ciudadano válida para crear el reporte.',
      )
      return
    }

    if (
      !formularioCompleto ||
      !sonCoordenadasValidas(coordenadas) ||
      obteniendoUbicacion ||
      cargandoCategorias ||
      errorCategorias
    ) {
      setError(
        'Completa todos los campos, selecciona una comuna de La Araucanía y obtén una ubicación válida antes de enviar.',
      )
      return
    }

    bloqueoEnvio.current = true
    bloquearSeleccion()
    setEnviando(true)

    try {
      let incidente: IncidenteCreado

      try {
        incidente = await crearIncidente({
          titulo,
          descripcion,
          categoriaId,
          usuarioReportaId: user.id,
          ubicacion: {
            ...ubicacionDescriptiva,
            region: REGION_REPORTE,
            latitud: coordenadas.latitud,
            longitud: coordenadas.longitud,
          },
        })
      } catch (fallo: unknown) {
        setError(mensajeErrorEnvio(fallo))
        desbloquearSeleccion()
        return
      }

      // Los reintentos posteriores usan este mismo incidente.
      setCreado(incidente)
      await completarFotografias(incidente.id)
    } finally {
      bloqueoEnvio.current = false
      setEnviando(false)
    }
  }

  async function reintentarFotografias() {
    if (
      bloqueoEnvio.current ||
      ocupado ||
      !creado ||
      totalFotografiasConfirmadas !== null
    ) {
      return
    }

    bloqueoEnvio.current = true
    setEnviando(true)
    setError(null)

    try {
      await completarFotografias(creado.id)
    } finally {
      bloqueoEnvio.current = false
      setEnviando(false)
    }
  }

  function continuarSinFotografiasFallidas() {
    if (
      bloqueoEnvio.current ||
      ocupado ||
      !creado ||
      totalFotografiasConfirmadas !== null ||
      !hayFallidas ||
      hayPendientes
    ) {
      return
    }

    const confirmadas = fotografias.filter(
      (fotografia) => fotografia.estado === 'confirmada',
    ).length

    const sinConfirmar = fotografias.filter(
      (fotografia) => fotografia.estado === 'fallida',
    ).length

    setTotalFotografiasConfirmadas(confirmadas)
    setTotalSinConfirmar(sinConfirmar)
    setError(null)

    // Libera archivos locales; no elimina evidencias del backend.
    limpiarFotografias()
  }

  return (
    <section
      className="nuevo-reporte-pagina"
      aria-labelledby="nuevo-reporte-titulo"
    >
      {!ocupado && (
        <Link className="nuevo-reporte-volver" to="/">
          ← Volver al inicio
        </Link>
      )}

      <header className="nuevo-reporte-encabezado">
        <h1 id="nuevo-reporte-titulo">Nuevo reporte</h1>
        <p>
          Reporta un incidente de la Región de La Araucanía.
          Los datos del incidente y su ubicación son obligatorios.
          Las fotografías son opcionales.
        </p>
      </header>

      {creado ? (
        <div
          className="nuevo-reporte-formulario"
          aria-busy={ocupado}
        >
          <div
            className="nuevo-reporte-exito"
            role="status"
            data-incidente-creado-id={creado.id}
          >
            <h2>Reporte creado correctamente</h2>
            <p>{titulo.trim()}</p>
            <p>
              Identificador: <strong>{creado.id}</strong>
            </p>

            {totalFotografiasConfirmadas !== null ? (
              <>
                <p>
                  {totalFotografiasConfirmadas > 0
                    ? `Se confirmó la subida de ${totalFotografiasConfirmadas} ${
                        totalFotografiasConfirmadas === 1
                          ? 'fotografía'
                          : 'fotografías'
                      }.`
                    : totalSinConfirmar > 0
                      ? 'El reporte se conserva. No se confirmó ninguna fotografía.'
                      : 'El reporte se creó sin fotografías.'}
                </p>

                {totalSinConfirmar > 0 && (
                  <p>
                    Finalizaste sin reenviar{' '}
                    {totalSinConfirmar === 1
                      ? '1 fotografía no confirmada'
                      : `${totalSinConfirmar} fotografías no confirmadas`}.
                    Los archivos locales se liberaron. Las evidencias
                    que ya estuvieran guardadas se conservan.
                  </p>
                )}
              </>
            ) : (
              <p>
                {ocupado
                  ? 'El incidente está guardado. Estamos gestionando sus fotografías.'
                  : 'El incidente está guardado. Quedan fotografías sin confirmar.'}
              </p>
            )}
          </div>

          {error && (
            <p className="nuevo-reporte-error" role="alert">
              {error}
            </p>
          )}

          {totalFotografiasConfirmadas === null && (
            <FotografiasReporte
              fotografias={fotografias}
              erroresSeleccion={erroresSeleccion}
              bloqueado={ocupado}
              incidenteCreado
              subiendo={subiendo}
              alSeleccionar={agregarFotografias}
              alQuitar={quitarFotografia}
              alReintentar={() => {
                void reintentarFotografias()
              }}
            />
          )}

          {!ocupado && (
            <>
              {totalFotografiasConfirmadas === null && (
                <>
                  {hayFallidas && !hayPendientes && (
                    <div>
                      <p>
                        Puedes finalizar sin reenviar las fotografías
                        no confirmadas. El incidente y las evidencias
                        guardadas se conservarán. Los archivos locales
                        se liberarán y ya no podrás reintentarlos desde
                        este formulario.
                      </p>

                      <button
                        type="button"
                        onClick={continuarSinFotografiasFallidas}
                      >
                        Continuar sin las fotografías fallidas
                      </button>
                    </div>
                  )}

                  <p>
                    Si sales de esta página o la recargas, los archivos
                    pendientes dejarán de estar disponibles para
                    reintentar. El incidente creado se conservará.
                  </p>
                </>
              )}

              <Link to="/">Volver al inicio</Link>
            </>
          )}
        </div>
      ) : (
        <form
          className="nuevo-reporte-formulario"
          onSubmit={manejarEnvio}
          aria-busy={ocupado}
        >
          {error && (
            <p className="nuevo-reporte-error" role="alert">
              {error}
            </p>
          )}

          <fieldset disabled={ocupado}>
            <legend>Datos del incidente</legend>

            <div className="nuevo-reporte-campo">
              <label htmlFor="reporte-titulo">Título</label>
              <input
                id="reporte-titulo"
                value={titulo}
                onChange={(event) => setTitulo(event.target.value)}
                required
              />
            </div>

            <div className="nuevo-reporte-campo">
              <label htmlFor="reporte-categoria">Categoría</label>

              {cargandoCategorias && (
                <p role="status">Cargando categorías…</p>
              )}

              {errorCategorias && (
                <p className="nuevo-reporte-error" role="alert">
                  {errorCategorias}
                </p>
              )}

              <select
                id="reporte-categoria"
                value={categoriaValida ? categoriaId : ''}
                onChange={(event) => setCategoriaId(event.target.value)}
                disabled={
                  cargandoCategorias ||
                  Boolean(errorCategorias) ||
                  categorias.length === 0
                }
                required
              >
                <option value="">Selecciona una categoría</option>
                {categorias.map((categoria) => (
                  <option key={categoria.id} value={categoria.id}>
                    {categoria.nombre}
                  </option>
                ))}
              </select>

              {!cargandoCategorias &&
                !errorCategorias &&
                categorias.length === 0 && (
                  <p role="status">
                    No hay categorías disponibles para crear reportes.
                  </p>
                )}

              {(errorCategorias ||
                (!cargandoCategorias && categorias.length === 0)) && (
                <button
                  type="button"
                  onClick={reintentarCategorias}
                  disabled={cargandoCategorias}
                >
                  Volver a consultar categorías
                </button>
              )}
            </div>

            <div className="nuevo-reporte-campo">
              <label htmlFor="reporte-descripcion">Descripción</label>
              <textarea
                id="reporte-descripcion"
                rows={5}
                value={descripcion}
                onChange={(event) => setDescripcion(event.target.value)}
                required
              />
            </div>
          </fieldset>

          <fieldset disabled={ocupado}>
            <legend>Ubicación del incidente</legend>

            <p>
              Obtén la ubicación desde el lugar del incidente.
              Las coordenadas se tomarán del navegador.
              Selecciona la comuna y completa la dirección y el sector.
            </p>

            <button
              type="button"
              onClick={solicitarUbicacion}
              disabled={obteniendoUbicacion}
            >
              {obteniendoUbicacion
                ? 'Obteniendo ubicación…'
                : coordenadas
                  ? 'Actualizar mi ubicación'
                  : 'Obtener mi ubicación'}
            </button>

            {errorUbicacion && (
              <p className="nuevo-reporte-error" role="alert">
                {errorUbicacion}
              </p>
            )}

            {coordenadas && (
              <div className="nuevo-reporte-coordenadas" role="status">
                <p>Latitud: {coordenadas.latitud}</p>
                <p>Longitud: {coordenadas.longitud}</p>
              </div>
            )}

            <div className="nuevo-reporte-ubicacion">
              <div className="nuevo-reporte-campo">
                <label htmlFor="reporte-region">Región</label>
                <input
                  id="reporte-region"
                  value={REGION_REPORTE}
                  readOnly
                />
              </div>

              <div className="nuevo-reporte-campo">
                <label htmlFor="reporte-comuna">Comuna</label>
                <select
                  id="reporte-comuna"
                  value={ubicacionDescriptiva.comuna}
                  onChange={(event) => {
                    const comuna = event.target.value

                    setUbicacionDescriptiva((actual) => ({
                      ...actual,
                      comuna,
                    }))
                  }}
                  required
                >
                  <option value="">Selecciona una comuna</option>
                  {COMUNAS_DISPONIBLES.map((comuna) => (
                    <option key={comuna} value={comuna}>
                      {comuna}
                    </option>
                  ))}
                </select>
              </div>

              <div className="nuevo-reporte-campo">
                <label htmlFor="reporte-direccion">
                  Dirección o punto de referencia
                </label>
                <input
                  id="reporte-direccion"
                  value={ubicacionDescriptiva.direccionReferencia}
                  onChange={(event) => {
                    const direccionReferencia = event.target.value

                    setUbicacionDescriptiva((actual) => ({
                      ...actual,
                      direccionReferencia,
                    }))
                  }}
                  required
                />
              </div>

              <div className="nuevo-reporte-campo">
                <label htmlFor="reporte-sector">Sector o barrio</label>
                <input
                  id="reporte-sector"
                  value={ubicacionDescriptiva.sector}
                  onChange={(event) => {
                    const sector = event.target.value

                    setUbicacionDescriptiva((actual) => ({
                      ...actual,
                      sector,
                    }))
                  }}
                  required
                />
              </div>
            </div>
          </fieldset>

          <FotografiasReporte
            fotografias={fotografias}
            erroresSeleccion={erroresSeleccion}
            bloqueado={ocupado}
            incidenteCreado={false}
            subiendo={subiendo}
            alSeleccionar={agregarFotografias}
            alQuitar={quitarFotografia}
            alReintentar={() => {
              void reintentarFotografias()
            }}
          />

          <button type="submit" disabled={!puedeEnviar}>
            {ocupado ? 'Creando reporte…' : 'Crear reporte'}
          </button>
        </form>
      )}
    </section>
  )
}