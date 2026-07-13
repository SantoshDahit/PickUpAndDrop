import Link from "next/link";
import { getActiveRoutes, getTiersForRoute } from "@/lib/db";

export default async function Home() {
  const routes = await getActiveRoutes();
  const featured = routes[0];
  const tiers = featured ? await getTiersForRoute(featured.id) : [];

  return (
    <div className="mx-auto max-w-5xl px-5">
      {/* Hero */}
      <section className="pt-16 pb-14 sm:pt-24 sm:pb-20 grid gap-12 lg:grid-cols-[1.15fr_1fr] items-center">
        <div>
          <p className="eyebrow mb-4">Airport pickup · Incheon → your doorstep</p>
          <h1 className="text-4xl sm:text-[3.4rem] text-ink mb-5">
            You just landed in Korea.
            <br />
            <span className="text-brick">We&rsquo;ll take it from here.</span>
          </h1>
          <p className="text-lg text-ink-soft max-w-xl mb-8">
            Tell us where you&rsquo;re landing and where you need to go — solo or
            with your whole group. Book together, split the fare, and pay the
            driver in cash when you arrive. Any route, any request.
          </p>
          <div className="flex flex-wrap gap-3">
            <Link href="/book" className="btn btn-primary">
              Book a pickup
            </Link>
            <Link href="#fares" className="btn btn-ghost">
              See fares
            </Link>
          </div>
        </div>

        {/* Fare card */}
        {featured && tiers.length > 0 && (
          <div id="fares" className="ticket p-0">
            <div className="px-6 pt-5 pb-4 border-b border-line">
              <p className="eyebrow mb-1">Fare card</p>
              <h2 className="text-xl">
                {featured.from_location} → {featured.to_location}
              </h2>
            </div>
            <div className="px-6 py-4">
              <table className="w-full text-[15px]">
                <thead>
                  <tr className="text-left text-muted text-[12.5px] uppercase tracking-wider">
                    <th className="py-2 font-semibold">Group size</th>
                    <th className="py-2 font-semibold text-right">Per person</th>
                  </tr>
                </thead>
                <tbody>
                  {tiers.map((t, i) => (
                    <tr key={t.id} className={i > 0 ? "border-t border-line" : ""}>
                      <td className="py-2.5">
                        {t.group_size} {t.group_size === 1 ? "person" : "people"}
                        {i === tiers.length - 1 && (
                          <span className="text-muted"> or more</span>
                        )}
                      </td>
                      <td className="py-2.5 text-right tabular font-semibold">
                        ₩{t.price_per_person.toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="px-6 py-3.5 text-pine text-[14px] font-medium rule-dashed" style={{ background: "var(--pine-tint)" }}>
              More people in the van, less everyone pays.
            </div>
          </div>
        )}
      </section>

      {/* How it works */}
      <section className="py-14 border-t border-line">
        <p className="eyebrow mb-3">How it works</p>
        <h2 className="text-3xl mb-10 max-w-lg">
          Three steps between the plane and your new room.
        </h2>
        <div className="grid sm:grid-cols-3 gap-6">
          {[
            {
              n: "01",
              title: "Tell us your flight",
              body: "Route, arrival date, flight number, and how many of you are coming. Booking for your whole group takes one form.",
            },
            {
              n: "02",
              title: "We confirm your van",
              body: "We group requests landing around the same time so the fare stays low, then send you the driver and meeting point.",
            },
            {
              n: "03",
              title: "Pay cash on arrival",
              body: "Your driver waits at arrivals with your name. Pay in cash when you land — no card or Korean account needed.",
            },
          ].map((s) => (
            <div key={s.n} className="card p-6">
              <p className="font-display text-brick text-lg mb-3">{s.n}</p>
              <h3 className="text-lg mb-2">{s.title}</h3>
              <p className="text-[15px] text-ink-soft">{s.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Why us */}
      <section className="py-14 border-t border-line grid gap-10 lg:grid-cols-2 items-start">
        <div>
          <p className="eyebrow mb-3">Why book with us</p>
          <h2 className="text-3xl mb-5 max-w-md">
            Made for your first day, not your hundredth.
          </h2>
          <p className="text-ink-soft max-w-lg">
            Local taxi apps need a local card, a local number, and the local
            language. You&rsquo;ll get there — but not in your first hour off a
            long flight with three suitcases. Until then, there&rsquo;s us.
          </p>
        </div>
        <ul className="grid gap-3">
          {[
            ["Bring your group", "One person can book seats for everyone — just list the names."],
            ["Fair, fixed fares", "The price you see when you book is the price you pay. No meter anxiety."],
            ["Ask for anything", "Special luggage, odd hours, extra stops — put it in your request and we'll sort it out."],
            ["More services soon", "SIM cards, bank account help, and room finding are on the way."],
          ].map(([title, body]) => (
            <li key={title} className="card px-5 py-4 flex gap-4 items-start">
              <span className="mt-1.5 h-2 w-2 rounded-full bg-brick shrink-0" />
              <div>
                <p className="font-semibold text-[15.5px]">{title}</p>
                <p className="text-[14.5px] text-ink-soft">{body}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>

      {/* CTA band */}
      <section className="my-14 ticket px-8 py-10 text-center">
        <h2 className="text-3xl mb-3">Flying to Korea soon?</h2>
        <p className="text-ink-soft mb-7 max-w-md mx-auto">
          Put in your request now — the earlier you book, the easier it is to
          fill your van and drop the fare.
        </p>
        <Link href="/book" className="btn btn-primary">
          Book your pickup
        </Link>
      </section>
    </div>
  );
}
