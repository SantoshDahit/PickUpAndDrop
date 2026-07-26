import { Link } from 'react-router-dom'
import { getToken } from '../api'
import { Header } from '../App'

export default function Landing() {
  const authed = !!getToken()
  return (
    <>
      <Header />
      <main className="container">
        <section className="hero">
          <span className="stamp">Incheon Airport → anywhere in Korea</span>
          <h1>Land in Korea.<br />Your ride is already sorted.</h1>
          <p>
            Book an airport pickup before you fly — solo or with your whole group.
            A driver meets you at arrivals, you split a fixed fare, and you pay in cash.
            No apps, no Korean card, no guesswork.
          </p>
          <div className="hero-actions">
            <Link className="btn primary" to={authed ? '/book' : '/signup'}>Book a pickup</Link>
            {!authed && <Link className="btn" to="/login">Log in</Link>}
          </div>
          <ul className="checks">
            <li>Fixed fares</li><li>Driver meets you inside</li><li>Cash on arrival</li>
          </ul>
        </section>
        <section className="feature-grid">
          <div><h3>Meet at arrivals</h3><p>Your driver waits inside the terminal with your name — no bus maze with your luggage.</p></div>
          <div><h3>Cheaper together</h3><p>We group travellers landing within a week of each other; you chat and settle one landing day.</p></div>
          <div><h3>Join a ride</h3><p>Or hop on a published ride that already has a date — see the seats left before you book.</p></div>
          <div><h3>Pay cash on arrival</h3><p>No Korean card or bank account needed. The price you see is what you pay the driver.</p></div>
        </section>
      </main>
      <footer className="site-footer"><div className="container">
        Pickup&Drop — airport pickups anywhere in Korea. Fixed fares, cash on arrival.
      </div></footer>
    </>
  )
}
