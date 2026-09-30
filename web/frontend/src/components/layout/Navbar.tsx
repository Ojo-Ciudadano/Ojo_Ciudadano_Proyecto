import { Link as RouterLink, useNavigate } from 'react-router-dom'
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
        <img src="/logo-3.jpeg" alt="Ojo Ciudadano" className="sidebar-logo" />
        <span className="ws-brand-text">Ojo Ciudadano</span>
        </div>

      <nav className="ws-nav">
        <RouterLink to="/" className="ws-nav-item">
          Inicio
        </RouterLink>
        <RouterLink to="/incidentes" className="ws-nav-item active">
          Incidentes
        </RouterLink>
        <RouterLink to="/mapa" className="ws-nav-item">
          Mapa territorial
        </RouterLink>
      </nav>

      {isAuthenticated && user?.roles?.includes('ADMINISTRADOR') && (
        <div className="ws-admin-section">
          <div className="ws-section-label">Administración</div>
          <RouterLink to="/admin" className="ws-nav-item">
            Usuarios y reglas
          </RouterLink>
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