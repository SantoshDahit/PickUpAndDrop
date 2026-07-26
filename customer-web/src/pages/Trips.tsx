import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api, type Booking } from '../api'

export default function Trips() {
  const [trips, setTrips] = useState<Booking[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const location = useLocation()
  const [flash, setFlash] = useState<string | null>((location.state as { flash?: string } | null)?.flash ?? null)

  const load = useCallback(async () => {
    try {
      setTrips(await api<Booking[]>('/v1/bookings/me'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load trips')
    }
  }, [])

  useEffect(() => { load() }, [load])

  async function cancel(b: Booking) {
    if (!confirm('Cancel this booking?')) return
    try {
      await api(`/v1/bookings/${b.id}`, { method: 'DELETE' })
      setFlash('Booking cancelled.')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cancel failed')
    }
  }

  return (
    <>
      <p className="eyebrow">My trips</p>
      <h1>Your trips.</h1>
      <div style={{ marginTop: 18 }}>
        {flash && <div className="notice ok">{flash}</div>}
        {error && <div className="notice error">{error}</div>}
        {trips && trips.length === 0 && (
          <div className="card form-card">
            <p className="muted">No trips yet. Book your first pickup and we'll take it from there.</p>
            <p style={{ marginTop: 14 }}><Link className="btn primary" to="/book">Book a pickup</Link></p>
          </div>
        )}
        {trips?.map(b => (
          <div className="card trip-card" key={b.id}>
            <div className="trip-head">
              <div>
                <strong>{b.route ? `${b.route.fromLocation} → ${b.route.toLocation}` : 'Route'}</strong>
                <span className="muted"> · {b.travelDate}</span>
                {b.flightNo && <span className="muted"> · {b.flightNo}</span>}
              </div>
              <span className={`stamp ${b.status === 'ACTIVE' ? 'ok' : 'off'}`}>{b.status.toLowerCase()}</span>
            </div>
            <p className="muted small">
              {b.partySize} {b.partySize === 1 ? 'traveller' : 'travellers'} · {b.groupId ? 'group ride' : 'riding individually'}
            </p>
            {b.driver && (
              <div className="trip-driver">
                <span>🚐 <strong>{b.driver.name}</strong>
                  {b.driver.vehicle && <span className="muted"> — {b.driver.vehicle}{b.driver.plateNo ? ` · ${b.driver.plateNo}` : ''}</span>}
                </span>
                {b.driver.phone && <a className="btn sm" href={`tel:${b.driver.phone}`}>{b.driver.phone}</a>}
              </div>
            )}
            {b.status === 'ACTIVE' && (
              <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                {b.groupId && <Link className="btn sm" to={`/groups/${b.groupId}`}>Open group &amp; chat</Link>}
                <button className="btn sm" onClick={() => cancel(b)}>Cancel booking</button>
              </div>
            )}
          </div>
        ))}
        {trips && trips.length > 0 && (
          <p style={{ marginTop: 16 }}><Link className="btn primary" to="/book">Book another pickup</Link></p>
        )}
      </div>
    </>
  )
}
