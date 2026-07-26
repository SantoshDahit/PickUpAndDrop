import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, setSession, type CurrentUser } from '../api'

interface TokenResponse { accessToken: string; user: CurrentUser }

export default function Signup() {
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const res = await api<TokenResponse>('/v1/auth/signup', {
        method: 'POST', body: JSON.stringify(form) })
      setSession(res.accessToken, res.user)
      navigate('/book')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Signup failed')
    }
  }

  return (
    <main className="container auth-panel">
      <p className="eyebrow">Sign up</p>
      <h1>Create your account.</h1>
      <p className="muted" style={{ margin: '6px 0 18px' }}>Book pickups for you and your group in minutes.</p>
      {error && <div className="notice error">{error}</div>}
      <form className="card form-card" onSubmit={submit}>
        <div className="field"><label htmlFor="name">Name</label>
          <input id="name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required autoFocus /></div>
        <div className="field"><label htmlFor="email">Email</label>
          <input id="email" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} required /></div>
        <div className="field"><label htmlFor="phone">Phone <span className="muted">(optional)</span></label>
          <input id="phone" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></div>
        <div className="field"><label htmlFor="password">Password</label>
          <input id="password" type="password" minLength={6} placeholder="6+ characters"
                 value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required /></div>
        <button className="btn primary block" type="submit">Create account</button>
      </form>
      <p className="muted center" style={{ marginTop: 14 }}>Already registered? <Link to="/login">Log in</Link></p>
    </main>
  )
}
