import { useCallback, useEffect, useState } from 'react'
import {
  api,
  type AddCandidate,
  type AdminGroupDetail,
  type AdminMessage,
  type ChatSummary,
} from '../api'

function when(iso: string | null) {
  if (!iso) return '—'
  return iso.slice(0, 16).replace('T', ' ')
}

export default function Chats() {
  const [chats, setChats] = useState<ChatSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [openId, setOpenId] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setChats(await api<ChatSummary[]>('/v1/admin/groups/chats'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load chats')
    }
  }, [])

  useEffect(() => { load() }, [load])

  function note(ok: string | null, err: string | null) { setFlash(ok); setError(err) }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Chats</h1>
          <p className="muted small">
            Every group conversation. Open one to read it, reply as the Pickup&amp;Drop team, or fix who is in it.
          </p>
        </div>
      </div>
      {error && <div className="notice error">{error}</div>}
      {flash && <div className="notice ok">{flash}</div>}

      <div className="card table-card">
        <table>
          <thead>
            <tr>
              <th>Route</th><th>Landing week</th><th>Members</th><th>Seats left</th>
              <th>Messages</th><th>Last message</th><th></th>
            </tr>
          </thead>
          <tbody>
            {chats?.map(chat => (
              <tr key={chat.id}>
                <td>
                  <a href="#" onClick={e => { e.preventDefault(); setOpenId(chat.id) }} style={{ fontWeight: 550 }}>
                    {chat.route.fromLocation} → {chat.route.toLocation}
                  </a>
                  {chat.official && <span className="stamp ok" style={{ marginLeft: 6 }}>official</span>}
                  {!chat.driverAssigned && <span className="stamp warn" style={{ marginLeft: 6 }}>no driver</span>}
                </td>
                <td className="muted small">
                  {chat.weekStart ? `${chat.weekStart} – ${chat.weekEnd}` : '—'}
                </td>
                <td>{chat.memberCount}</td>
                <td><span className={`stamp ${chat.seatsLeft > 0 ? 'ok' : 'warn'}`}>{chat.seatsLeft}</span></td>
                <td>{chat.messageCount}</td>
                <td className="muted small" style={{ maxWidth: 320 }}>
                  {chat.lastMessagePreview ? (
                    <>
                      <span style={{ fontWeight: 550, color: chat.lastMessageStaff ? 'var(--accent-deep)' : 'inherit' }}>
                        {chat.lastMessageAuthor}:
                      </span>{' '}
                      {chat.lastMessagePreview}
                      <br />
                      <span style={{ fontSize: 12 }}>{when(chat.lastMessageAt)}</span>
                    </>
                  ) : 'Nobody has written yet'}
                </td>
                <td style={{ textAlign: 'right' }}>
                  <button className="btn" onClick={() => setOpenId(chat.id)}>Open</button>
                </td>
              </tr>
            ))}
            {chats && chats.length === 0 && (
              <tr><td colSpan={7} className="muted">No groups yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {openId && (
        <ChatDetail
          groupId={openId}
          onClose={() => { setOpenId(null); load() }}
          onNote={note}
        />
      )}
    </>
  )
}

