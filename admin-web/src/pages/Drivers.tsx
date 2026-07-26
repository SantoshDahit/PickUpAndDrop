import { useCallback, useEffect, useState } from 'react'
import { api, type Driver, type Page } from '../api'

const EMPTY = { name: '', phone: '', licenseNo: '', vehicle: '', plateNo: '', seats: 4, ownsVehicle: true }

export default function Drivers() {
  const [page, setPage] = useState<Page<Driver> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [registering, setRegistering] = useState(false)

  const load = useCallback(async () => {
    try {
      setPage(await api<Page<Driver>>('/v1/admin/drivers/search?size=100'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load drivers')
    }
  }, [])

  useEffect(() => { load() }, [load])

  function note(ok: string | null, err: string | null) {
    setFlash(ok); setError(err)
  }

  async function toggleStatus(d: Driver) {
    try {
      await api(`/v1/admin/drivers/${d.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status: d.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }),
      })
      note('Status updated.', null)
      load()
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Update failed')
    }
  }

  async function remove(d: Driver) {
    if (!confirm(`Remove ${d.name} from the roster?`)) return
    try {
      await api(`/v1/admin/drivers/${d.id}`, { method: 'DELETE' })
      note(`${d.name} removed.`, null)
      load()
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Delete failed')
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Drivers</h1>
          <p className="muted small">{page ? `${page.page.totalElements} on the roster` : 'Loading…'}</p>
        </div>
        <button className="btn primary" onClick={() => setRegistering(true)}>+ Register driver</button>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}
      <div className="card table-card">
          <table>
            <thead>
              <tr><th>Name</th><th>Phone</th><th>Vehicle</th><th>Seats</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {page?.content.map(d => (
                <tr key={d.id}>
                  <td>
                    <a href="#" onClick={e => { e.preventDefault(); setDetailId(d.id) }}
                       style={{ fontWeight: 550 }}>{d.name}</a>
                    <div className="muted small">{d.licenseNo ?? ''}</div>
                  </td>
                  <td>{d.phone ?? '—'}</td>
                  <td>{d.vehicle ?? '—'}<div className="muted small">{d.plateNo ?? ''}</div></td>
                  <td>{d.seats}</td>
                  <td><span className={`stamp ${d.status === 'ACTIVE' ? 'ok' : 'off'}`}>{d.status.toLowerCase()}</span></td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    <button className="btn" onClick={() => toggleStatus(d)}>
                      {d.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                    </button>{' '}
                    <button className="btn danger" onClick={() => remove(d)}>Remove</button>
                  </td>
                </tr>
              ))}
              {page && page.content.length === 0 && (
                <tr><td colSpan={6} className="muted">No drivers yet — register the first one.</td></tr>
              )}
            </tbody>
          </table>
      </div>
      {registering && (
        <RegisterDialog
          onClose={() => setRegistering(false)}
          onDone={(name) => { setRegistering(false); note(`Driver ${name} registered.`, null); load() }}
        />
      )}
      {detailId && (
        <DriverDetail
          driverId={detailId}
          onClose={() => setDetailId(null)}
          onChanged={(ok) => { note(ok, null); load() }}
          onError={(m) => note(null, m)}
        />
      )}
    </>
  )
}

function RegisterDialog({ onClose, onDone }: { onClose: () => void; onDone: (name: string) => void }) {
  const [form, setForm] = useState({ ...EMPTY })
  const [localError, setLocalError] = useState<string | null>(null)

  async function create(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      await api('/v1/admin/drivers', { method: 'POST', body: JSON.stringify(form) })
      onDone(form.name)
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Create failed')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 440, maxHeight: '88vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>Register driver</h2>
        <form onSubmit={create}>
          <div className="field"><label>Name</label>
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required autoFocus /></div>
          <div className="field"><label>Phone</label>
            <input value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} /></div>
          <div className="field"><label>License no.</label>
            <input value={form.licenseNo} onChange={e => setForm({ ...form, licenseNo: e.target.value })} /></div>
          <div className="field"><label>Vehicle model</label>
            <input value={form.vehicle} onChange={e => setForm({ ...form, vehicle: e.target.value })} placeholder="Hyundai Staria" /></div>
          <div className="field"><label>Plate no.</label>
            <input value={form.plateNo} onChange={e => setForm({ ...form, plateNo: e.target.value })} placeholder="12가3456" /></div>
          <div className="field"><label>Passenger seats</label>
            <select value={form.seats} onChange={e => setForm({ ...form, seats: Number(e.target.value) })}>
              {[1,2,3,4,5,6,7,8,9,10].map(n => <option key={n} value={n}>{n}</option>)}
            </select></div>
          {localError && <div className="notice error">{localError}</div>}
          <div className="row">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary">Register</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function DriverDetail({ driverId, onClose, onChanged, onError }: {
  driverId: string
  onClose: () => void
  onChanged: (message: string) => void
  onError: (message: string) => void
}) {
  const [driver, setDriver] = useState<Driver | null>(null)
  const [edit, setEdit] = useState({ name: '', phone: '', licenseNo: '', vehicle: '', plateNo: '', seats: 4 })
  const [account, setAccount] = useState({ email: '', password: '' })
  const [localError, setLocalError] = useState<string | null>(null)

  const loadDetail = useCallback(async () => {
    try {
      const d = await api<Driver>(`/v1/admin/drivers/${driverId}`)
      setDriver(d)
      setEdit({
        name: d.name, phone: d.phone ?? '', licenseNo: d.licenseNo ?? '',
        vehicle: d.vehicle ?? '', plateNo: d.plateNo ?? '', seats: d.seats,
      })
    } catch (err) {
      onError(err instanceof Error ? err.message : 'Failed to load driver')
      onClose()
    }
  }, [driverId, onError, onClose])

  useEffect(() => { loadDetail() }, [loadDetail])

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      await api(`/v1/admin/drivers/${driverId}`, { method: 'PATCH', body: JSON.stringify(edit) })
      onChanged('Driver updated.')
      loadDetail()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Update failed')
    }
  }

  async function createLogin(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      await api(`/v1/admin/drivers/${driverId}/account`, { method: 'POST', body: JSON.stringify(account) })
      onChanged(`Login created for ${driver?.name}.`)
      setAccount({ email: '', password: '' })
      loadDetail()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Login creation failed')
    }
  }

  if (!driver) return null
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 480, maxHeight: '88vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>{driver.name}
          {' '}<span className={`stamp ${driver.status === 'ACTIVE' ? 'ok' : 'off'}`}>{driver.status.toLowerCase()}</span>
          {' '}{driver.hasAccount && <span className="stamp ok">login linked</span>}
        </h2>

        <form onSubmit={save}>
          <div className="field"><label>Name</label>
            <input value={edit.name} onChange={e => setEdit({ ...edit, name: e.target.value })} /></div>
          <div className="field"><label>Phone</label>
            <input value={edit.phone} onChange={e => setEdit({ ...edit, phone: e.target.value })} /></div>
          <div className="field"><label>License no.</label>
            <input value={edit.licenseNo} onChange={e => setEdit({ ...edit, licenseNo: e.target.value })} /></div>
          <div className="field"><label>Vehicle model</label>
            <input value={edit.vehicle} onChange={e => setEdit({ ...edit, vehicle: e.target.value })} /></div>
          <div className="field"><label>Plate no.</label>
            <input value={edit.plateNo} onChange={e => setEdit({ ...edit, plateNo: e.target.value })} /></div>
          <div className="field"><label>Passenger seats</label>
            <select value={edit.seats} onChange={e => setEdit({ ...edit, seats: Number(e.target.value) })}>
              {[1,2,3,4,5,6,7,8,9,10].map(n => <option key={n} value={n}>{n}</option>)}
            </select></div>
          {localError && <div className="notice error">{localError}</div>}
          <button className="btn primary" type="submit">Save changes</button>
        </form>

        {!driver.hasAccount && (
          <form onSubmit={createLogin} style={{ marginTop: 22, borderTop: '1px solid var(--line)', paddingTop: 16 }}>
            <h2>Create login</h2>
            <p className="muted small" style={{ marginBottom: 10 }}>
              Lets this driver sign in to see their assigned rides.
            </p>
            <div className="field"><label>Email</label>
              <input type="email" value={account.email} required
                     onChange={e => setAccount({ ...account, email: e.target.value })} /></div>
            <div className="field"><label>Initial password</label>
              <input type="text" value={account.password} required minLength={6}
                     onChange={e => setAccount({ ...account, password: e.target.value })} /></div>
            <button className="btn primary" type="submit">Create login</button>
          </form>
        )}

        <div className="row" style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 18 }}>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
