import Link from "next/link";
import { getActiveRoutes, getAllTiers, getTiersForRoute } from "@/lib/db";
import FareCalculator from "@/components/FareCalculator";

function Icon({ d, children }: { d?: string; children?: React.ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {d ? <path d={d} /> : children}
    </svg>
  );
}

const FEATURES = [
  {
    title: "Fixed fares",
    body: "The price you see when you book is what you pay. No meter, no surge.",
    icon: (
      <Icon>
        <rect x="2" y="6" width="20" height="12" rx="2" />
        <circle cx="12" cy="12" r="2.5" />
        <path d="M6 12h.01M18 12h.01" />
      </Icon>
    ),
  },
  {
    title: "Meet at arrivals",
    body: "Your driver waits inside the terminal with your name on a sign.",
    icon: (
      <Icon>
        <path d="M2.5 19h19" />
        <path d="M9.5 4.5 8 9l-5 3 1 1.5 5.5-1.5 2 4.5h1.8l-.8-6 4.5-1.3a1.7 1.7 0 0 0-1-3.2L9.5 4.5Z" />
      </Icon>
    ),
  },
  {
    title: "Pay cash on arrival",
    body: "No Korean card or bank account needed. Settle up when you land.",
    icon: (
      <Icon>
        <rect x="3" y="7" width="18" height="11" rx="2" />
        <circle cx="12" cy="12.5" r="2.5" />
        <path d="M7 7V5.5A1.5 1.5 0 0 1 8.5 4h7A1.5 1.5 0 0 1 17 5.5V7" />
      </Icon>
    ),
  },
  {
    title: "Cheaper together",
    body: "One booking covers your whole group — the fuller the van, the less each person pays.",
    icon: (
      <Icon>
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
      </Icon>
    ),
  },
];

const STEPS = [
  {
    n: "1",
    title: "Tell us your flight",
    body: "Pick your route, arrival date and group size. One form covers everyone travelling with you.",
  },
  {
    n: "2",
    title: "We confirm your van",
    body: "We match requests landing around the same time, then send you your driver and exact meeting point.",
  },
  {
    n: "3",
    title: "Land and ride",
    body: "Your driver is waiting at arrivals with your name. Load the bags, pay in cash, head straight to your door.",
  },
];

const FAQS = [
  {
    q: "What if my flight is delayed?",
    a: "Give us your flight number when you book and we track it. Your driver adjusts to the actual landing time at no extra cost.",
  },
  {
    q: "Do I need Korean won in cash?",
    a: "Yes — you pay the driver in Korean won when you arrive. There are currency exchanges and ATMs in the arrivals hall before you exit.",
  },
  {
    q: "Can I book for my friends too?",
    a: "That's the whole idea. One account can book seats for a full group — just add everyone's names to the booking.",
  },
  {
    q: "Where can you drop us off?",
    a: "Anywhere in Korea. If your destination isn't in the route list yet, put it in the notes and we'll quote it for you.",
  },
  {
    q: "How do I change or cancel a booking?",
    a: "Pending requests can be cancelled from My trips with one click. For confirmed rides, message us on the contact you gave and we'll sort it out.",
  },
];

