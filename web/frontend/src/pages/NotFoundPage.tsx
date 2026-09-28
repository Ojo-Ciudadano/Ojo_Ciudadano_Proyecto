import { Link as RouterLink } from 'react-router-dom'
import { Box, Typography, Button, Container } from '@mui/material'

export function NotFoundPage() {
  return (
    <Container maxWidth="sm">
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h1" color="primary" gutterBottom>
          404
        </Typography>
        <Typography variant="h5" gutterBottom>
          Página no encontrada
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          La página que buscas no existe o fue movida.
        </Typography>
        <Button variant="contained" component={RouterLink} to="/">
          Volver al inicio
        </Button>
      </Box>
    </Container>
  )
}