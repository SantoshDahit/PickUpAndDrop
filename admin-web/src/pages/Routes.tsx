import { useCallback, useEffect, useState } from 'react'
import { api, type AdminRoute, type Tier } from '../api'

const WON = (n: number) => `₩${n.toLocaleString()}`

function TierEditor({ tiers, onChange }: { tiers: Tier[]; onChange: (tiers: Tier[]) => void }) {
  function set(i: number, patch: Partial<Tier>) {
    onChange(tiers.map((t, idx) => (idx === i ? { ...t, ...patch } : t)))
  }
  return (
    <div className="field">
      <label>Fare ladder (per person by group size)</label>
      {tiers.map((t, i) => (
        <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <input type="number" min={1} max={10} value={t.groupSize} aria-label="Group size"
                 style={{ width: 90 }}
                 onChange={e => set(i, { groupSize: Number(e.target.value) })} />
          <input type="number" min={1} value={t.pricePerPerson} aria-label="Price per person"
                 onChange={e => set(i, { pricePerPerson: Number(e.target.value) })} />
          <button type="button" className="btn danger" onClick={() => onChange(tiers.filter((_, idx) => idx !== i))}>
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="btn"
              onClick={() => onChange([...tiers, {
                groupSize: (tiers[tiers.length - 1]?.groupSize ?? 0) + 1,
                pricePerPerson: tiers[tiers.length - 1]?.pricePerPerson ?? 20000,
              }])}>
        + Add tier
      </button>
    </div>
  )
}

export default function Routes() {
  const [routes, setRoutes] = useState<AdminRoute[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [detailId, setDetailId] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setRoutes(await api<AdminRoute[]>('/v1/admin/routes'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load routes')
    }
  }, [])

  useEffect(() => { load() }, [load])

  function note(ok: string | null, err: string | null) { setFlash(ok); setError(err) }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Routes</h1>
          <p className="muted small">{routes ? `${routes.length} routes · fares are per person` : 'Loading…'}</p>
        </div>
        <button className="btn primary" onClick={() => setCreating(true)}>+ New route</button>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}

      <div className="card table-card">
        <table>
          <thead>
            <tr><th>From</th><th>To</th><th>Fares</th><th>Status</th></tr>
          </thead>
          <tbody>
            {routes?.map(r => (
              <tr key={r.id}>
                <td>
                  <a href="#" onClick={e => { e.preventDefault(); setDetailId(r.id) }}
                     style={{ fontWeight: 550 }}>{r.fromLocation}</a>
                </td>
                <td>{r.toLocation}</td>
                <td>
                  {r.tiers.length === 0
                    ? <span className="stamp warn">no fares — not bookable</span>
                    : <>
                        {WON(Math.min(...r.tiers.map(t => t.pricePerPerson)))}
                        {' – '}
                        {WON(Math.max(...r.tiers.map(t => t.pricePerPerson)))}
                        <span className="muted small"> · {r.tiers.length} tiers</span>
                      </>}
                </td>
                <td><span className={`stamp ${r.active ? 'ok' : 'off'}`}>{r.active ? 'active' : 'inactive'}</span></td>
              </tr>
            ))}
            {routes && routes.length === 0 && (
              <tr><td colSpan={4} className="muted">No routes yet — create the first one.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {creating && (
        <RouteCreate
          onClose={() => setCreating(false)}
          onDone={() => { setCreating(false); note('Route created.', null); load() }}
        />
      )}
      {detailId && (
        <RouteDetail
          routeId={detailId}
          onClose={() => setDetailId(null)}
          onChanged={ok => { note(ok, null); load() }}
          onDeleted={ok => { setDetailId(null); note(ok, null); load() }}
          onError={m => note(null, m)}
        />
      )}
    </>
  )
}

function RouteCreate({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [tiers, setTiers] = useState<Tier[]>([
    { groupSize: 1, pricePerPerson: 25000 },
    { groupSize: 4, pricePerPerson: 16000 },
  ])
  const [localError, setLocalError] = useState<string | null>(null)

  async function create(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      await api('/v1/admin/routes', {
        method: 'POST',
        body: JSON.stringify({ fromLocation: from, toLocation: to, tiers }),
      })
      onDone()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Create failed')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 460 }} onClick={e => e.stopPropagation()}>
        <h2>New route</h2>
        <form onSubmit={create}>
          <div className="field"><label>From</label>
            <input value={from} onChange={e => setFrom(e.target.value)} placeholder="Airport or city" required autoFocus /></div>
          <div className="field"><label>To</label>
            <input value={to} onChange={e => setTo(e.target.value)} placeholder="Destination" required /></div>
          <TierEditor tiers={tiers} onChange={setTiers} />
          {localError && <div className="notice error">{localError}</div>}
          <div className="row">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary">Create route</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function RouteDetail({ routeId, onClose, onChanged, onDeleted, onError }: {
  routeId: string
  onClose: () => void
  onChanged: (message: string) => void
  onDeleted: (message: string) => void
  onError: (message: string) => void
}) {
  const [route, setRoute] = useState<AdminRoute | null>(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [tiers, setTiers] = useState<Tier[]>([])
  const [localError, setLocalError] = useState<string | null>(null)

  const loadDetail = useCallback(async () => {
    try {
      const r = await api<AdminRoute>(`/v1/admin/routes/${routeId}`)
      setRoute(r)
      setFrom(r.fromLocation)
      setTo(r.toLocation)
      setTiers(r.tiers)
    } catch (err) {
      onError(err instanceof Error ? err.message : 'Failed to load route')
      onClose()
    }
  }, [routeId, onError, onClose])

  useEffect(() => { loadDetail() }, [loadDetail])

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      await api(`/v1/admin/routes/${routeId}`, {
        method: 'PATCH',
        body: JSON.stringify({ fromLocation: from, toLocation: to, tiers }),
      })
      onChanged('Route updated.')
      loadDetail()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Update failed')
    }
  }

  async function toggleActive() {
    setLocalError(null)
    try {
      await api(`/v1/admin/routes/${routeId}`, {
        method: 'PATCH',
        body: JSON.stringify({ active: !route?.active }),
      })
      onChanged(route?.active ? 'Route deactivated — hidden from travellers.' : 'Route activated.')
      loadDetail()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Update failed')
    }
  }

  async function remove() {
    if (!confirm('Delete this route permanently? Routes with bookings or rides are refused.')) return
    setLocalError(null)
    try {
      await api(`/v1/admin/routes/${routeId}`, { method: 'DELETE' })
      onDeleted('Route deleted.')
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Delete failed')
    }
  }

  if (!route) return null
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 480, maxHeight: '88vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>{route.fromLocation} → {route.toLocation}
          {' '}<span className={`stamp ${route.active ? 'ok' : 'off'}`}>{route.active ? 'active' : 'inactive'}</span>
        </h2>

        <form onSubmit={save}>
          <div className="field"><label>From</label>
            <input value={from} onChange={e => setFrom(e.target.value)} required /></div>
          <div className="field"><label>To</label>
            <input value={to} onChange={e => setTo(e.target.value)} required /></div>
          <TierEditor tiers={tiers} onChange={setTiers} />
          {localError && <div className="notice error">{localError}</div>}
          <button className="btn primary" type="submit">Save changes</button>
        </form>

        <div className="row" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 18 }}>
          <span>
            <button className="btn" onClick={toggleActive}>
              {route.active ? 'Deactivate' : 'Activate'}
            </button>{' '}
            <button className="btn danger" onClick={remove}>Delete</button>
          </span>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
