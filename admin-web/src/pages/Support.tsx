import { useCallback, useEffect, useState } from 'react'
import { api, type SupportInboxRow, type SupportThread } from '../api'

function when(iso: string | null) {
  return iso ? iso.slice(0, 16).replace('T', ' ') : '—'
}

export default function Support() {
  const [inbox, setInbox] = useState<SupportInboxRow[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [openUser, setOpenUser] = useState<SupportInboxRow | null>(null)

  const load = useCallback(async () => {
    try {
      setInbox(await api<SupportInboxRow[]>('/v1/admin/support'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load the inbox')
    }
  }, [])

  useEffect(() => { load() }, [load])

  const waiting = inbox?.filter(r => r.unread > 0).length ?? 0

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Support</h1>
          <p className="muted small">
            Direct messages from travellers — including those riding alone, who have no group chat.
            {waiting > 0 && <> <strong>{waiting} waiting on a reply.</strong></>}
          </p>
        </div>
      </div>
      {error && <div className="notice error">{error}</div>}

      <div className="card table-card">
        <table>
          <thead>
            <tr><th>Traveller</th><th>Messages</th><th>Waiting</th><th>Last activity</th><th></th></tr>
          </thead>
          <tbody>
            {inbox?.map(row => (
              <tr key={row.userId}>
                <td>
                  <a href="#" onClick={e => { e.preventDefault(); setOpenUser(row) }} style={{ fontWeight: 550 }}>
                    {row.customerName}
                  </a>
                  <br /><span className="muted small">{row.customerEmail}</span>
                  {row.customerPhone && <><br /><span className="muted small">{row.customerPhone}</span></>}
                </td>
                <td>{row.messageCount}</td>
                <td>
                  {row.unread > 0
                    ? <span className="stamp warn">{row.unread} unread</span>
                    : <span className="stamp off">answered</span>}
                </td>
                <td className="muted small">{when(row.lastMessageAt)}</td>
                <td style={{ textAlign: 'right' }}>
                  <button className="btn" onClick={() => setOpenUser(row)}>Open</button>
                </td>
              </tr>
            ))}
            {inbox && inbox.length === 0 && (
              <tr><td colSpan={5} className="muted">Nobody has written yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {openUser && (
        <ThreadDialog
          row={openUser}
          onClose={() => { setOpenUser(null); load() }}
          onError={m => setError(m)}
        />
      )}
    </>
  )
}

function ThreadDialog({ row, onClose, onError }: {
  row: SupportInboxRow
  onClose: () => void
  onError: (message: string) => void
}) {
  const [thread, setThread] = useState<SupportThread | null>(null)
  const [body, setBody] = useState('')
  const [sending, setSending] = useState(false)
  const [localError, setLocalError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setThread(await api<SupportThread>(`/v1/admin/support/${row.userId}/messages`))
  }, [row.userId])

  useEffect(() => {
    load().catch(err => {
      onError(err instanceof Error ? err.message : 'Failed to load the thread')
      onClose()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [row.userId])

  async function send(e: React.FormEvent) {
    e.preventDefault()
    if (!body.trim()) return
    setSending(true)
    setLocalError(null)
    try {
      await api(`/v1/admin/support/${row.userId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ body: body.trim() }),
      })
      setBody('')
      await load()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Could not send')
    } finally {
      setSending(false)
    }
  }

  if (!thread) return null

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 620, maxHeight: '88vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>{row.customerName}</h2>
        <p className="muted small" style={{ marginBottom: 14 }}>
          {row.customerEmail}{row.customerPhone ? ` · ${row.customerPhone}` : ''}
        </p>
        {localError && <div className="notice error">{localError}</div>}

        <div style={{
          border: '1px solid var(--line)', borderRadius: 10, padding: 14,
          maxHeight: 340, overflowY: 'auto', marginBottom: 12,
        }}>
          {thread.messages.length === 0 && <p className="muted small">No messages.</p>}
          {thread.messages.map(m => (
            <div key={m.id} style={{ marginBottom: 12, textAlign: m.staff ? 'right' : 'left' }}>
              <p className="muted small" style={{ marginBottom: 2 }}>
                <span style={{ fontWeight: 550, color: m.staff ? 'var(--accent-deep)' : 'var(--ink)' }}>
                  {m.authorName}
                </span>
                {' '}{when(m.createdAt)}
              </p>
              <p style={{
                fontSize: 14, whiteSpace: 'pre-wrap', display: 'inline-block', maxWidth: '85%',
                background: m.staff ? 'var(--accent-tint)' : 'var(--paper-deep)',
                borderRadius: 8, padding: '8px 11px', textAlign: 'left',
              }}>
                {m.body}
              </p>
            </div>
          ))}
        </div>

        <form onSubmit={send}>
          <div className="field">
            <label htmlFor="reply">Reply as the Pickup&amp;Drop team</label>
            <textarea id="reply" rows={3} maxLength={1000} value={body}
                      onChange={e => setBody(e.target.value)}
                      placeholder="The traveller sees this as an official message." />
          </div>
          <div className="row">
            <button type="button" className="btn" onClick={onClose}>Close</button>
            <button type="submit" className="btn primary" disabled={sending || !body.trim()}>
              {sending ? 'Sending…' : 'Send reply'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
