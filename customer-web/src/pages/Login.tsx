import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, setSession, type CurrentUser } from '../api'

interface TokenResponse { accessToken: string; user: CurrentUser }

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const res = await api<TokenResponse>('/v1/auth/login', {
        method: 'POST', body: JSON.stringify({ email, password }) })
      setSession(res.accessToken, res.user)
      navigate('/trips')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    }
  }

  return (
    <main className="container auth-panel">
      <p className="eyebrow">Log in</p>
      <h1>Welcome back.</h1>
      <p className="muted" style={{ margin: '6px 0 18px' }}>Check on your trips or book the next pickup.</p>
      {error && <div className="notice error">{error}</div>}
      <form className="card form-card" onSubmit={submit}>
        <div className="field"><label htmlFor="email">Email</label>
          <input id="email" type="email" value={email} onChange={e => setEmail(e.target.value)} required autoFocus /></div>
        <div className="field"><label htmlFor="password">Password</label>
          <input id="password" type="password" value={password} onChange={e => setPassword(e.target.value)} required /></div>
        <button className="btn primary block" type="submit">Log in</button>
      </form>
      <p className="muted center" style={{ marginTop: 14 }}>First time here? <Link to="/signup">Create an account</Link></p>
    </main>
  )
}
