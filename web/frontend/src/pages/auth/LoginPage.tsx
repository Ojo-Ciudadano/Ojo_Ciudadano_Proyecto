import { useState, type FormEvent } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useNotification } from '../../components/notifications/NotificationProvider'
import type { ApiError } from '../../types'
import './LoginPage.css'

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { login, isLoading, isAuthenticated } = useAuth()
  const { showNotification } = useNotification()
  const navigate = useNavigate()
  const location = useLocation()

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/'

  if (isAuthenticated) {
    navigate(from, { replace: true })
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)

    try {
      await login(email, password)
      showNotification('¡Inicio de sesión exitoso!', 'success')
      navigate(from, { replace: true })
    } catch (err) {
      const apiError = err as ApiError
      const errorMessage = Array.isArray(apiError.message)
        ? apiError.message.join(', ')
        : apiError.message || 'Error al iniciar sesión'

      setError(errorMessage)
      showNotification(errorMessage, 'error')
    }
  }

  return (
    <div className="web-auth">
      <div className="web-auth-card">
        <div className="web-auth-header">
          <img src="/logo-2.jpeg" alt="Ojo Ciudadano" className="login-logo" />
          <h2>Acceso plataforma</h2>
          </div>

        <div className="role-toggle web-role-toggle">
          <button className="role-btn active">Ciudadano</button>
          <button className="role-btn">Administrador</button>
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

          {error && <div className="error-message">{error}</div>}

          <button type="submit" className="btn btn-primary" disabled={isLoading}>
            {isLoading ? 'Cargando...' : 'Iniciar sesión'}
          </button>
        </form>
      </div>
    </div>
  )
}