function ChatDetail({ groupId, onClose, onNote }: {
  groupId: string
  onClose: () => void
  onNote: (ok: string | null, err: string | null) => void
}) {
  const [detail, setDetail] = useState<AdminGroupDetail | null>(null)
  const [messages, setMessages] = useState<AdminMessage[]>([])
  const [body, setBody] = useState('')
  const [sending, setSending] = useState(false)
  const [localError, setLocalError] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const load = useCallback(async () => {
    const [d, m] = await Promise.all([
      api<AdminGroupDetail>(`/v1/admin/groups/${groupId}/detail`),
      api<AdminMessage[]>(`/v1/admin/groups/${groupId}/messages`),
    ])
    setDetail(d)
    setMessages(m)
  }, [groupId])

  useEffect(() => {
    load().catch(err => {
      onNote(null, err instanceof Error ? err.message : 'Failed to load the chat')
      onClose()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupId])

  async function send(e: React.FormEvent) {
    e.preventDefault()
    if (!body.trim()) return
    setSending(true)
    setLocalError(null)
    try {
      await api(`/v1/admin/groups/${groupId}/messages`, {
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

  async function remove(bookingId: string, name: string) {
    if (!confirm(`Remove ${name} from this group? Their booking stays active and travels individually.`)) return
    setLocalError(null)
    try {
      await api(`/v1/admin/groups/${groupId}/members/${bookingId}`, { method: 'DELETE' })
      await load()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Could not remove')
    }
  }

  if (!detail) return null

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal"
        style={{ width: 720, maxHeight: '90vh', overflowY: 'auto' }}
        onClick={e => e.stopPropagation()}
      >
        <h2>
          {detail.route.fromLocation} → {detail.route.toLocation}{' '}
          <span className="stamp ok">{detail.status.toLowerCase()}</span>
          {detail.official && <span className="stamp ok" style={{ marginLeft: 6 }}>official</span>}
        </h2>
        <p className="muted small" style={{ marginBottom: 14 }}>
          {detail.weekStart ? `Landing week ${detail.weekStart} – ${detail.weekEnd}` : 'No landing week'}
          {' · '}{detail.seatsLeft} seat{detail.seatsLeft === 1 ? '' : 's'} left
          {' · '}{detail.agreedDate ? `agreed ${detail.agreedDate}` : 'day not agreed'}
          {' · '}{detail.driver ? `driver ${detail.driver.name}` : 'no driver'}
        </p>

        {localError && <div className="notice error">{localError}</div>}

        <h2 style={{ fontSize: 15 }}>Members</h2>
        {detail.members.length === 0 ? (
          <p className="muted small">Nobody in this group.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Traveller</th><th>Contact</th><th>Party</th><th>Lands</th><th>Flight</th><th></th></tr>
            </thead>
            <tbody>
              {detail.members.map(m => (
                <tr key={m.bookingId}>
                  <td style={{ fontWeight: 550 }}>{m.name}</td>
                  <td className="muted small">
                    {m.email}{m.phone ? <><br />{m.phone}</> : null}
                    {m.contact ? <><br />{m.contact}</> : null}
                  </td>
                  <td>{m.partySize}</td>
                  <td>{m.travelDate}</td>
                  <td className="muted small">{m.flightNo ?? '—'}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="btn danger" onClick={() => remove(m.bookingId, m.name)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="row" style={{ marginTop: 10, marginBottom: 18 }}>
          <button className="btn" onClick={() => setAdding(true)} disabled={detail.seatsLeft <= 0}
                  title={detail.seatsLeft <= 0 ? 'The van is full' : ''}>
            + Add member
          </button>
        </div>

        <h2 style={{ fontSize: 15 }}>Conversation</h2>
        <div style={{
          border: '1px solid var(--line)', borderRadius: 10, padding: 14,
          maxHeight: 300, overflowY: 'auto', marginBottom: 12,
        }}>
          {messages.length === 0 && <p className="muted small">No messages yet.</p>}
          {messages.map(m => (
            <div key={m.id} style={{ marginBottom: 12 }}>
              <p className="muted small" style={{ marginBottom: 2 }}>
                <span style={{ fontWeight: 550, color: m.staff ? 'var(--accent-deep)' : 'var(--ink)' }}>
                  {m.authorName}
                </span>
                {m.staff && <span className="stamp ok" style={{ marginLeft: 6 }}>team</span>}
                {' '}{when(m.createdAt)}
              </p>
              <p style={{
                fontSize: 14, whiteSpace: 'pre-wrap',
                background: m.staff ? 'var(--accent-tint)' : 'var(--paper-deep)',
                borderRadius: 8, padding: '8px 11px', display: 'inline-block', maxWidth: '90%',
              }}>
                {m.body}
              </p>
            </div>
          ))}
        </div>

        <form onSubmit={send}>
          <div className="field">
            <label htmlFor="reply">Reply as the Pickup&amp;Drop team</label>
            <textarea
              id="reply"
              rows={3}
              maxLength={1000}
              value={body}
              onChange={e => setBody(e.target.value)}
              placeholder="Travellers see this as an official message from Pickup&Drop."
            />
          </div>
          <div className="row">
            <button type="button" className="btn" onClick={onClose}>Close</button>
            <button type="submit" className="btn primary" disabled={sending || !body.trim()}>
              {sending ? 'Sending…' : 'Send reply'}
            </button>
          </div>
        </form>

        {adding && (
          <AddMemberDialog
            groupId={groupId}
            onClose={() => setAdding(false)}
            onDone={async () => { setAdding(false); await load() }}
          />
        )}
      </div>
    </div>
  )
}

function AddMemberDialog({ groupId, onClose, onDone }: {
  groupId: string
  onClose: () => void
  onDone: () => void
}) {
  const [candidates, setCandidates] = useState<AddCandidate[] | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    api<AddCandidate[]>(`/v1/admin/groups/${groupId}/candidates`)
      .then(setCandidates)
      .catch(err => setLocalError(err instanceof Error ? err.message : 'Failed to load candidates'))
  }, [groupId])

  async function add(bookingId: string) {
    setLocalError(null)
    try {
      await api(`/v1/admin/groups/${groupId}/members`, {
        method: 'POST',
        body: JSON.stringify({ bookingId }),
      })
      onDone()
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Could not add')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" style={{ width: 560, maxHeight: '80vh', overflowY: 'auto' }}
           onClick={e => e.stopPropagation()}>
        <h2>Add a member</h2>
        <p className="muted small" style={{ marginBottom: 12 }}>
          Only bookings on this route, landing in this group&apos;s week, that still fit the remaining
          seats. Someone already in another group will be moved.
        </p>
        {localError && <div className="notice error">{localError}</div>}
        {candidates === null ? (
          <p className="muted small">Loading…</p>
        ) : candidates.length === 0 ? (
          <p className="muted">No eligible bookings for this group.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Traveller</th><th>Party</th><th>Lands</th><th>Now in</th><th></th></tr>
            </thead>
            <tbody>
              {candidates.map(c => (
                <tr key={c.bookingId}>
                  <td>
                    <span style={{ fontWeight: 550 }}>{c.name}</span>
                    <br /><span className="muted small">{c.email}</span>
                  </td>
                  <td>{c.partySize}</td>
                  <td>{c.travelDate}</td>
                  <td className="muted small">{c.currentGroupId ? 'another group' : 'riding alone'}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="btn primary" onClick={() => add(c.bookingId)}>Add</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="row" style={{ marginTop: 14 }}>
          <button type="button" className="btn" onClick={onClose}>Done</button>
        </div>
      </div>
    </div>
  )
}
