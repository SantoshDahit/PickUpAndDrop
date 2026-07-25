import Link from "next/link";
import { getActiveRoutes, getAllTiers, getTiersForRoute, type PriceTier, type Route } from "@/lib/db";
import FareCalculator from "@/components/FareCalculator";

function Icon({ children }: { children: React.ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {children}
    </svg>
  );
}

const PHOTOS: Record<string, string> = {
  seoul: "https://images.unsplash.com/photo-1546874177-9e664107314e",
  daejeon: "https://images.unsplash.com/photo-1506816561089-5cc37b3aa9b0",
  busan: "https://images.unsplash.com/photo-1538485399081-7191377e8241",
  default: "https://images.unsplash.com/photo-1517154421773-0529f29ea451",
  palace: "https://images.unsplash.com/photo-1583833008338-31a6657917ab",
  food: "https://images.unsplash.com/photo-1580651315530-69c8e0026377",
  plane: "https://images.unsplash.com/photo-1436491865332-7a61a109cc05",
};

function photoFor(route: Route): string {
  const to = route.to_location.toLowerCase();
  for (const key of ["seoul", "daejeon", "busan"]) {
    if (to.includes(key)) return PHOTOS[key];
  }
  return PHOTOS.default;
}

function cheapestFare(tiers: PriceTier[], routeId: number): number | null {
  const prices = tiers.filter((t) => t.route_id === routeId).map((t) => t.price_per_person);
  return prices.length ? Math.min(...prices) : null;
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
      <section className="hero-band overflow-hidden">
        <div className="relative mx-auto max-w-6xl px-5 pt-16 pb-14 sm:pt-24 sm:pb-20">
          {/* Floating photos (TravHub-style) */}
          <img
            src={`${PHOTOS.palace}?w=480&h=640&fit=crop`}
            alt="Gyeongbokgung Palace, Seoul"
            className="hidden lg:block absolute left-0 top-16 w-[190px] h-[250px] object-cover rounded-[28px] rotate-[-5deg] shadow-lg"
          />
          <img
            src={`${PHOTOS.plane}?w=480&h=640&fit=crop`}
            alt="Airplane wing above the clouds"
            className="hidden lg:block absolute right-0 top-24 w-[190px] h-[250px] object-cover rounded-[28px] rotate-[5deg] shadow-lg"
          />

          <div className="text-center max-w-2xl mx-auto">
            <p className="script text-[26px] mb-3">Welcome to Korea!</p>
            <h1 className="text-[2.7rem] leading-[1.08] sm:text-[3.6rem] mb-6">
              Your Adventure Starts
              <br />
              at the Arrivals Gate
            </h1>
            <p className="text-[17px] text-ink-soft max-w-xl mx-auto mb-10">
              Book an airport pickup before you fly — solo or with your whole
              group. A driver meets you inside the terminal, you split a fixed
              fare, and you pay in cash when you land.
            </p>
          </div>

          {routes.length > 0 && (
            <div className="max-w-4xl mx-auto">
              <FareCalculator routes={routes} tiers={tiers} />
            </div>
          )}

          <div className="flex flex-wrap justify-center gap-x-7 gap-y-2 mt-8 text-[14px] text-ink-soft">
            {["Fixed fares", "Driver meets you inside", "Cash on arrival"].map((t) => (
              <span key={t} className="inline-flex items-center gap-1.5">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
                {t}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* Destinations */}
      <section className="mx-auto max-w-6xl px-5 py-16 sm:py-20">
        <div className="text-center mb-10">
          <p className="script text-[24px] mb-1">Where are you headed?</p>
          <h2 className="text-[2rem]">Popular routes</h2>
        </div>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {routes.slice(0, 5).map((r) => {
            const from = cheapestFare(tiers, r.id);
            return (
              <Link key={r.id} href="/book" className="photo-card h-[300px] group">
                <img src={`${photoFor(r)}?w=800&h=600&fit=crop`} alt={r.to_location} />
                <div className="photo-card-body">
                  <p className="text-[13px] text-white/75 mb-0.5">{r.from_location}</p>
                  <div className="flex items-end justify-between gap-3">
                    <h3 className="font-display text-[22px] !text-white">{r.to_location}</h3>
                    {from !== null && (
                      <span className="rounded-full bg-accent px-3.5 py-1 text-[13px] font-medium whitespace-nowrap">
                        from ₩{from.toLocaleString()}
                      </span>
                    )}
                  </div>
                </div>
              </Link>
            );
          })}
          <Link href="/book" className="photo-card h-[300px] group">
            <img src={`${PHOTOS.default}?w=800&h=600&fit=crop`} alt="Anywhere in Korea" />
            <div className="photo-card-body">
              <p className="text-[13px] text-white/75 mb-0.5">Somewhere else?</p>
              <div className="flex items-end justify-between gap-3">
                <h3 className="font-display text-[22px] !text-white">Anywhere in Korea</h3>
                <span className="rounded-full bg-white/20 backdrop-blur px-3.5 py-1 text-[13px] font-medium whitespace-nowrap">
                  ask us
                </span>
              </div>
            </div>
          </Link>
        </div>
      </section>

      {/* Features + photos */}
      <section id="why-features" className="mx-auto max-w-6xl px-5 py-8 sm:py-12 grid gap-12 lg:grid-cols-[1fr_1.1fr] items-center">
        <div className="relative hidden sm:block h-[440px]">
          <img
            src={`${PHOTOS.seoul}?w=700&h=880&fit=crop`}
            alt="Seoul at night"
            className="absolute left-0 top-0 w-[62%] h-[82%] object-cover rounded-[28px] shadow-md"
          />
          <img
            src={`${PHOTOS.food}?w=600&h=600&fit=crop`}
            alt="Korean street food"
            className="absolute right-0 bottom-0 w-[48%] h-[52%] object-cover rounded-[28px] border-4 border-white shadow-lg"
          />
          <span className="absolute left-[52%] top-[6%] rounded-full bg-accent script !text-white text-[22px] px-5 py-2 rotate-3 shadow-md">
            first day in Korea
          </span>
        </div>
        <div>
          <p className="script text-[24px] mb-1">Why book with us</p>
          <h2 className="text-[2rem] mb-4">
            Made for your first day, not your hundredth
          </h2>
          <p className="text-ink-soft mb-8 max-w-lg">
            Local taxi apps want a Korean card, a Korean number and Korean
            language. You&rsquo;ll get there — but not in your first hour off a
            long-haul flight with three suitcases. Until then, there&rsquo;s us.
          </p>
          <div className="grid gap-6 sm:grid-cols-2">
            {FEATURES.map((f) => (
              <div key={f.title} className="flex gap-4 items-start">
                <span className="icon-tile">{f.icon}</span>
                <div>
                  <h3 className="text-[15.5px] mb-1">{f.title}</h3>
                  <p className="text-[14px] text-ink-soft leading-relaxed">{f.body}</p>
                </div>
              </div>
            ))}
          </div>
          <Link href="/book" className="btn btn-primary mt-9">
            Start booking
          </Link>
        </div>
      </section>

      {/* How it works */}
      <section id="how-it-works" className="hero-band mt-16">
        <div className="mx-auto max-w-6xl px-5 py-16 sm:py-20">
          <div className="text-center mb-12">
            <p className="script text-[24px] mb-1">Easy as 1, 2, 3</p>
            <h2 className="text-[2rem]">From the plane to your door</h2>
          </div>
          <div className="grid gap-10 sm:grid-cols-3 sm:gap-8">
            {STEPS.map((s, i) => (
              <div key={s.n} className="text-center sm:text-left">
                <div className="flex items-center justify-center sm:justify-start gap-4 mb-4">
                  <span className="flex h-12 w-12 items-center justify-center rounded-full bg-accent text-white font-display text-[18px] shadow-md">
                    {s.n}
                  </span>
                  {i < STEPS.length - 1 && (
                    <span className="hidden sm:block flex-1 border-t-2 border-dashed" style={{ borderColor: "#bfe9de" }} />
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
            <p className="script text-[24px] mb-1">Group pricing</p>
            <h2 className="text-[2rem] mb-4">The fuller the van, the less everyone pays</h2>
            <p className="text-ink-soft max-w-lg mb-7">
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
                <p className="text-[13px] text-muted mb-0.5">Route</p>
                <h3 className="text-[17px]">
                  {featured.from_location} → {featured.to_location}
                </h3>
              </div>
              <span className="script text-[20px]">per person</span>
            </div>
            <div className="px-6 py-3">
              <table className="w-full text-[15px]">
                <tbody>
                  {featuredTiers.map((t, i) => (
                    <tr key={t.id} className={i > 0 ? "border-t border-line" : ""}>
                      <td className="py-3 text-ink">
                        {t.group_size} {t.group_size === 1 ? "person" : "people"}
                        {i === featuredTiers.length - 1 && (
                          <span className="text-muted"> or more</span>
                        )}
                      </td>
                      <td className="py-3 text-right tabular font-display text-ink">
                        ₩{t.price_per_person.toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="px-6 py-3.5 text-[14px] font-medium border-t border-line" style={{ background: "var(--accent-tint)", color: "var(--accent-deep)" }}>
              Prices are fixed when you book — no surprises at the curb.
            </div>
          </div>
        </section>
      )}

      {/* Dark band */}
      <section id="why-us" className="dark-glow text-white">
        <div className="mx-auto max-w-6xl px-5 py-16 sm:py-20 grid gap-12 lg:grid-cols-2 items-start">
          <div>
            <p className="script text-[24px] mb-1 !text-[#7de3cc]">More than a ride</p>
            <h2 className="text-[2rem] mb-4 !text-white">
              Ask us for anything — we&rsquo;re locals
            </h2>
            <p className="text-white/60 max-w-lg leading-relaxed">
              Every booking is confirmed by a real person over WhatsApp, Viber
              or Messenger. Special luggage, odd hours, extra stops, or help
              beyond the ride — just ask.
            </p>
          </div>
          <ul className="grid gap-3">
            {[
              ["Bring your group", "One person books seats for everyone — just list the names."],
              ["Ask for anything", "Special luggage, odd hours, extra stops — add it to your request and we'll sort it out."],
              ["A person, not a bot", "We confirm every booking over WhatsApp, Viber or Messenger — whichever works before you land."],
              ["More help coming", "SIM cards, bank account help and room finding are on the way."],
            ].map(([title, body]) => (
              <li key={title} className="rounded-2xl border border-white/10 bg-white/[0.05] px-5 py-4 flex gap-4 items-start">
                <span className="mt-0.5 flex h-6 w-6 items-center justify-center rounded-full shrink-0" style={{ background: "rgba(27,188,155,0.3)" }}>
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#7de3cc" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                </span>
                <div>
                  <p className="font-display text-[15.5px] !text-white mb-0.5">{title}</p>
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
          <p className="script text-[24px] mb-1">Good to know</p>
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
        <div className="rounded-[28px] bg-accent px-8 py-12 sm:py-16 text-center text-white relative overflow-hidden">
          <p className="script text-[26px] !text-white/90 mb-1">Flying to Korea soon?</p>
          <h2 className="text-[2rem] mb-3 !text-white">Book your pickup before you board</h2>
          <p className="text-white/85 mb-8 max-w-md mx-auto">
            The earlier you book, the easier it is to fill your van and drop
            everyone&rsquo;s fare.
          </p>
          <Link
            href="/book"
            className="btn !h-[52px] !px-9 bg-white text-accent-deep hover:bg-navy hover:text-white transition-colors"
          >
            Book your pickup
          </Link>
        </div>
      </section>
    </div>
  );
}
