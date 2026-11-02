import { Link as RouterLink, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { getVisibleNavigation } from '../../config/navigation'
import './Navbar.css'

export function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const userRoles = Array.isArray(user?.roles) ? user.roles : []
  const mainNavItems = getVisibleNavigation(userRoles, 'main')
  const adminNavItems = getVisibleNavigation(userRoles, 'admin')
  const roleLabel = isAdmin ? 'Administrador' : userRoles.includes('CIUDADANO') ? 'Ciudadano' : 'Sin rol válido'

  const isActivePath = (path: string) => {
    if (path === '/') {
      return location.pathname === '/'
    }
    return location.pathname === path || location.pathname.startsWith(`${path}/`)
  }

  return (
    <aside className="web-sidebar">
      <div className="ws-brand">
        <img src="/logo-3.jpeg" alt="Ojo Ciudadano" className="sidebar-logo" />
        <span className="ws-brand-text">Ojo Ciudadano</span>
      </div>

      <nav className="ws-nav">
        {mainNavItems.map((item) => (
          <RouterLink
            key={item.path}
            to={item.path}
            className={`ws-nav-item ${isActivePath(item.path) ? 'active' : ''}`}
          >
            {item.label}
          </RouterLink>
        ))}
      </nav>

      {/* Sección exclusiva de Administración gobernada por NAVIGATION_RULES (#130) */}
      {isAuthenticated && adminNavItems.length > 0 && (
        <div className="ws-admin-section">
          <div className="ws-section-label">Administración</div>
          {adminNavItems.map((item) => (
            <RouterLink
              key={item.path}
              to={item.path}
              className={`ws-nav-item ${isActivePath(item.path) ? 'active' : ''}`}
            >
              {item.label}
            </RouterLink>
          ))}
        </div>
      )}

      <div className="ws-user">
        <div className="avatar">
          {user?.nombre ? user.nombre.charAt(0).toUpperCase() : 'U'}
        </div>
        <div>
          <strong>{user?.nombre || 'Usuario'}</strong>
          <span>{roleLabel}</span>
        </div>
      </div>

      <button className="web-logout" onClick={handleLogout}>
        Cerrar sesión
      </button>
    </aside>
  )
}
