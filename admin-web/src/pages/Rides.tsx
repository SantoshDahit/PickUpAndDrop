import { useCallback, useEffect, useState } from 'react'
import { api, type Driver, type GroupView, type OpenRide, type Page, type Route } from '../api'

export default function Rides() {
  const [rides, setRides] = useState<OpenRide[] | null>(null)
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [publishing, setPublishing] = useState(false)
  const [detailId, setDetailId] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setRides(await api<OpenRide[]>('/v1/groups/open'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rides')
    }
  }, [])

  useEffect(() => {
    load()
    api<Page<Driver>>('/v1/admin/drivers/search?statusList=ACTIVE&size=100').then(p => setDrivers(p.content))
  }, [load])

  function note(ok: string | null, err: string | null) { setFlash(ok); setError(err) }

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
        <button className="btn primary" onClick={() => setPublishing(true)}>+ Publish ride</button>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}

      <div className="card table-card">
        <table>
          <thead>
            <tr><th>Route</th><th>Target day</th><th>Members</th><th>Seats left</th><th>Dates</th><th></th></tr>
          </thead>
          <tbody>
            {rides?.map(ride => (
              <tr key={ride.id}>
                <td>
                  <a href="#" onClick={e => { e.preventDefault(); setDetailId(ride.id) }}
                     style={{ fontWeight: 550 }}>
                    {ride.route.fromLocation} → {ride.route.toLocation}
                  </a>
                </td>
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

      {publishing && (
        <PublishDialog
          onClose={() => setPublishing(false)}
          onDone={() => { setPublishing(false); note('Ride published — travellers can now find and join it.', null); load() }}
        />
      )}
      {detailId && (
        <RideDetail
          groupId={detailId}
          onClose={() => setDetailId(null)}
          onError={m => note(null, m)}
        />
      )}
    </>
  )
}

function PublishDialog({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [routes, setRoutes] = useState<Route[]>([])
  const [routeId, setRouteId] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    api<Route[]>('/v1/routes').then(r => { setRoutes(r); if (r[0]) setRouteId(prev => prev || r[0].id) })
  }, [])

  async function publish(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      await api('/v1/admin/groups', { method: 'POST', body: JSON.stringify({ routeId, targetDate }) })
      onDone()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Publish failed')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 420 }} onClick={e => e.stopPropagation()}>
        <h2>Publish a ride</h2>
        <form onSubmit={publish}>
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
          {localError && <div className="notice error">{localError}</div>}
          <p className="muted small" style={{ marginBottom: 14 }}>
            Joins must land within 7 days of the advertised day; capacity is 6 seats.
          </p>
          <div className="row">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary">Publish</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function RideDetail({ groupId, onClose, onError }: {
  groupId: string
  onClose: () => void
  onError: (message: string) => void
}) {
  const [group, setGroup] = useState<GroupView | null>(null)

  useEffect(() => {
    api<GroupView>(`/v1/groups/${groupId}`)
      .then(setGroup)
      .catch(err => { onError(err instanceof Error ? err.message : 'Failed to load ride'); onClose() })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupId])

  if (!group) return null
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 460, maxHeight: '88vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>
          {group.route.fromLocation} → {group.route.toLocation}
          {' '}<span className="stamp ok">{group.status.toLowerCase()}</span>
        </h2>
        <p className="muted small" style={{ marginBottom: 12 }}>
          {group.agreedDate ? `Agreed landing day: ${group.agreedDate}` : 'Landing day not agreed yet'}
          {group.driver ? ` · driver: ${group.driver.name}` : ' · no driver assigned'}
        </p>

        <h2 style={{ fontSize: 15 }}>Members</h2>
        {group.members.length === 0
          ? <p className="muted">No members yet — the ride is open for joins.</p>
          : (
            <table>
              <thead>
                <tr><th>Name</th><th>Party</th><th>Landing day</th><th>Intro</th></tr>
              </thead>
              <tbody>
                {group.members.map((m, i) => (
                  <tr key={i}>
                    <td>{m.firstName}</td>
                    <td>{m.partySize}</td>
                    <td>{m.travelDate}</td>
                    <td className="muted small">{m.intro ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

        <div className="row" style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 18 }}>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
