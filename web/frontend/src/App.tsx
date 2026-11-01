import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/auth/LoginPage'
import { HomePage } from './pages/home/HomePage'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { IncidentesPage } from './pages/incidentes/IncidentesPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { RoleGuard } from './components/auth/RoleGuard'
import { ROLES, getInitialRouteForRoles } from './config/navigation'

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

        {/* Mapa territorial: disponible para ambos roles */}
        <Route
          path="mapa"
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <HomePage />
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