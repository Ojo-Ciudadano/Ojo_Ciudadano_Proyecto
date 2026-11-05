
import { useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useUsuarios } from '../../hooks/useUsuarios'
import type { RolUsuario, UsuarioListado } from '../../types/usuario'
import './UsuariosPage.css'

function formatearFecha(fecha: string): string {
  const valor = new Date(fecha)

  if (Number.isNaN(valor.getTime())) {
    return 'No disponible'
  }

  return valor.toLocaleDateString('es-CL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function nombreRol(rol: RolUsuario): string {
  return rol === 'ADMIN' ? 'Administrador' : 'Ciudadano'
}

interface ConfirmacionRol {
  usuario: UsuarioListado
  nuevoRol: RolUsuario
}

export function UsuariosPage() {
  const { user, isAdmin } = useAuth()

  const {
    usuarios,
    meta,
    pagina,
    cargando,
    error,
    actualizandoRolId,
    mensajeRol,
    errorRol,
    cambiarRol,
    siguientePagina,
    paginaAnterior,
    reintentar,
  } = useUsuarios()

  const [rolesSeleccionados, setRolesSeleccionados] = useState<
    Record<string, RolUsuario>
  >({})

  const [confirmacion, setConfirmacion] =
    useState<ConfirmacionRol | null>(null)

  function obtenerRolActual(usuario: UsuarioListado): RolUsuario | null {
    if (usuario.roles.length !== 1) return null

    const rol = usuario.roles[0]

    return rol === 'ADMIN' || rol === 'CIUDADANO'
      ? rol
      : null
  }

  function obtenerRolSeleccionado(usuario: UsuarioListado): RolUsuario | '' {
    return rolesSeleccionados[usuario.id] ??
      obtenerRolActual(usuario) ??
      ''
  }

  function esAutodegradacion(
    usuario: UsuarioListado,
    nuevoRol: RolUsuario,
  ): boolean {
    return (
      usuario.id === user?.id &&
      usuario.roles.includes('ADMIN') &&
      nuevoRol === 'CIUDADANO'
    )
  }

  function solicitarCambio(usuario: UsuarioListado) {
    if (!isAdmin || actualizandoRolId || cargando) return

    const nuevoRol = obtenerRolSeleccionado(usuario)
    const rolActual = obtenerRolActual(usuario)

    if (!nuevoRol || nuevoRol === rolActual) return
    if (esAutodegradacion(usuario, nuevoRol)) return

    setConfirmacion({ usuario, nuevoRol })
  }

  function cancelarCambio() {
    if (actualizandoRolId) return

    if (confirmacion) {
      setRolesSeleccionados((actuales) => {
        const copia = { ...actuales }
        delete copia[confirmacion.usuario.id]
        return copia
      })
    }

    setConfirmacion(null)
  }

  async function confirmarCambio() {
    if (!confirmacion || !isAdmin || actualizandoRolId || cargando) {
      return
    }

    const { usuario, nuevoRol } = confirmacion

    if (
      obtenerRolActual(usuario) === nuevoRol ||
      esAutodegradacion(usuario, nuevoRol)
    ) {
      return
    }

    // El hook actualiza la tabla solo cuando el backend confirma.
    await cambiarRol(usuario.id, nuevoRol)

    setRolesSeleccionados((actuales) => {
      const copia = { ...actuales }
      delete copia[usuario.id]
      return copia
    })

    setConfirmacion(null)
  }

  return (
    <main className="usuarios-page">
      <header className="usuarios-header">
        <div>
          <h1>Administración de usuarios</h1>
          <p>
            Consulta los usuarios registrados en Ojo Ciudadano
            y administra sus roles.
          </p>
        </div>
      </header>

      <section
        className="usuarios-card"
        aria-label="Listado de usuarios"
      >
        <div className="usuarios-card-header">
          <h2>Usuarios registrados</h2>

          {meta && !error && (
            <span className="usuarios-total">
              Total: {meta.total} usuarios
            </span>
          )}
        </div>

        {mensajeRol && (
          <div className="usuarios-mensaje-exito" role="status">
            {mensajeRol}
          </div>
        )}

        {errorRol && (
          <div className="usuarios-mensaje-error" role="alert">
            {errorRol}
          </div>
        )}

        {cargando && (
          <div className="usuarios-estado" role="status">
            <p>Cargando usuarios...</p>
          </div>
        )}

        {!cargando && error && (
          <div className="usuarios-error" role="alert">
            <p>{error}</p>
            <button
              type="button"
              className="usuarios-btn usuarios-btn-primary"
              onClick={reintentar}
            >
              Reintentar
            </button>
          </div>
        )}

        {!cargando && !error && usuarios.length === 0 && (
          <div className="usuarios-estado">
            <p>No se encontraron usuarios registrados.</p>
          </div>
        )}

        {!cargando && !error && usuarios.length > 0 && (
          <>
            <div className="usuarios-tabla-contenedor">
              <table className="usuarios-tabla">
                <thead>
                  <tr>
                    <th scope="col">Nombre</th>
                    <th scope="col">Correo electrónico</th>
                    <th scope="col">Teléfono</th>
                    <th scope="col">Rol vigente</th>
                    <th scope="col">Fecha de registro</th>
                    {isAdmin && <th scope="col">Cambiar rol</th>}
                  </tr>
                </thead>

                <tbody>
                  {usuarios.map((usuario) => {
                    const rolActual = obtenerRolActual(usuario)
                    const rolSeleccionado = obtenerRolSeleccionado(usuario)

                    const esPropioAdmin =
                      usuario.id === user?.id &&
                      usuario.roles.includes('ADMIN')

                    const bloqueado = Boolean(actualizandoRolId)
                    const sinCambios = rolSeleccionado === rolActual

                    return (
                      <tr
                        key={usuario.id}
                        data-usuario-id={usuario.id}
                      >
                        <td>{usuario.nombre}</td>
                        <td>{usuario.email}</td>
                        <td>{usuario.telefono || 'No registrado'}</td>

                        <td>
                          <div className="usuarios-roles">
                            {usuario.roles.length > 0 ? (
                              usuario.roles.map((rol) => (
                                <span
                                  key={rol}
                                  className={`usuarios-rol ${
                                    rol === 'ADMIN'
                                      ? 'usuarios-rol-admin'
                                      : 'usuarios-rol-ciudadano'
                                  }`}
                                >
                                  {rol === 'ADMIN'
                                    ? 'Administrador'
                                    : rol === 'CIUDADANO'
                                      ? 'Ciudadano'
                                      : rol}
                                </span>
                              ))
                            ) : (
                              <span>Sin rol asignado</span>
                            )}
                          </div>
                        </td>

                        <td>{formatearFecha(usuario.fechaRegistro)}</td>

                        {isAdmin && (
                          <td>
                            <div className="usuarios-cambiar-rol">
                              <select
                                className="usuarios-select"
                                aria-label={`Nuevo rol de ${usuario.nombre}`}
                                value={rolSeleccionado}
                                disabled={bloqueado}
                                onChange={(evento) => {
                                  const nuevoRol = evento.target.value

                                  if (
                                    nuevoRol !== 'ADMIN' &&
                                    nuevoRol !== 'CIUDADANO'
                                  ) return

                                  setRolesSeleccionados((actuales) => ({
                                    ...actuales,
                                    [usuario.id]: nuevoRol,
                                  }))
                                }}
                              >
                                {!rolSeleccionado && (
                                  <option value="">Seleccionar rol</option>
                                )}
                                <option value="CIUDADANO" disabled={esPropioAdmin}>
                                  Ciudadano
                                </option>
                                <option value="ADMIN">
                                  Administrador
                                </option>
                              </select>

                              <button
                                type="button"
                                className="usuarios-btn usuarios-btn-primary"
                                disabled={
                                  bloqueado ||
                                  sinCambios ||
                                  !rolSeleccionado ||
                                  (esPropioAdmin &&
                                    rolSeleccionado === 'CIUDADANO')
                                }
                                onClick={() => solicitarCambio(usuario)}
                              >
                                {actualizandoRolId === usuario.id
                                  ? 'Guardando...'
                                  : 'Cambiar'}
                              </button>
                            </div>

                            {esPropioAdmin && (
                              <small className="usuarios-ayuda">
                                No puedes quitarte el rol de administrador.
                              </small>
                            )}
                          </td>
                        )}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {meta && (
              <div className="usuarios-paginacion">
                <span>
                  Página {meta.page} de {meta.totalPages}
                </span>

                <div className="usuarios-paginacion-botones">
                  <button
                    type="button"
                    className="usuarios-btn"
                    onClick={paginaAnterior}
                    disabled={
                      cargando ||
                      Boolean(actualizandoRolId) ||
                      pagina <= 1
                    }
                  >
                    Anterior
                  </button>

                  <button
                    type="button"
                    className="usuarios-btn usuarios-btn-primary"
                    onClick={siguientePagina}
                    disabled={
                      cargando ||
                      Boolean(actualizandoRolId) ||
                      pagina >= meta.totalPages
                    }
                  >
                    Siguiente
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </section>

      {confirmacion && (
        <div className="usuarios-modal-fondo">
          <div
            className="usuarios-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="usuarios-confirmacion-titulo"
          >
            <h2 id="usuarios-confirmacion-titulo">
              Confirmar cambio de rol
            </h2>

            <p>
              ¿Deseas cambiar el rol de{' '}
              <strong>{confirmacion.usuario.nombre}</strong> a{' '}
              <strong>{nombreRol(confirmacion.nuevoRol)}</strong>?
            </p>

            <p className="usuarios-modal-aviso">
              El cambio se guardará en el sistema después de confirmar.
            </p>

            <div className="usuarios-modal-acciones">
              <button
                type="button"
                className="usuarios-btn"
                disabled={Boolean(actualizandoRolId)}
                onClick={cancelarCambio}
              >
                Cancelar
              </button>

              <button
                type="button"
                className="usuarios-btn usuarios-btn-primary"
                disabled={Boolean(actualizandoRolId)}
                onClick={() => void confirmarCambio()}
              >
                {actualizandoRolId
                  ? 'Guardando...'
                  : 'Confirmar cambio'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
