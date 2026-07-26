import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, type BookingAdminDetail, type BookingSummary, type Driver, type Page } from '../api'

export default function Bookings() {
  const [page, setPage] = useState<Page<BookingSummary> | null>(null)
  const [pageNo, setPageNo] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [assignTarget, setAssignTarget] = useState<BookingSummary | null>(null)
  const [detailId, setDetailId] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setPage(await api<Page<BookingSummary>>(`/v1/admin/bookings/search?page=${pageNo}&size=20`))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load bookings')
    }
  }, [pageNo])

  useEffect(() => { load() }, [load])

  async function unassign(b: BookingSummary) {
    setError(null); setFlash(null)
    try {
      const path = b.groupId ? `/v1/admin/groups/${b.groupId}/driver` : `/v1/admin/bookings/${b.id}/driver`
      await api(path, { method: 'DELETE' })
      setFlash('Driver unassigned.')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unassign failed')
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Bookings</h1>
          <p className="muted small">{page ? `${page.page.totalElements} total` : 'Loading…'}</p>
        </div>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}
      <div className="card table-card">
        <table>
          <thead>
            <tr>
              <th>Route</th><th>Landing day</th><th>Party</th><th>Ride</th><th>Status</th><th>Driver</th><th></th>
            </tr>
          </thead>
          <tbody>
            {page?.content.map(b => (
              <tr key={b.id}>
                <td>
                  <a href="#" onClick={e => { e.preventDefault(); setDetailId(b.id) }}
                     style={{ fontWeight: 550 }}>
                    {b.route ? `${b.route.fromLocation} → ${b.route.toLocation}` : '—'}
                  </a>
                </td>
                <td>{b.travelDate}{b.flightNo ? <span className="muted"> · {b.flightNo}</span> : null}</td>
                <td>{b.partySize}</td>
                <td>{b.groupId
                  ? <span className="stamp ok">group …{b.groupId.slice(-6)}</span>
                  : <span className="stamp">individual</span>}</td>
                <td><span className={`stamp ${b.status === 'ACTIVE' ? 'ok' : 'off'}`}>{b.status.toLowerCase()}</span></td>
                <td>{b.driver
                  ? <>{b.driver.name}<div className="muted small">{b.driver.vehicle} · {b.driver.plateNo}</div></>
                  : <span className="muted">—</span>}</td>
                <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                  {b.status === 'ACTIVE' && (
                    <>
                      <button className="btn" onClick={() => setAssignTarget(b)}>
                        {b.driver ? 'Replace driver' : 'Assign driver'}
                      </button>{' '}
                      {b.driver && <button className="btn danger" onClick={() => unassign(b)}>Unassign</button>}
                    </>
                  )}
                </td>
              </tr>
            ))}
            {page && page.content.length === 0 && (
              <tr><td colSpan={7} className="muted">No bookings yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
      {page && page.page.totalPages > 1 && (
        <div className="pager">
          <button className="btn" disabled={pageNo === 0} onClick={() => setPageNo(p => p - 1)}>‹ Prev</button>
          <span className="muted small">page {page.page.number + 1} / {page.page.totalPages}</span>
          <button className="btn" disabled={pageNo >= page.page.totalPages - 1} onClick={() => setPageNo(p => p + 1)}>Next ›</button>
        </div>
      )}
      {assignTarget && (
        <AssignDialog
          booking={assignTarget}
          onClose={() => setAssignTarget(null)}
          onDone={() => { setAssignTarget(null); setFlash('Driver assigned.'); load() }}
          onError={m => setError(m)}
        />
      )}
      {detailId && (
        <BookingDetail
          bookingId={detailId}
          onClose={() => setDetailId(null)}
          onChanged={m => { setFlash(m); setError(null); load() }}
          onError={m => { setError(m); setFlash(null) }}
        />
      )}
    </>
  )
}

function BookingDetail({ bookingId, onClose, onChanged, onError }: {
  bookingId: string
  onClose: () => void
  onChanged: (message: string) => void
  onError: (message: string) => void
}) {
  const [b, setB] = useState<BookingAdminDetail | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  const loadDetail = useCallback(async () => {
    try {
      setB(await api<BookingAdminDetail>(`/v1/admin/bookings/${bookingId}`))
    } catch (err) {
      onError(err instanceof Error ? err.message : 'Failed to load booking')
      onClose()
    }
  }, [bookingId, onError, onClose])

  useEffect(() => { loadDetail() }, [loadDetail])

  async function cancel() {
    if (!confirm('Cancel this booking on the customer\'s behalf?')) return
    setLocalError(null)
    try {
      await api(`/v1/admin/bookings/${bookingId}`, { method: 'DELETE' })
      onChanged('Booking cancelled.')
      loadDetail()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Cancel failed')
    }
  }

  if (!b) return null
  const row = (label: string, value: React.ReactNode) => (
    <p style={{ marginBottom: 6 }}>
      <span className="muted small" style={{ display: 'inline-block', width: 110 }}>{label}</span>
      {value ?? <span className="muted">—</span>}
    </p>
  )
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 480, maxHeight: '88vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>
          {b.route ? `${b.route.fromLocation} → ${b.route.toLocation}` : 'Booking'}
          {' '}<span className={`stamp ${b.status === 'ACTIVE' ? 'ok' : 'off'}`}>{b.status.toLowerCase()}</span>
        </h2>

        {row('Customer', <>{b.customer.name} · {b.customer.email}{b.customer.phone ? ` · ${b.customer.phone}` : ''}</>)}
        {row('Reach at', b.contact)}
        {row('Landing day', <>{b.travelDate}{b.flightNo ? ` · flight ${b.flightNo}` : ''}</>)}
        {row('Party', `${b.partySize} ${b.partySize === 1 ? 'person' : 'people'}`)}
        {row('Ride', b.groupId ? `shared group …${b.groupId.slice(-6)}` : 'individual')}
        {row('Driver', b.driver ? `${b.driver.name}${b.driver.vehicle ? ` · ${b.driver.vehicle}` : ''}${b.driver.plateNo ? ` · ${b.driver.plateNo}` : ''}` : null)}
        {row('Intro', b.intro)}
        {row('Notes', b.notes)}
        {row('Booked', new Date(b.createdAt).toLocaleString())}

        {localError && <div className="notice error" style={{ marginTop: 10 }}>{localError}</div>}

        <div className="row" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 18 }}>
          <span>
            {b.status === 'ACTIVE' && (
              <button className="btn danger" onClick={cancel}>Cancel booking</button>
            )}
          </span>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}

