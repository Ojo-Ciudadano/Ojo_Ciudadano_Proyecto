import { useNavigate, useLocation } from 'react-router-dom'
import { Box, Typography } from '@mui/material'
import { useAuth } from '../../contexts/AuthContext'
import { LoginForm } from '../../components/auth/LoginForm'

export function LoginPage() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/'

  if (isAuthenticated) {
    navigate(from, { replace: true })
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Typography variant="h4" component="h1" gutterBottom color="primary">
        Ojo Ciudadano
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Plataforma de Reportes e Incidentes
      </Typography>
      <LoginForm onSuccess={() => navigate(from, { replace: true })} />
    </Box>
  )
}