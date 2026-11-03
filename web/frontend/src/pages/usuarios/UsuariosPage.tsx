
import { useUsuarios } from '../../hooks/useUsuarios'
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

export function UsuariosPage() {
  const {
    usuarios,
    meta,
    pagina,
    cargando,
    error,
    siguientePagina,
    paginaAnterior,
    reintentar,
  } = useUsuarios()

  return (
    <main className="usuarios-page">
      <header className="usuarios-header">
        <div>
          <h1>Administración de usuarios</h1>
          <p>
            Consulta los usuarios registrados en Ojo Ciudadano
            y sus roles actuales.
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
                  </tr>
                </thead>

                <tbody>
                  {usuarios.map((usuario) => (
                    <tr
                    key={usuario.id}
                    data-usuario-id={usuario.id}
                    >
                      <td>{usuario.nombre}</td>

                      <td>{usuario.email}</td>

                      <td>
                        {usuario.telefono || 'No registrado'}
                      </td>

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

                      <td>
                        {formatearFecha(usuario.fechaRegistro)}
                      </td>
                    </tr>
                  ))}
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
                    disabled={cargando || pagina <= 1}
                  >
                    Anterior
                  </button>

                  <button
                    type="button"
                    className="usuarios-btn usuarios-btn-primary"
                    onClick={siguientePagina}
                    disabled={
                      cargando ||
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
    </main>
  )
}