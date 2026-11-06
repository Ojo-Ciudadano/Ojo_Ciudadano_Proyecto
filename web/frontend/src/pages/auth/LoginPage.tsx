import { useState, useEffect, type FormEvent } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useNotification } from '../../components/notifications/NotificationProvider'
import { getInitialRouteForRoles } from '../../config/navigation'
import type { ApiError } from '../../types'
import './LoginPage.css'

interface LoginLocationState {
  from?: { pathname: string }
  registrationSuccess?: string
  registeredEmail?: string
}

export function LoginPage() {
  const location = useLocation()
  const locationState = location.state as LoginLocationState | null
  const [email, setEmail] = useState(
    locationState?.registeredEmail ?? '',
  )
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(
    locationState?.registrationSuccess ?? null,
  )
  const {
    user,
    login,
    isLoading,
    isAuthenticated,
    sessionExpiredMessage,
    clearSessionExpiredMessage,
  } = useAuth()
  const { showNotification } = useNotification()
  const navigate = useNavigate()

  // Si ya está autenticado, redirige al destino previo o al inicio correspondiente por rol
  useEffect(() => {
    if (isAuthenticated && user) {
      const from = (location.state as { from?: { pathname: string } })?.from?.pathname
      const defaultTarget = getInitialRouteForRoles(user.roles)
      navigate(from || defaultTarget, { replace: true })
    }
  }, [isAuthenticated, user, navigate, location.state])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccessMessage(null)
    clearSessionExpiredMessage()

    try {
      const loggedUser = await login(email, password)
      showNotification('¡Inicio de sesión exitoso!', 'success')
      const from = (location.state as { from?: { pathname: string } })?.from?.pathname
      const target = from || getInitialRouteForRoles(loggedUser.roles)
      navigate(target, { replace: true })
    } catch (err) {
      const apiError = err as ApiError
      const errorMessage = Array.isArray(apiError.message)
        ? apiError.message.join(', ')
        : apiError.message || 'Error al iniciar sesión'

      setError(errorMessage)
      showNotification(errorMessage, 'error')
    }
  }

  const visibleMessage = error ?? sessionExpiredMessage

  return (
    <div className="web-auth">
      <div className="web-auth-card">
        <div className="web-auth-header">
          <img
           src="/img/02.png"
           alt="Ojo Ciudadano"
           className="login-logo"
          />
          <h2>Acceso plataforma</h2>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Correo</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="field">
            <label>Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {successMessage && !visibleMessage && (
            <div className="success-message" role="status">
              {successMessage}
            </div>
          )}

          {visibleMessage && (
            <div className="error-message" role="alert">
              {visibleMessage}
            </div>
          )}

          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? 'Cargando...' : 'Iniciar sesión'}
          </button>
        </form>

        <div className="auth-switch">
          ¿No tienes cuenta? <Link to="/registro">Regístrate aquí</Link>
        </div>
      </div>
    </div>
  )
}