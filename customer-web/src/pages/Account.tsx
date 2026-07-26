import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, getUser, setSession, type CurrentUser } from '../api'

export default function Account() {
  const [profile, setProfile] = useState({ name: '', phone: '' })
  const [email, setEmail] = useState('')
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' })
  const [delPw, setDelPw] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    api<CurrentUser>('/v1/users/me').then(u => {
      setProfile({ name: u.name, phone: u.phone ?? '' })
      setEmail(u.email)
    }).catch(err => setError(err instanceof Error ? err.message : 'Failed to load profile'))
  }, [])

  function note(ok: string | null, err: string | null) { setFlash(ok); setError(err) }

  async function saveProfile(e: React.FormEvent) {
    e.preventDefault()
    try {
      const updated = await api<CurrentUser>('/v1/users/me', { method: 'PATCH', body: JSON.stringify(profile) })
      setSession(localStorage.getItem('pud_token'), updated)
      note('Profile saved.', null)
    } catch (err) { note(null, err instanceof Error ? err.message : 'Save failed') }
  }

  async function changePassword(e: React.FormEvent) {
    e.preventDefault()
    try {
      await api('/v1/users/me/password', { method: 'PATCH', body: JSON.stringify(pw) })
      setPw({ currentPassword: '', newPassword: '' })
      note('Password changed.', null)
    } catch (err) { note(null, err instanceof Error ? err.message : 'Change failed') }
  }

  async function deleteAccount(e: React.FormEvent) {
    e.preventDefault()
    if (!confirm('Delete your account? Active bookings are cancelled. This cannot be undone.')) return
    try {
      await api('/v1/users/me', { method: 'DELETE', body: JSON.stringify({ password: delPw }) })
      setSession(null, null)
      navigate('/')
    } catch (err) { note(null, err instanceof Error ? err.message : 'Deletion failed') }
  }

  return (
    <>
      <p className="eyebrow">Account</p>
      <h1>Your account.</h1>
      <p className="muted" style={{ margin: '6px 0 18px' }}>{email || getUser()?.email}</p>
      {flash && <div className="notice ok">{flash}</div>}
      {error && <div className="notice error">{error}</div>}
      <div className="grid-2">
        <div>
          <form className="card form-card" onSubmit={saveProfile}>
            <h2>Profile</h2>
            <div className="field"><label htmlFor="name">Name</label>
              <input id="name" value={profile.name} onChange={e => setProfile({ ...profile, name: e.target.value })} required /></div>
            <div className="field"><label htmlFor="phone">Phone</label>
              <input id="phone" value={profile.phone} onChange={e => setProfile({ ...profile, phone: e.target.value })} /></div>
            <button className="btn primary" type="submit">Save profile</button>
          </form>
          <form className="card form-card danger-zone" onSubmit={deleteAccount}>
            <h2>Danger zone</h2>
            <p className="muted small" style={{ marginBottom: 12 }}>
              Deleting your account cancels active bookings and logs you out everywhere.
            </p>
            <div className="field"><label htmlFor="delpw">Confirm with your password</label>
              <input id="delpw" type="password" value={delPw} onChange={e => setDelPw(e.target.value)} required /></div>
            <button className="btn danger" type="submit">Delete my account</button>
          </form>
        </div>
        <form className="card form-card" onSubmit={changePassword}>
          <h2>Password</h2>
          <div className="field"><label htmlFor="cur">Current password</label>
            <input id="cur" type="password" value={pw.currentPassword}
                   onChange={e => setPw({ ...pw, currentPassword: e.target.value })} required /></div>
          <div className="field"><label htmlFor="new">New password</label>
            <input id="new" type="password" minLength={6} placeholder="6+ characters" value={pw.newPassword}
                   onChange={e => setPw({ ...pw, newPassword: e.target.value })} required /></div>
          <button className="btn primary" type="submit">Change password</button>
        </form>
      </div>
    </>
  )
}