export default async function Home() {
  const routes = await getActiveRoutes();
  const tiers = await getAllTiers();
  const featured = routes[0];
  const featuredTiers = featured ? await getTiersForRoute(featured.id) : [];

  return (
    <div>
      {/* Hero */}
      <section className="bg-surface border-b border-line">
        <div className="mx-auto max-w-6xl px-5 pt-14 pb-16 sm:pt-20 sm:pb-24 grid gap-12 lg:grid-cols-[1.2fr_420px] items-center">
          <div>
            <p className="inline-flex items-center rounded-full border border-line px-3.5 py-1.5 text-[13px] font-medium text-ink-soft mb-6">
              Incheon Airport → anywhere in Korea
            </p>
            <h1 className="text-[2.6rem] leading-[1.06] sm:text-[3.6rem] text-ink mb-6">
              Land in Korea.
              <br />
              Your ride is sorted.
            </h1>
            <p className="text-lg text-ink-soft max-w-xl mb-8">
              Book an airport pickup before you fly — solo or with your whole
              group. A driver meets you at arrivals, you split a fixed fare,
              and you pay in cash. No apps, no Korean card, no guesswork.
            </p>
            <div className="flex flex-wrap items-center gap-3">
              <Link href="/book" className="btn btn-primary !h-[50px] !px-7 !text-[15.5px]">
                Book a pickup
              </Link>
              <Link href="#how-it-works" className="btn btn-ghost !h-[50px] !px-7 !text-[15.5px]">
                How it works
              </Link>
            </div>
            <div className="flex flex-wrap gap-x-6 gap-y-2 mt-8 text-[14px] text-muted">
              {["Fixed fares", "Driver meets you inside", "Cash on arrival"].map((t) => (
                <span key={t} className="inline-flex items-center gap-1.5">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--ink)" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                  {t}
                </span>
              ))}
            </div>
          </div>

          {routes.length > 0 && <FareCalculator routes={routes} tiers={tiers} />}
        </div>
      </section>

      {/* Trust strip */}
      <section className="mx-auto max-w-6xl px-5 py-16">
        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((f) => (
            <div key={f.title} className="flex flex-col gap-3.5">
              <span className="icon-tile">{f.icon}</span>
              <div>
                <h3 className="text-[16px] mb-1">{f.title}</h3>
                <p className="text-[14.5px] text-ink-soft leading-relaxed">{f.body}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section id="how-it-works" className="border-y border-line" style={{ background: "#fafafa" }}>
        <div className="mx-auto max-w-6xl px-5 py-16 sm:py-20">
          <div className="max-w-xl mb-12">
            <p className="eyebrow mb-3">How it works</p>
            <h2 className="text-[2rem] mb-3">From the plane to your door in three steps</h2>
            <p className="text-ink-soft">
              No app to install, no account juggling — one booking and we handle
              the rest.
            </p>
          </div>
          <div className="grid gap-10 sm:grid-cols-3 sm:gap-8">
            {STEPS.map((s, i) => (
              <div key={s.n} className="relative">
                <div className="flex items-center gap-4 mb-4">
                  <span className="flex h-11 w-11 items-center justify-center rounded-full bg-navy text-white font-display font-bold text-[17px]">
                    {s.n}
                  </span>
                  {i < STEPS.length - 1 && (
                    <span className="hidden sm:block flex-1 border-t border-dashed" style={{ borderColor: "var(--line-strong)" }} />
                  )}
                </div>
                <h3 className="text-[17px] mb-2">{s.title}</h3>
                <p className="text-[14.5px] text-ink-soft leading-relaxed">{s.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Fares */}
      {featured && featuredTiers.length > 0 && (
        <section id="fares" className="mx-auto max-w-6xl px-5 py-16 sm:py-20 grid gap-12 lg:grid-cols-[1fr_460px] items-center">
          <div>
            <p className="eyebrow mb-3">Group pricing</p>
            <h2 className="text-[2rem] mb-4">The fuller the van, the less everyone pays</h2>
            <p className="text-ink-soft max-w-lg mb-6">
              Fares are per person and drop at every group size. Coming with
              classmates or family? One of you books, everyone saves — and you
              can keep adding people until the van is full.
            </p>
            <Link href="/book" className="btn btn-dark">
              Check your fare
            </Link>
          </div>

          <div className="ticket">
            <div className="px-6 pt-5 pb-4 border-b border-line flex items-center justify-between gap-3">
              <div>
                <p className="text-[13px] font-semibold text-muted mb-0.5">Route</p>
                <h3 className="text-[17px]">
                  {featured.from_location} → {featured.to_location}
                </h3>
              </div>
              <span className="text-[13px] font-semibold text-muted uppercase tracking-wider">per person</span>
            </div>
            <div className="px-6 py-3">
              <table className="w-full text-[15px]">
                <tbody>
                  {featuredTiers.map((t, i) => (
                    <tr key={t.id} className={i > 0 ? "border-t border-line" : ""}>
                      <td className="py-3">
                        {t.group_size} {t.group_size === 1 ? "person" : "people"}
                        {i === featuredTiers.length - 1 && (
                          <span className="text-muted"> or more</span>
                        )}
                      </td>
                      <td className="py-3 text-right tabular font-semibold">
                        ₩{t.price_per_person.toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="px-6 py-3.5 text-[14px] text-ink-soft border-t border-line" style={{ background: "#fafafa" }}>
              Prices are fixed when you book — no surprises at the curb.
            </div>
          </div>
        </section>
      )}

      {/* Why us */}
      <section id="why-us" className="bg-navy text-white">
        <div className="mx-auto max-w-6xl px-5 py-16 sm:py-20 grid gap-12 lg:grid-cols-2 items-start">
          <div>
            <p className="eyebrow mb-3">Why book with us</p>
            <h2 className="text-[2rem] mb-4 text-white">
              Made for your first day in Korea, not your hundredth
            </h2>
            <p className="text-white/65 max-w-lg leading-relaxed">
              Local taxi apps want a Korean card, a Korean number and Korean
              language. You&rsquo;ll get there — but not in your first hour off
              a long-haul flight with three suitcases. Until then, there&rsquo;s
              us.
            </p>
          </div>
          <ul className="grid gap-3">
            {[
              ["Bring your group", "One person books seats for everyone — just list the names."],
              ["Ask for anything", "Special luggage, odd hours, extra stops — add it to your request and we'll sort it out."],
              ["A person, not a bot", "We confirm every booking over WhatsApp, Viber or Messenger — whichever works before you land."],
              ["More help coming", "SIM cards, bank account help and room finding are on the way."],
            ].map(([title, body]) => (
              <li key={title} className="rounded-xl border border-white/12 px-5 py-4 flex gap-4 items-start">
                <span className="mt-0.5 flex h-6 w-6 items-center justify-center rounded-full shrink-0 bg-white/10">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                </span>
                <div>
                  <p className="font-medium text-[15.5px] mb-0.5">{title}</p>
                  <p className="text-[14px] text-white/55 leading-relaxed">{body}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" className="mx-auto max-w-3xl px-5 py-16 sm:py-20">
        <div className="text-center mb-10">
          <p className="eyebrow mb-3">FAQ</p>
          <h2 className="text-[2rem]">Questions travellers actually ask</h2>
        </div>
        <div className="grid gap-3">
          {FAQS.map((f) => (
            <details key={f.q} className="faq-item">
              <summary>{f.q}</summary>
              <p>{f.a}</p>
            </details>
          ))}
        </div>
      </section>

      {/* CTA band */}
      <section className="mx-auto max-w-6xl px-5 pb-4">
        <div className="rounded-2xl bg-surface border border-line shadow-md px-8 py-12 sm:py-14 text-center">
          <h2 className="text-[1.9rem] mb-3">Flying to Korea soon?</h2>
          <p className="text-ink-soft mb-8 max-w-md mx-auto">
            Put in your request now — the earlier you book, the easier it is to
            fill your van and drop everyone&rsquo;s fare.
          </p>
          <Link href="/book" className="btn btn-primary !h-[50px] !px-8 !text-[15.5px]">
            Book your pickup
          </Link>
        </div>
      </section>
    </div>
  );
}
