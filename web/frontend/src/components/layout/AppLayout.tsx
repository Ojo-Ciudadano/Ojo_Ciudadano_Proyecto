import { Outlet } from 'react-router-dom'
import { Box } from '@mui/material'
import { Navbar } from './Navbar'
import { Footer } from './Footer'

export function AppLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Outlet />
      </Box>
      <Footer />
    </Box>
  )
}