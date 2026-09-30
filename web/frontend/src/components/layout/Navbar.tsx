import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import './Navbar.css'

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <aside className="web-sidebar">
      <div className="ws-brand">
        <img src="/Logo_ojo_ciudadano1.png" alt="Ojo Ciudadano" className="sidebar-logo" />
      </div>

      <nav className="ws-nav">
        <NavLink to="/" className={({ isActive }) => `ws-nav-item${isActive ? ' active' : ''}`}>
          Inicio
        </NavLink>
        <NavLink to="/mapa" className={({ isActive }) => `ws-nav-item${isActive ? ' active' : ''}`}>
          Mapa territorial
        </NavLink>
      </nav>

      {isAuthenticated && user?.roles?.includes('ADMINISTRADOR') && (
        <div className="ws-admin-section">
          <div className="ws-section-label">Administración</div>
          <NavLink to="/admin" className={({ isActive }) => `ws-nav-item${isActive ? ' active' : ''}`}>
            Usuarios y reglas
          </NavLink>
        </div>
      )}

      <div className="ws-user">
        <div className="avatar">
          {user?.nombre?.charAt(0)}{user?.apellido?.charAt(0)}
        </div>
        <div>
          <strong>{user?.nombre} {user?.apellido}</strong>
          <span>{user?.roles?.includes('ADMINISTRADOR') ? 'Administrador' : 'Ciudadano'}</span>
        </div>
      </div>

      <button className="web-logout" onClick={handleLogout}>
        Cerrar sesión
      </button>
    </aside>
  )
}