function AssignDialog({ booking, onClose, onDone, onError }: {
  booking: BookingSummary
  onClose: () => void
  onDone: () => void
  onError: (message: string) => void
}) {
  const [drivers, setDrivers] = useState<Driver[]>([])
  const [driverId, setDriverId] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    api<Page<Driver>>('/v1/admin/drivers/search?statusList=ACTIVE&size=100')
      .then(p => {
        if (cancelled) return
        setDrivers(p.content)
        // Never clobber a selection the user already made (late responses race).
        setDriverId(prev => prev || (p.content[0]?.id ?? ''))
      })
      .catch(err => { if (!cancelled) onError(err instanceof Error ? err.message : 'Failed to load drivers') })
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function assign() {
    setLocalError(null)
    try {
      const path = booking.groupId
        ? `/v1/admin/groups/${booking.groupId}/driver`
        : `/v1/admin/bookings/${booking.id}/driver`
      await api(path, { method: 'PUT', body: JSON.stringify({ driverId }) })
      onDone()
    } catch (err) {
      setLocalError(err instanceof ApiError ? err.message : 'Assignment failed')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h2>{booking.groupId ? 'Assign driver to group' : 'Assign driver to individual ride'}</h2>
        <p className="muted small" style={{ marginBottom: 12 }}>
          {booking.route ? `${booking.route.fromLocation} → ${booking.route.toLocation}` : ''} · {booking.travelDate}
          {booking.groupId ? ' · rides as a group' : ` · party of ${booking.partySize}`}
        </p>
        {localError && <div className="notice error">{localError}</div>}
        <div className="field">
          <label htmlFor="driver">Driver (active only)</label>
          <select id="driver" value={driverId} onChange={e => setDriverId(e.target.value)}>
            {drivers.map(d => (
              <option key={d.id} value={d.id}>
                {d.name} — {d.vehicle ?? 'vehicle n/a'} ({d.seats} seats)
              </option>
            ))}
          </select>
        </div>
        <div className="row">
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn primary" onClick={assign} disabled={!driverId}>Assign</button>
        </div>
      </div>
    </div>
  )
}
