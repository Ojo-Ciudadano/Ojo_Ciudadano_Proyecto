import { Routes, Route } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/auth/LoginPage'
import { HomePage } from './pages/home/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProtectedRoute } from './components/auth/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<HomePage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AuthProvider>
  )
}

export default App