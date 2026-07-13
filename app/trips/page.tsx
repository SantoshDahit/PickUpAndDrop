import Link from "next/link";
import { redirect } from "next/navigation";
import { all } from "@/lib/db";
import { getSession } from "@/lib/session";
import { cancelOwnRequest } from "@/lib/actions";

type TripRow = {
  id: number;
  travel_date: string;
  flight_no: string | null;
  num_people: number;
  passenger_names: string | null;
  status: string;
  price_per_person: number;
  total_price: number;
  created_at: string;
  from_location: string;
  to_location: string;
  driver_name: string | null;
  driver_phone: string | null;
};

const STAMP: Record<string, string> = {
  pending: "stamp-pending",
  confirmed: "stamp-confirmed",
  completed: "stamp-completed",
  cancelled: "stamp-cancelled",
};

export default async function TripsPage({
  searchParams,
}: {
  searchParams: Promise<{ requested?: string }>;
}) {
  const session = await getSession();
  if (!session) redirect("/login");

  const { requested } = await searchParams;

  const trips = await all<TripRow>(
    `SELECT t.id, t.travel_date, t.flight_no, t.num_people, t.passenger_names,
            t.status, t.price_per_person, t.total_price, t.created_at,
            r.from_location, r.to_location,
            d.name AS driver_name, d.phone AS driver_phone
       FROM trip_requests t
       JOIN routes r ON r.id = t.route_id
       LEFT JOIN drivers d ON d.id = t.driver_id
      WHERE t.user_id = ?
      ORDER BY t.travel_date DESC, t.id DESC`,
    [session.uid]
  );

  return (
    <div className="mx-auto max-w-3xl px-5 py-12">
      <p className="eyebrow mb-3">My trips</p>
      <h1 className="text-3xl mb-2">Your pickups</h1>
      <p className="text-ink-soft mb-8">
        Pending requests get confirmed once we&rsquo;ve set your van and driver —
        we&rsquo;ll reach you on the contact you gave.
      </p>

      {requested && (
        <div className="notice-ok mb-8">
          Request received! We&rsquo;ll be in touch to confirm your van. You can
          see its status below.
        </div>
      )}

      {trips.length === 0 ? (
        <div className="card p-10 text-center">
          <p className="font-display text-xl mb-2">No trips yet</p>
          <p className="text-ink-soft mb-6">Your booked pickups will show up here as tickets.</p>
          <Link href="/book" className="btn btn-primary">Book your first pickup</Link>
        </div>
      ) : (
        <div className="grid gap-6">
          {trips.map((t) => (
            <article key={t.id} className="ticket grid sm:grid-cols-[1fr_200px]">
              {/* Ticket body */}
              <div className="p-6">
                <div className="flex items-center justify-between gap-3 mb-4">
                  <p className="eyebrow">Pickup #{String(t.id).padStart(4, "0")}</p>
                  <span className={`stamp ${STAMP[t.status] ?? "stamp-pending"}`}>{t.status}</span>
                </div>
                <h2 className="text-xl mb-1">
                  {t.from_location} → {t.to_location}
                </h2>
                <p className="text-ink-soft text-[15px] mb-4">
                  {new Date(t.travel_date + "T00:00:00").toLocaleDateString("en-GB", {
                    weekday: "long",
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                  })}
                  {t.flight_no && <> · Flight {t.flight_no}</>}
                </p>
                <div className="flex flex-wrap gap-x-6 gap-y-1 text-[14.5px] text-ink-soft">
                  <span>
                    <span className="text-muted">Passengers:</span> {t.num_people}
                  </span>
                  {t.passenger_names && <span>{t.passenger_names}</span>}
                  {t.driver_name && (
                    <span>
                      <span className="text-muted">Driver:</span> {t.driver_name}
                      {t.driver_phone && <> · {t.driver_phone}</>}
                    </span>
                  )}
                </div>
                {t.status === "pending" && (
                  <form action={cancelOwnRequest} className="mt-5">
                    <input type="hidden" name="id" value={t.id} />
                    <button className="btn btn-ghost btn-sm">Cancel request</button>
                  </form>
                )}
              </div>

              {/* Ticket stub */}
              <div className="ticket-perf p-6 flex flex-col justify-center gap-1 text-right sm:text-left">
                <p className="field-label mb-0">Per person</p>
                <p className="tabular font-semibold">₩{t.price_per_person.toLocaleString()}</p>
                <p className="field-label mb-0 mt-3">Total · cash</p>
                <p className="font-display text-2xl tabular">₩{t.total_price.toLocaleString()}</p>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
