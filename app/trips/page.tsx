import Link from "next/link";
import { redirect } from "next/navigation";
import { api, priceFor, getRoutesWithTiers, type Booking } from "@/lib/api";
import { getSession } from "@/lib/session";
import { cancelOwnRequest } from "@/lib/actions";
import PageHeader from "@/components/PageHeader";

const STAMP: Record<string, string> = {
  ACTIVE: "stamp-confirmed",
  CANCELLED: "stamp-cancelled",
};

export default async function TripsPage({
  searchParams,
}: {
  searchParams: Promise<{ requested?: string; left?: string; error?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { requested, left, error } = await searchParams;
  const [trips, { tiers }] = await Promise.all([
    api<Booking[]>("/v1/bookings/me"),
    getRoutesWithTiers(),
  ]);

  return (
    <div>
      <PageHeader
        script="My trips"
        title="Your pickups"
        subtitle="Grouped pickups share one van — open the group to chat and settle your landing day together."
      />
      <div className="mx-auto max-w-3xl px-5 py-12">
        {requested && (
          <div className="notice-ok mb-8">
            Request received! If other travellers land within a week of you,
            you&rsquo;ll share a group — open it below to say hi.
          </div>
        )}
        {left && <div className="notice-ok mb-8">You left the group — your booking continues individually.</div>}
        {error && <div className="notice-error mb-8">{error}</div>}

        {trips.length === 0 ? (
          <div className="card p-10 text-center">
            <p className="font-display text-xl mb-2">No trips yet</p>
            <p className="text-ink-soft mb-6">Your booked pickups and their status will show up here.</p>
            <Link href="/book" className="btn btn-primary">Book your first pickup</Link>
          </div>
        ) : (
          <div className="grid gap-6">
            {trips.map((t) => {
              const perPerson = t.route ? priceFor(tiers, t.route.id, t.partySize) : null;
              return (
                <article key={t.id} className="ticket grid sm:grid-cols-[1fr_200px]">
                  <div className="p-6">
                    <div className="flex items-center justify-between gap-3 mb-4">
                      <p className="eyebrow">Pickup #{t.id.slice(-6).toUpperCase()}</p>
                      <span className={`stamp ${STAMP[t.status] ?? "stamp-pending"}`}>
                        {t.status.toLowerCase()}
                      </span>
                    </div>
                    <h2 className="text-xl mb-1">
                      {t.route ? `${t.route.fromLocation} → ${t.route.toLocation}` : "Route"}
                    </h2>
                    <p className="text-ink-soft text-[15px] mb-4">
                      {new Date(t.travelDate + "T00:00:00").toLocaleDateString("en-GB", {
                        weekday: "long", day: "numeric", month: "long", year: "numeric",
                      })}
                      {t.flightNo && <> · Flight {t.flightNo}</>}
                    </p>
                    <div className="flex flex-wrap gap-x-6 gap-y-1 text-[14.5px] text-ink-soft">
                      <span>
                        <span className="text-muted">Passengers:</span> {t.partySize}
                      </span>
                      <span>
                        <span className="text-muted">Ride:</span>{" "}
                        {t.groupId ? "shared group van" : "individual"}
                      </span>
                      {t.driver && (
                        <span>
                          <span className="text-muted">Driver:</span> {t.driver.name}
                          {t.driver.vehicle && <> · {t.driver.vehicle}</>}
                          {t.driver.plateNo && <> · {t.driver.plateNo}</>}
                          {t.driver.phone && <> · {t.driver.phone}</>}
                        </span>
                      )}
                    </div>
                    {t.status === "ACTIVE" && (
                      <div className="mt-5 flex flex-wrap gap-3">
                        {t.groupId && (
                          <Link href={`/groups/${t.groupId}`} className="btn btn-primary btn-sm">
                            Open group &amp; chat
                          </Link>
                        )}
                        <form action={cancelOwnRequest}>
                          <input type="hidden" name="id" value={t.id} />
                          <button className="btn btn-ghost btn-sm">Cancel request</button>
                        </form>
                      </div>
                    )}
                  </div>

                  <div className="ticket-perf p-6 flex flex-col justify-center gap-1 text-right sm:text-left">
                    <p className="field-label mb-0">Per person</p>
                    <p className="tabular font-semibold">
                      {perPerson !== null ? `₩${perPerson.toLocaleString()}` : "—"}
                    </p>
                    <p className="field-label mb-0 mt-3">Total · cash</p>
                    <p className="font-display text-2xl tabular">
                      {perPerson !== null ? `₩${(perPerson * t.partySize).toLocaleString()}` : "—"}
                    </p>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
