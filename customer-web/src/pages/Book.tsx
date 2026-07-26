import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, type OpenRide, type Route } from '../api'

export default function Book() {
  const [routes, setRoutes] = useState<Route[]>([])
  const [rides, setRides] = useState<OpenRide[]>([])
  const [form, setForm] = useState({
    routeId: '', travelDate: '', partySize: 1, wantsGroup: true,
    intro: '', flightNo: '', contact: '', notes: '',
  })
  const [joinRide, setJoinRide] = useState<OpenRide | null>(null)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    api<Route[]>('/v1/routes').then(r => { setRoutes(r); if (r[0]) setForm(f => ({ ...f, routeId: f.routeId || r[0].id })) })
    api<OpenRide[]>('/v1/groups/open').then(setRides).catch(() => {})
  }, [])

  function pickRide(ride: OpenRide) {
    setJoinRide(ride)
    setForm(f => ({ ...f, routeId: ride.route.id, travelDate: ride.targetDate, wantsGroup: true }))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await api('/v1/bookings', {
        method: 'POST',
        body: JSON.stringify({
          routeId: form.routeId,
          groupId: joinRide?.id ?? null,
          travelDate: form.travelDate,
          partySize: form.partySize,
          matchPref: form.wantsGroup ? 'GROUP' : 'INDIVIDUAL',
          intro: form.intro || null,
          flightNo: form.flightNo || null,
          contact: form.contact || null,
          notes: form.notes || null,
        }),
      })
      navigate('/trips', { state: { flash: joinRide ? 'You joined the ride — say hi in the group chat!' : 'Booked! Find it below.' } })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Booking failed')
    }
  }

  return (
    <>
      <p className="eyebrow">Book</p>
      <h1>Book your pickup.</h1>
      <p className="muted" style={{ margin: '6px 0 20px' }}>
        Tell us about your landing — travellers arriving within a week of each other can share one van.
      </p>
      {error && <div className="notice error">{error}</div>}
      <div className="grid-2">
        <form className="card form-card" onSubmit={submit}>
          {joinRide && (
            <div className="notice ok">
              Joining the published ride to <strong>{joinRide.route.toLocation}</strong> around <strong>{joinRide.targetDate}</strong>.
              {' '}<a href="#" onClick={e => { e.preventDefault(); setJoinRide(null) }}>Book normally instead</a>
            </div>
          )}
          <div className="field"><label htmlFor="route">Route</label>
            <select id="route" value={form.routeId} disabled={!!joinRide}
                    onChange={e => setForm({ ...form, routeId: e.target.value })}>
              {routes.map(r => <option key={r.id} value={r.id}>{r.fromLocation} → {r.toLocation}</option>)}
            </select></div>
          <div className="field"><label htmlFor="date">Landing day</label>
            <input id="date" type="date" value={form.travelDate} required
                   onChange={e => setForm({ ...form, travelDate: e.target.value })} />
            {joinRide && <p className="muted small" style={{ marginTop: 5 }}>Must land within 7 days of {joinRide.targetDate}.</p>}
          </div>
          <div className="field"><label htmlFor="party">Travellers in your party</label>
            <select id="party" value={form.partySize} onChange={e => setForm({ ...form, partySize: Number(e.target.value) })}>
              {[1,2,3,4,5,6].map(n => <option key={n} value={n}>{n}</option>)}
            </select></div>
          {!joinRide && (
            <div className="field">
              <span style={{ display: 'block', fontSize: 13, fontWeight: 550, color: 'var(--ink-soft)', marginBottom: 6 }}>Riding preference</span>
              <label className="choice">
                <input type="radio" checked={form.wantsGroup} onChange={() => setForm({ ...form, wantsGroup: true })} />
                <span><strong>Group me with other travellers</strong> — chat and settle the final day together, everyone pays less.</span>
              </label>
              <label className="choice">
                <input type="radio" checked={!form.wantsGroup} onChange={() => setForm({ ...form, wantsGroup: false })} />
                <span><strong>I'd rather ride alone</strong> — just my party, no matching.</span>
              </label>
            </div>
          )}
          <div className="field"><label htmlFor="intro">About you <span className="muted">(shown to your group)</span></label>
            <textarea id="intro" rows={2} maxLength={300} value={form.intro}
                      placeholder="e.g. Exchange student from Spain, two big suitcases"
                      onChange={e => setForm({ ...form, intro: e.target.value })} /></div>
          <div className="field"><label htmlFor="flight">Flight number <span className="muted">(optional)</span></label>
            <input id="flight" maxLength={20} value={form.flightNo} placeholder="KE082"
                   onChange={e => setForm({ ...form, flightNo: e.target.value })} /></div>
          <div className="field"><label htmlFor="contact">Contact for the driver <span className="muted">(optional, never shown to the group)</span></label>
            <input id="contact" maxLength={100} value={form.contact} placeholder="WhatsApp / KakaoTalk"
                   onChange={e => setForm({ ...form, contact: e.target.value })} /></div>
          <div className="field"><label htmlFor="notes">Notes for us <span className="muted">(optional)</span></label>
            <textarea id="notes" rows={2} maxLength={1000} value={form.notes}
                      onChange={e => setForm({ ...form, notes: e.target.value })} /></div>
          <button className="btn primary block" type="submit">
            {joinRide ? 'Join this ride' : 'Book my pickup'}
          </button>
          <p className="muted small center" style={{ marginTop: 10 }}>No payment now — pay the driver in cash when you land.</p>
        </form>

        <div>
          <h2>Or join a published ride</h2>
          <p className="muted small" style={{ marginBottom: 12 }}>
            Rides with a date already set — join one and you're instantly in its group.
          </p>
          {rides.length === 0 && <div className="card form-card"><p className="muted">No open rides right now — book normally and we'll match you.</p></div>}
          {rides.map(ride => (
            <div className="card ride-card" key={ride.id}>
              <div>
                <strong>{ride.route.fromLocation} → {ride.route.toLocation}</strong>
                <div className="muted small">
                  around {ride.targetDate} · {ride.memberCount} {ride.memberCount === 1 ? 'traveller' : 'travellers'} in
                  · {ride.seatsLeft} seats left
                </div>
              </div>
              <button className="btn sm" onClick={() => pickRide(ride)}>Join</button>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
