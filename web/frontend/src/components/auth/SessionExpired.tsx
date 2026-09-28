import { Box, Button, Card, CardContent, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

export function SessionExpired() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const handleGoToLogin = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Card sx={{ maxWidth: 400, width: '100%' }}>
        <CardContent sx={{ textAlign: 'center' }}>
          <Typography variant="h5" gutterBottom>
            Sesión expirada
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Tu sesión ha expirado. Por favor, inicia sesión de nuevo.
          </Typography>
          <Button variant="contained" onClick={handleGoToLogin}>
            Ir a iniciar sesión
          </Button>
        </CardContent>
      </Card>
    </Box>
  )
}