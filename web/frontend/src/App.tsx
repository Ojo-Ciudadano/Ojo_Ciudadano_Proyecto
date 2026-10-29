import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/auth/LoginPage'
import { HomePage } from './pages/home/HomePage'
import { DashboardPage } from './pages/dashboard/DashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'
import { RoleGuard } from './components/auth/RoleGuard'
import { ROLES, getInitialRouteForRoles } from './config/navigation'

/**
 * Redirige o muestra la raíz '/' según las reglas centralizadas del rol:
 * - ADMIN -> /dashboard (gestión administrativa)
 * - CIUDADANO -> HomePage en '/'
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

      {/* Rutas generales protegidas que requieren sesión autenticada */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        {/* Destino inicial inteligente según rol */}
        <Route
          index
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <RoleHomeRedirect />
            </RoleGuard>
          }
        />

        {/* Dashboard: Exclusivo para rol ADMIN */}
        <Route
          path="dashboard"
          element={
            <RoleGuard allowedRoles={[ROLES.ADMIN]} redirectTo="/">
              <DashboardPage />
            </RoleGuard>
          }
        />

        {/* Incidentes y mapa territorial accesibles para roles autenticados conocidos */}
        <Route
          path="incidentes"
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <HomePage />
            </RoleGuard>
          }
        />
        <Route
          path="mapa"
          element={
            <RoleGuard allowedRoles={[ROLES.CIUDADANO, ROLES.ADMIN]}>
              <HomePage />
            </RoleGuard>
          }
        />

        {/* Administración: Exclusiva para rol ADMIN */}
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
