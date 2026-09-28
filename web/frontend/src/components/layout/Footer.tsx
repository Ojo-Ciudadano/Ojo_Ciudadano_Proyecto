import { Box, Typography, Link } from '@mui/material'

export function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        py: 3,
        px: 2,
        mt: 'auto',
        bgcolor: 'background.paper',
        borderTop: 1,
        borderColor: 'divider',
      }}
    >
      <Typography variant="body2" color="text.secondary" align="center">
        © {new Date().getFullYear()} Ojo Ciudadano — Plataforma de Reportes e Incidentes
      </Typography>
      <Typography variant="caption" color="text.secondary" align="center" display="block">
        <Link href="https://openstreetmap.org" target="_blank" rel="noopener">
          Mapas por OpenStreetMap
        </Link>
      </Typography>
    </Box>
  )
}