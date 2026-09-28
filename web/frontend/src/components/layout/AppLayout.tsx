import { Outlet } from 'react-router-dom'
import { AppBar, Toolbar, Typography, Button, Box } from '@mui/material'
import { useAuth } from '../../contexts/AuthContext'

export function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Ojo Ciudadano
          </Typography>
          {user && (
            <Typography variant="body1" sx={{ mr: 2 }}>
              {user.nombre} {user.apellido}
            </Typography>
          )}
          <Button color="inherit" onClick={logout}>
            Cerrar sesión
          </Button>
        </Toolbar>
      </AppBar>
      <Box component="main" sx={{ p: 3 }}>
        <Outlet />
      </Box>
    </Box>
  )
}