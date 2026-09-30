import { Outlet } from 'react-router-dom'
import { Navbar } from './Navbar'
import './AppLayout.css'

export function AppLayout() {
  return (
    <div className="web-shell">
      <Navbar />
      <div className="web-main">
        <Outlet />
      </div>
    </div>
  )
}