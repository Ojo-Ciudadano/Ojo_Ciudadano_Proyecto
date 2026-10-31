import {
  type ReactNode,
  useState,
  useCallback,
  createContext,
  useContext,
} from 'react'
import { Snackbar, Alert, type AlertColor } from '@mui/material'

interface NotificationContextType {
  showNotification: (message: string, severity?: AlertColor) => void
}

const NotificationContext = createContext<
  NotificationContextType | undefined
>(undefined)

export function useNotification() {
  const context = useContext(NotificationContext)

  if (!context) {
    throw new Error(
      'useNotification debe ser usado dentro de NotificationProvider',
    )
  }

  return context
}

interface NotificationProviderProps {
  children: ReactNode
}

export function NotificationProvider({
  children,
}: NotificationProviderProps) {
  const [notification, setNotification] = useState<{
    message: string
    severity: AlertColor
  } | null>(null)

  const showNotification = useCallback(
    (message: string, severity: AlertColor = 'info') => {
      setNotification({ message, severity })
    },
    [],
  )

  const handleClose = () => {
    setNotification(null)
  }

  return (
    <NotificationContext.Provider value={{ showNotification }}>
      {children}

      <Snackbar
        open={notification !== null}
        autoHideDuration={4000}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <Alert
          onClose={handleClose}
          severity={notification?.severity ?? 'info'}
          sx={{ width: '100%' }}
        >
          {notification?.message ?? ''}
        </Alert>
      </Snackbar>
    </NotificationContext.Provider>
  )
}