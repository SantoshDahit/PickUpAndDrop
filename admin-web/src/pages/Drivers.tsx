import { useCallback, useEffect, useState } from 'react'
import { api, type Driver, type Page } from '../api'

const EMPTY = { name: '', phone: '', licenseNo: '', vehicle: '', plateNo: '', seats: 4, ownsVehicle: true }

export default function Drivers() {
  const [page, setPage] = useState<Page<Driver> | null>(null)
  const [form, setForm] = useState({ ...EMPTY })
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)

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

  async function create(e: React.FormEvent) {
    e.preventDefault()
    try {
      await api('/v1/admin/drivers', { method: 'POST', body: JSON.stringify(form) })
      setForm({ ...EMPTY })
      note(`Driver ${form.name} registered.`, null)
      load()
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Create failed')
    }
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
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}
      <div className="grid-2">
        <form className="card form-card" onSubmit={create}>
          <h2>Register driver</h2>
          <div className="field"><label>Name</label>
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required /></div>
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
          <button className="btn primary block" type="submit">Register</button>
        </form>

        <div className="card table-card">
          <table>
            <thead>
              <tr><th>Name</th><th>Phone</th><th>Vehicle</th><th>Seats</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {page?.content.map(d => (
                <tr key={d.id}>
                  <td>{d.name}<div className="muted small">{d.licenseNo ?? ''}</div></td>
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
      </div>
    </>
  )
}
