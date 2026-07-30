import { useCallback, useEffect, useState } from 'react'
import { api, type AdminServiceRequest, type ServiceRequestStatus } from '../api'

const STAMP: Record<ServiceRequestStatus, string> = {
  REQUESTED: 'warn',
  CONFIRMED: 'ok',
  DELIVERED: 'ok',
  CANCELLED: 'off',
}

/** Mirrors ServiceRequestStatus.canMoveTo on the API — terminal states are final. */
const NEXT: Record<ServiceRequestStatus, ServiceRequestStatus[]> = {
  REQUESTED: ['CONFIRMED', 'DELIVERED', 'CANCELLED'],
  CONFIRMED: ['DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
}

// The API filters by a single status, so there is no server-side "open" filter;
// the queue already sorts open first, and this default shows everything.
const FILTERS: Array<{ label: string; value: string }> = [
  { label: 'All', value: '' },
  { label: 'Requested', value: 'REQUESTED' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Delivered', value: 'DELIVERED' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

export default function Services() {
  const [requests, setRequests] = useState<AdminServiceRequest[] | null>(null)
  const [filter, setFilter] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [noteFor, setNoteFor] = useState<AdminServiceRequest | null>(null)

  const load = useCallback(async () => {
    try {
      const qs = filter ? `?status=${filter}` : ''
      setRequests(await api<AdminServiceRequest[]>(`/v1/admin/service-requests${qs}`))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load service requests')
    }
  }, [filter])

  useEffect(() => { load() }, [load])

  function note(ok: string | null, err: string | null) { setFlash(ok); setError(err) }

  async function move(request: AdminServiceRequest, status: ServiceRequestStatus) {
    if (status === 'CANCELLED' && !confirm('Cancel this request?')) return
    try {
      await api(`/v1/admin/service-requests/${request.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      note(`Marked ${status.toLowerCase()}.`, null)
      load()
    } catch (err) {
      note(null, err instanceof Error ? err.message : 'Update failed')
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Services</h1>
          <p className="muted small">
            SIM card requests from travellers. Open ones sort to the top; confirm the plan and price
            with them, then mark it delivered at handover.
          </p>
        </div>
        <div className="row" style={{ gap: 6 }}>
          {FILTERS.map(f => (
            <button
              key={f.value}
              className={`btn ${filter === f.value ? 'primary' : ''}`}
              onClick={() => setFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}

      <div className="card table-card">
        <table>
          <thead>
            <tr>
              <th>Traveller</th><th>Plan</th><th>Arrival</th><th>Hand over</th>
              <th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            {requests?.map(r => (
              <tr key={r.id}>
                <td>
                  <span style={{ fontWeight: 550 }}>{r.customerName}</span>
                  <br /><span className="muted small">{r.customerEmail}</span>
                  {r.customerPhone && <><br /><span className="muted small">{r.customerPhone}</span></>}
                  {r.contact && <><br /><span className="muted small">on arrival: {r.contact}</span></>}
                </td>
                <td>
                  {r.detail ?? <span className="muted">—</span>}
                  {r.notes && <><br /><span className="muted small">“{r.notes}”</span></>}
                  {r.adminNote && (
                    <><br /><span className="small" style={{ color: 'var(--accent-deep)' }}>
                      note: {r.adminNote}
                    </span></>
                  )}
                </td>
                <td className="small">
                  {r.arrivalDate ?? <span className="muted">not set</span>}
                  {r.airport && <><br /><span className="muted">{r.airport}</span></>}
                </td>
                <td className="muted small">{r.deliverTo ?? '—'}</td>
                <td><span className={`stamp ${STAMP[r.status]}`}>{r.status.toLowerCase()}</span></td>
                <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                  {NEXT[r.status].map(next => (
                    <button
                      key={next}
                      className={`btn ${next === 'CANCELLED' ? 'danger' : next === 'DELIVERED' ? 'primary' : ''}`}
                      style={{ marginLeft: 6 }}
                      onClick={() => move(r, next)}
                    >
                      {next === 'CONFIRMED' ? 'Confirm' : next === 'DELIVERED' ? 'Delivered' : 'Cancel'}
                    </button>
                  ))}
                  <button className="btn" style={{ marginLeft: 6 }} onClick={() => setNoteFor(r)}>Note</button>
                </td>
              </tr>
            ))}
            {requests && requests.length === 0 && (
              <tr><td colSpan={6} className="muted">Nothing here.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {noteFor && (
        <NoteDialog
          request={noteFor}
          onClose={() => setNoteFor(null)}
          onDone={() => { setNoteFor(null); note('Note saved.', null); load() }}
          onError={m => note(null, m)}
        />
      )}
    </>
  )
}

function NoteDialog({ request, onClose, onDone, onError }: {
  request: AdminServiceRequest
  onClose: () => void
  onDone: () => void
  onError: (message: string) => void
}) {
  const [text, setText] = useState(request.adminNote ?? '')

  async function save(e: React.FormEvent) {
    e.preventDefault()
    try {
      await api(`/v1/admin/service-requests/${request.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ adminNote: text }),
      })
      onDone()
    } catch (err) {
      onError(err instanceof Error ? err.message : 'Could not save the note')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 460 }} onClick={e => e.stopPropagation()}>
        <h2>Internal note</h2>
        <p className="muted small" style={{ marginBottom: 12 }}>
          For {request.customerName}&apos;s SIM request. Only the team sees this — travellers never do.
        </p>
        <form onSubmit={save}>
          <div className="field">
            <textarea rows={4} maxLength={1000} value={text} onChange={e => setText(e.target.value)}
                      placeholder="Carrier, price quoted, stock, anything the next person needs." />
          </div>
          <div className="row">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn primary">Save note</button>
          </div>
        </form>
      </div>
    </div>
  )
}
