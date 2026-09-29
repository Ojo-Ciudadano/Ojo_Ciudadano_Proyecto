import { useState, type FormEvent } from 'react'
import {
  Box,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
  Link,
  Paper,
  Collapse,
} from '@mui/material'
import { useAuth } from '../../contexts/AuthContext'
import { useNotification } from '../notifications/NotificationProvider'
import type { ApiError } from '../../types'

interface LoginFormProps {
  onSuccess?: () => void
}

export function LoginForm({ onSuccess }: LoginFormProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
  const { login, isLoading } = useAuth()
  const { showNotification } = useNotification()

  const validate = (): boolean => {
    const errors: { email?: string; password?: string } = {}

    if (!email.trim()) {
      errors.email = 'El correo electrónico es obligatorio'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Ingresa un correo electrónico válido'
    }

    if (!password) {
      errors.password = 'La contraseña es obligatoria'
    } else if (password.length < 6) {
      errors.password = 'La contraseña debe tener al menos 6 caracteres'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!validate()) return

    try {
      await login(email, password)
      showNotification('¡Inicio de sesión exitoso!', 'success')
      onSuccess?.()
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
    <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: '100%', mx: 'auto' }}>
      <Typography variant="h5" component="h1" gutterBottom align="center">
        Iniciar sesión
      </Typography>

      <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
        Ingresa tus credenciales para acceder
      </Typography>

      <Collapse in={!!error}>
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => setError(null)}
        >
          {error}
        </Alert>
      </Collapse>

      <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          label="Correo electrónico"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={!!fieldErrors.email}
          helperText={fieldErrors.email}
          required
          fullWidth
          autoComplete="email"
          autoFocus
        />

        <TextField
          label="Contraseña"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={!!fieldErrors.password}
          helperText={fieldErrors.password}
          required
          fullWidth
          autoComplete="current-password"
        />

        <Button
          type="submit"
          variant="contained"
          size="large"
          disabled={isLoading}
          sx={{ mt: 1 }}
        >
          {isLoading ? <CircularProgress size={24} /> : 'Iniciar sesión'}
        </Button>

        <Box sx={{ textAlign: 'center', mt: 1 }}>
          <Link href="#" variant="body2" onClick={(e) => e.preventDefault()}>
            ¿Olvidaste tu contraseña?
          </Link>
        </Box>
      </Box>
    </Paper>
  )
}