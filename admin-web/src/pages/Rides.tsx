import { useCallback, useEffect, useState } from 'react'
import { api, type Driver, type OpenRide, type Page, type Route } from '../api'

export default function Rides() {
  const [rides, setRides] = useState<OpenRide[] | null>(null)
  const [routes, setRoutes] = useState<Route[]>([])
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [routeId, setRouteId] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setRides(await api<OpenRide[]>('/v1/groups/open'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rides')
    }
  }, [])

  useEffect(() => {
    load()
    api<Route[]>('/v1/routes').then(r => { setRoutes(r); if (r[0]) setRouteId(prev => prev || r[0].id) })
    api<Page<Driver>>('/v1/admin/drivers/search?statusList=ACTIVE&size=100').then(p => setDrivers(p.content))
  }, [load])

  function note(ok: string | null, err: string | null) { setFlash(ok); setError(err) }

  async function publish(e: React.FormEvent) {
    e.preventDefault()
    try {
      await api('/v1/admin/groups', { method: 'POST', body: JSON.stringify({ routeId, targetDate }) })
      note('Ride published — travellers can now find and join it.', null)
      setTargetDate('')
      load()
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Publish failed')
    }
  }

  async function close(ride: OpenRide) {
    if (!confirm('Close this ride? It disappears from the public list.')) return
    try {
      await api(`/v1/admin/groups/${ride.id}/close`, { method: 'PATCH' })
      note('Ride closed.', null)
      load()
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Close failed')
    }
  }

  async function assignDriver(ride: OpenRide, driverId: string) {
    if (!driverId) return
    try {
      await api(`/v1/admin/groups/${ride.id}/driver`, { method: 'PUT', body: JSON.stringify({ driverId }) })
      note('Driver assigned to the ride.', null)
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Assign failed')
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Published rides</h1>
          <p className="muted small">Rides you publish are browsable by travellers, who join with their own booking.</p>
        </div>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}

      <div className="grid-2">
        <form className="card form-card" onSubmit={publish}>
          <h2>Publish a ride</h2>
          <div className="field">
            <label htmlFor="route">Route</label>
            <select id="route" value={routeId} onChange={e => setRouteId(e.target.value)}>
              {routes.map(r => <option key={r.id} value={r.id}>{r.fromLocation} → {r.toLocation}</option>)}
            </select>
          </div>
          <div className="field">
            <label htmlFor="target">Advertised landing day</label>
            <input id="target" type="date" value={targetDate} onChange={e => setTargetDate(e.target.value)} required />
          </div>
          <button className="btn primary block" type="submit">Publish</button>
          <p className="muted small" style={{ marginTop: 10 }}>
            Joins must land within 7 days of the advertised day; capacity is 6 seats.
          </p>
        </form>

        <div className="card table-card">
          <table>
            <thead>
              <tr><th>Route</th><th>Target day</th><th>Members</th><th>Seats left</th><th>Dates</th><th></th></tr>
            </thead>
            <tbody>
              {rides?.map(ride => (
                <tr key={ride.id}>
                  <td>{ride.route.fromLocation} → {ride.route.toLocation}</td>
                  <td>{ride.targetDate}</td>
                  <td>{ride.memberCount}</td>
                  <td><span className={`stamp ${ride.seatsLeft > 0 ? 'ok' : 'warn'}`}>{ride.seatsLeft}</span></td>
                  <td className="muted small">
                    {ride.earliestDate ? (ride.earliestDate === ride.latestDate ? ride.earliestDate
                      : `${ride.earliestDate} – ${ride.latestDate}`) : '—'}
                  </td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    <select style={{ width: 170, display: 'inline-block', height: 34, marginRight: 6 }}
                            defaultValue="" onChange={e => assignDriver(ride, e.target.value)}>
                      <option value="" disabled>Assign driver…</option>
                      {drivers.map(d => <option key={d.id} value={d.id}>{d.name} ({d.seats})</option>)}
                    </select>
                    <button className="btn danger" onClick={() => close(ride)}
                            disabled={ride.memberCount > 0}
                            title={ride.memberCount > 0 ? 'Has members — handle them first' : ''}>Close</button>
                  </td>
                </tr>
              ))}
              {rides && rides.length === 0 && (
                <tr><td colSpan={6} className="muted">No open rides — publish the first one.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
