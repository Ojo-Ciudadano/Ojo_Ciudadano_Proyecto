import { Typography, Box, Grid, Card, CardContent } from '@mui/material'
import { useAuth } from '../../contexts/AuthContext'

export function DashboardPage() {
  const { user } = useAuth()

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Bienvenido, {user?.nombre} {user?.apellido}
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Incidentes
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Gestión y seguimiento de incidentes urbanos
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Mapa
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Visualización georreferenciada de incidentes
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Reportes
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Estadísticas y reportes de actividad
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}