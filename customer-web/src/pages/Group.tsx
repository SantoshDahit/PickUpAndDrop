import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, type Booking, type GroupView, type Message } from '../api'

export default function Group() {
  const { id } = useParams()
  const [view, setView] = useState<GroupView | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [body, setBody] = useState('')
  const [myDate, setMyDate] = useState('')
  const [myBookingId, setMyBookingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const navigate = useNavigate()

  const load = useCallback(async () => {
    try {
      const [v, msgs, mine] = await Promise.all([
        api<GroupView>(`/v1/groups/${id}`),
        api<Message[]>(`/v1/groups/${id}/messages`),
        api<Booking[]>('/v1/bookings/me'),
      ])
      setView(v)
      setMessages(msgs)
      const me = v.members.find(m => m.me)
      if (me) setMyDate(me.travelDate)
      const booking = mine.find(b => b.groupId === id && b.status === 'ACTIVE')
      setMyBookingId(booking?.id ?? null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load group')
    }
  }, [id])

  useEffect(() => { load() }, [load])

  async function send(e: React.FormEvent) {
    e.preventDefault()
    if (!body.trim()) return
    try {
      await api(`/v1/groups/${id}/messages`, { method: 'POST', body: JSON.stringify({ body }) })
      setBody('')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Message failed')
    }
  }

  async function updateDate(e: React.FormEvent) {
    e.preventDefault()
    if (!myBookingId) return
    try {
      await api(`/v1/bookings/${myBookingId}`, { method: 'PATCH', body: JSON.stringify({ travelDate: myDate }) })
      setFlash('Your landing day is updated.')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed')
    }
  }

  async function leave() {
    if (!confirm('Leave this group? Your booking continues individually.')) return
    try {
      await api(`/v1/groups/${id}/members/me`, { method: 'DELETE' })
      navigate('/trips', { state: { flash: 'You left the group — your booking continues individually.' } })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Leave failed')
    }
  }

  if (!view) return <>{error && <div className="notice error">{error}</div>}</>

  return (
    <>
      <p className="eyebrow">Travel group</p>
      <h1>{view.route.fromLocation} → {view.route.toLocation}</h1>
      <p className="muted" style={{ margin: '6px 0 16px' }}>
        Travellers landing within a week of each other. Chat below and settle on one landing day.
      </p>
      {flash && <div className="notice ok">{flash}</div>}
      {error && <div className="notice error">{error}</div>}
      {view.agreedDate && (
        <div className="notice ok">🎉 Everyone agrees on <strong>{view.agreedDate}</strong> — you're set.</div>
      )}
      {view.driver && (
        <div className="card form-card" style={{ marginBottom: 16, padding: '14px 18px' }}>
          🚐 Your driver: <strong>{view.driver.name}</strong>
          {view.driver.vehicle && <> — {view.driver.vehicle}{view.driver.plateNo ? ` · ${view.driver.plateNo}` : ''}</>}
          {view.driver.phone && <> · <a href={`tel:${view.driver.phone}`}>{view.driver.phone}</a></>}
        </div>
      )}

      <div className="grid-2">
        <div>
          <h2>Members</h2>
          {view.members.map((m, i) => (
            <div className="card member-card" key={i}>
              <span className="avatar">{m.firstName.charAt(0).toUpperCase()}</span>
              <div>
                <strong>{m.firstName}</strong> {m.me && <span className="stamp">you</span>}
                <p className="muted small">
                  {m.partySize} {m.partySize === 1 ? 'traveller' : 'travellers'} · lands <strong>{m.travelDate}</strong>
                </p>
                {m.intro && <p className="small" style={{ color: 'var(--ink-soft)', marginTop: 3 }}>{m.intro}</p>}
              </div>
            </div>
          ))}
          {myBookingId && (
            <div className="card form-card" style={{ marginTop: 12, padding: '16px 18px' }}>
              <h2>Your landing day</h2>
              <form className="date-row" onSubmit={updateDate}>
                <input type="date" value={myDate} onChange={e => setMyDate(e.target.value)} />
                <button className="btn sm" type="submit">Update</button>
              </form>
              <button className="btn sm" style={{ marginTop: 10 }} onClick={leave}>Leave group — ride alone</button>
            </div>
          )}
        </div>
        <div>
          <h2>Group chat</h2>
          <div className="card chat-card">
            <div className="chat-scroll">
              {messages.length === 0 && <p className="muted small center">No messages yet — say hi and share your plans!</p>}
              {messages.map(m => (
                <div className={`chat-msg${m.mine ? ' mine' : ''}`} key={m.id}>
                  <span className="avatar">{m.authorFirstName.charAt(0).toUpperCase()}</span>
                  <div className="chat-bubble">
                    <span className="chat-meta"><strong>{m.authorFirstName}</strong>
                      <span className="muted">{m.createdAt.slice(0, 16).replace('T', ' ')}</span></span>
                    <p>{m.body}</p>
                  </div>
                </div>
              ))}
            </div>
            <form className="chat-form" onSubmit={send}>
              <input value={body} maxLength={1000} placeholder="Write a message…"
                     onChange={e => setBody(e.target.value)} />
              <button className="btn primary sm" type="submit">Send</button>
            </form>
          </div>
        </div>
      </div>
    </>
  )
}
