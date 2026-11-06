import {
  useEffect,
  useState,
  type ChangeEvent,
  type FormEvent,
} from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useNotification } from '../../components/notifications/NotificationProvider'
import { getInitialRouteForRoles } from '../../config/navigation'
import { authService } from '../../services/auth.service'
import type { ApiError } from '../../types'
import {
  hasRegisterErrors,
  validateRegisterForm,
  type RegisterFieldErrors,
  type RegisterFormValues,
} from './registerValidation'
import './LoginPage.css'
import './RegisterPage.css'

const INITIAL_VALUES: RegisterFormValues = {
  nombre: '',
  email: '',
  telefono: '',
  password: '',
  confirmPassword: '',
}

const SUCCESS_MESSAGE =
  'Cuenta creada correctamente. Ya puedes iniciar sesión.'

function resolveErrorMessage(error: ApiError): string {
  if (error.statusCode === 409) {
    return 'Ya existe una cuenta registrada con ese correo electrónico.'
  }

  if (Array.isArray(error.message)) {
    return error.message.filter(Boolean).join(', ')
  }

  return (
    error.message ||
    'No se pudo crear la cuenta. Inténtalo nuevamente.'
  )
}

export function RegisterPage() {
  const [values, setValues] = useState<RegisterFormValues>(INITIAL_VALUES)
  const [fieldErrors, setFieldErrors] = useState<RegisterFieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const { isAuthenticated, user } = useAuth()
  const { showNotification } = useNotification()
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(getInitialRouteForRoles(user.roles), { replace: true })
    }
  }, [isAuthenticated, user, navigate])

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target

    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
    setServerError(null)
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError(null)

    const errors = validateRegisterForm(values)
    setFieldErrors(errors)

    if (hasRegisterErrors(errors)) {
      return
    }

    setIsSubmitting(true)

    try {
      const telefono = values.telefono.trim()

      await authService.register({
        nombre: values.nombre.trim(),
        email: values.email.trim(),
        password: values.password,
        ...(telefono ? { telefono } : {}),
      })

      showNotification(SUCCESS_MESSAGE, 'success')
      navigate('/login', {
        replace: true,
        state: {
          registrationSuccess: SUCCESS_MESSAGE,
          registeredEmail: values.email.trim(),
        },
      })
    } catch (err) {
      const message = resolveErrorMessage(err as ApiError)

      setServerError(message)
      showNotification(message, 'error')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="web-auth web-auth-register">
      <div className="web-auth-card">
        <div className="web-auth-header">
          <img
            src="/img/02.png"
            alt="Ojo Ciudadano"
            className="login-logo"
          />
          <h2>Crear cuenta</h2>
        </div>

        <p className="auth-intro">
          Regístrate para reportar incidentes y seguir su estado.
        </p>

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="register-nombre">Nombre completo</label>
            <input
              id="register-nombre"
              name="nombre"
              type="text"
              value={values.nombre}
              onChange={handleChange}
              autoComplete="name"
              aria-invalid={Boolean(fieldErrors.nombre)}
              aria-describedby={
                fieldErrors.nombre ? 'register-nombre-error' : undefined
              }
              disabled={isSubmitting}
              required
            />
            {fieldErrors.nombre && (
              <p
                id="register-nombre-error"
                className="field-error"
                role="alert"
              >
                {fieldErrors.nombre}
              </p>
            )}
          </div>

          <div className="field">
            <label htmlFor="register-email">Correo</label>
            <input
              id="register-email"
              name="email"
              type="email"
              value={values.email}
              onChange={handleChange}
              autoComplete="email"
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={
                fieldErrors.email ? 'register-email-error' : undefined
              }
              disabled={isSubmitting}
              required
            />
            {fieldErrors.email && (
              <p
                id="register-email-error"
                className="field-error"
                role="alert"
              >
                {fieldErrors.email}
              </p>
            )}
          </div>

          <div className="field">
            <label htmlFor="register-telefono">
              Teléfono (opcional)
            </label>
            <input
              id="register-telefono"
              name="telefono"
              type="tel"
              value={values.telefono}
              onChange={handleChange}
              autoComplete="tel"
              disabled={isSubmitting}
            />
          </div>

          <div className="field">
            <label htmlFor="register-password">Contraseña</label>
            <input
              id="register-password"
              name="password"
              type="password"
              value={values.password}
              onChange={handleChange}
              autoComplete="new-password"
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={
                fieldErrors.password
                  ? 'register-password-error'
                  : undefined
              }
              disabled={isSubmitting}
              required
            />
            {fieldErrors.password && (
              <p
                id="register-password-error"
                className="field-error"
                role="alert"
              >
                {fieldErrors.password}
              </p>
            )}
          </div>

          <div className="field">
            <label htmlFor="register-confirm-password">
              Confirmar contraseña
            </label>
            <input
              id="register-confirm-password"
              name="confirmPassword"
              type="password"
              value={values.confirmPassword}
              onChange={handleChange}
              autoComplete="new-password"
              aria-invalid={Boolean(fieldErrors.confirmPassword)}
              aria-describedby={
                fieldErrors.confirmPassword
                  ? 'register-confirm-password-error'
                  : undefined
              }
              disabled={isSubmitting}
              required
            />
            {fieldErrors.confirmPassword && (
              <p
                id="register-confirm-password-error"
                className="field-error"
                role="alert"
              >
                {fieldErrors.confirmPassword}
              </p>
            )}
          </div>

          {serverError && (
            <div className="error-message" role="alert">
              {serverError}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <div className="auth-switch">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </div>
      </div>
    </div>
  )
}
