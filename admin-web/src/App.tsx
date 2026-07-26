import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { getToken, setToken } from './api'
import Login from './pages/Login'
import Bookings from './pages/Bookings'
import Rides from './pages/Rides'
import Drivers from './pages/Drivers'

function Shell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  if (!getToken()) return <Navigate to="/login" replace />
  return (
    <div className="shell">
      <nav className="sidebar">
        <div className="brand">Pickup&amp;Drop <span>admin</span></div>
        <NavLink to="/rides">Rides</NavLink>
        <NavLink to="/bookings">Bookings</NavLink>
        <NavLink to="/drivers">Drivers</NavLink>
        <div className="spacer" />
        <button onClick={() => { setToken(null); navigate('/login') }}>Log out</button>
      </nav>
      <main className="main">{children}</main>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/rides" element={<Shell><Rides /></Shell>} />
      <Route path="/bookings" element={<Shell><Bookings /></Shell>} />
      <Route path="/drivers" element={<Shell><Drivers /></Shell>} />
      <Route path="*" element={<Navigate to="/bookings" replace />} />
    </Routes>
  )
}
