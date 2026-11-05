
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/auth/LoginPage'
import { HomePage } from './pages/home/HomePage'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { IncidentesPage } from './pages/incidentes/IncidentesPage'
import { IncidenteDetallePage } from './pages/incidentes/IncidenteDetallePage'
import { IncidentMapPage } from './pages/map/IncidentMapPage'
import { UsuariosPage } from './pages/usuarios/UsuariosPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { RoleGuard } from './components/auth/RoleGuard'
import {
  INCIDENT_DETAIL_ALLOWED_ROLES,
  ROLES,
  getInitialRouteForRoles,
} from './config/navigation'
import { NuevoIncidentePage } from './pages/reportes/NuevoIncidentePage'
import { NotificacionesPage } from './pages/notificaciones/NotificacionesPage'

/**
 * Destino inicial según el rol:
 * - ADMIN: /dashboard
 * - CIUDADANO: inicio en /
 */
function RoleHomeRedirect() {
  const { user } = useAuth()
  const initialRoute = getInitialRouteForRoles(user?.roles)

  if (initialRoute !== '/') {
    return <Navigate to={initialRoute} replace />
  }

  return <HomePage />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      {/* Rutas que requieren una sesión autenticada */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        {/* Destino inicial según el rol */}
        <Route
          index
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <RoleHomeRedirect />
            </RoleGuard>
          }
        />
                {/* Nuevo reporte: exclusivo para CIUDADANO */}
        <Route
          path="reportes/nuevo"
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO]} redirectTo="/">
              <NuevoIncidentePage />
            </RoleGuard>
          }
        />

        {/* Dashboard: exclusivo para ADMIN */}
        <Route
          path="dashboard"
          element={
            <RoleGuard allowedRoles={[ROLES.ADMIN]} redirectTo="/">
              <DashboardPage />
            </RoleGuard>
          }
        />

        {/* Gestión de incidentes: exclusiva para ADMIN */}
        <Route
          path="incidentes"
          element={
            <RoleGuard allowedRoles={[ROLES.ADMIN]} redirectTo="/">
              <IncidentesPage />
            </RoleGuard>
          }
        />

        {/* Listado de usuarios: exclusivo para ADMIN - Issue #147 */}
        <Route
          path="usuarios"
          element={
            <RoleGuard allowedRoles={[ROLES.ADMIN]} redirectTo="/">
              <UsuariosPage />
            </RoleGuard>
          }
        />

        {/* Detalle del incidente: consulta para CIUDADANO y ADMIN; gestión exclusiva para ADMIN (#133, #142) */}
        <Route
          path="incidentes/:id"
          element={
            <RoleGuard
              allowedRoles={INCIDENT_DETAIL_ALLOWED_ROLES}
              redirectTo="/"
            >
              <IncidenteDetallePage />
            </RoleGuard>
          }
        />

        {/* Mapa de incidentes: disponible para ambos roles */}
        <Route
          path="mapa"
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <IncidentMapPage />
            </RoleGuard>
          }
        />

        {/* Notificaciones: disponibles para ambos roles - Issue #145 */}
        <Route
          path="notificaciones"
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <NotificacionesPage />
            </RoleGuard>
          }
        />

        {/* Administración: exclusiva para ADMIN */}
        <Route
          path="admin"
          element={
            <RoleGuard allowedRoles={[ROLES.ADMIN]} redirectTo="/">
              <DashboardPage />
            </RoleGuard>
          }
        />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  )
}

export default App
