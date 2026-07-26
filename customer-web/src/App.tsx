import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { getToken, getUser, setSession } from './api'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Book from './pages/Book'
import Trips from './pages/Trips'
import Group from './pages/Group'
import Account from './pages/Account'

export function Header() {
  const user = getUser()
  const navigate = useNavigate()
  return (
    <header className="site-header">
      <div className="container header-row">
        <a className="brand" href="/">Pickup&Drop</a>
        <nav className="nav">
          {user ? (
            <>
              <NavLink to="/book">Book</NavLink>
              <NavLink to="/trips">My trips</NavLink>
              <NavLink to="/account" className="avatar" title={user.name}>
                {user.name.charAt(0).toUpperCase()}
              </NavLink>
              <button className="btn sm" onClick={() => { setSession(null, null); navigate('/') }}>Log out</button>
            </>
          ) : (
            <>
              <NavLink to="/login">Log in</NavLink>
              <NavLink to="/signup" className="btn primary sm" style={{ color: '#fff' }}>Sign up</NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}

function Guarded({ children }: { children: React.ReactNode }) {
  if (!getToken()) return <Navigate to="/login" replace />
  return (
    <>
      <Header />
      <main className="container page">{children}</main>
      <footer className="site-footer"><div className="container">
        Pickup&Drop — airport pickups anywhere in Korea. Fixed fares, cash on arrival.
      </div></footer>
    </>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/book" element={<Guarded><Book /></Guarded>} />
      <Route path="/trips" element={<Guarded><Trips /></Guarded>} />
      <Route path="/groups/:id" element={<Guarded><Group /></Guarded>} />
      <Route path="/account" element={<Guarded><Account /></Guarded>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
