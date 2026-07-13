import { redirect } from "next/navigation";
import { all, getActiveDrivers } from "@/lib/db";
import { getSession } from "@/lib/session";
import { updateRequest } from "@/lib/actions";
import AdminNav from "@/components/AdminNav";

type AdminRow = {
  id: number;
  travel_date: string;
  flight_no: string | null;
  num_people: number;
  passenger_names: string | null;
  contact: string | null;
  notes: string | null;
  status: string;
  driver_id: number | null;
  price_per_person: number;
  total_price: number;
  driver_fee: number | null;
  vehicle_cost: number | null;
  other_cost: number | null;
  from_location: string;
  to_location: string;
  customer_name: string;
  customer_email: string;
  customer_phone: string | null;
};

const STAMP: Record<string, string> = {
  pending: "stamp-pending",
  confirmed: "stamp-confirmed",
  completed: "stamp-completed",
  cancelled: "stamp-cancelled",
};

export default async function AdminPage() {
  const session = await getSession();
  if (!session?.isAdmin) redirect("/login");

  const requests = await all<AdminRow>(
    `SELECT t.*, r.from_location, r.to_location,
            u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone
       FROM trip_requests t
       JOIN routes r ON r.id = t.route_id
       JOIN users u ON u.id = t.user_id
      ORDER BY t.travel_date ASC, t.id ASC`
  );

  const drivers = await getActiveDrivers();

  // Group by travel date — this is the admin's van-planning view.
  const byDate = new Map<string, AdminRow[]>();
  for (const r of requests) {
    const list = byDate.get(r.travel_date) ?? [];
    list.push(r);
    byDate.set(r.travel_date, list);
  }

  const pendingCount = requests.filter((r) => r.status === "pending").length;

  return (
    <div className="mx-auto max-w-6xl px-5 py-12">
      <p className="eyebrow mb-3">Admin</p>
      <h1 className="text-3xl mb-2">Trip requests</h1>
      <p className="text-ink-soft mb-8">
        {requests.length} total · {pendingCount} pending. Grouped by arrival date —
        requests on the same day and route can share one van.
      </p>

      <AdminNav active="requests" />

      {requests.length === 0 && (
        <div className="card p-10 text-center text-ink-soft">No requests yet.</div>
      )}

      <div className="grid gap-10">
        {[...byDate.entries()].map(([date, rows]) => {
          const paxTotal = rows
            .filter((r) => r.status !== "cancelled")
            .reduce((s, r) => s + r.num_people, 0);
          return (
            <section key={date}>
              <div className="flex items-baseline justify-between mb-3">
                <h2 className="text-xl">
                  {new Date(date + "T00:00:00").toLocaleDateString("en-GB", {
                    weekday: "long",
                    day: "numeric",
                    month: "long",
                    year: "numeric",
                  })}
                </h2>
                <span className="text-[14px] text-muted">{paxTotal} passengers this day</span>
              </div>

              <div className="grid gap-4">
                {rows.map((r) => {
                  const costs = (r.driver_fee ?? 0) + (r.vehicle_cost ?? 0) + (r.other_cost ?? 0);
                  const hasCosts = r.driver_fee !== null || r.vehicle_cost !== null || r.other_cost !== null;
                  return (
                    <div key={r.id} className="card p-5">
                      <div className="flex flex-wrap items-center gap-3 mb-3">
                        <span className="font-semibold">#{String(r.id).padStart(4, "0")}</span>
                        <span className={`stamp ${STAMP[r.status] ?? "stamp-pending"}`}>{r.status}</span>
                        <span className="text-ink-soft">
                          {r.from_location} → {r.to_location}
                        </span>
                        <span className="text-muted text-[14px]">
                          {r.num_people} pax{r.flight_no ? ` · ${r.flight_no}` : ""}
                        </span>
                        <span className="ml-auto tabular font-semibold">
                          ₩{r.total_price.toLocaleString()}
                          {hasCosts && (
                            <span
                              className="ml-2 text-[13.5px] font-medium"
                              style={{ color: r.total_price - costs >= 0 ? "var(--green-ok)" : "var(--brick)" }}
                            >
                              ({r.total_price - costs >= 0 ? "+" : "−"}₩
                              {Math.abs(r.total_price - costs).toLocaleString()})
                            </span>
                          )}
                        </span>
                      </div>

                      <p className="text-[14.5px] text-ink-soft mb-1">
                        <span className="text-muted">Booked by:</span> {r.customer_name} · {r.customer_email}
                        {r.customer_phone && <> · {r.customer_phone}</>}
                        {r.contact && <> · <span className="text-muted">reach at:</span> {r.contact}</>}
                      </p>
                      {r.passenger_names && (
                        <p className="text-[14.5px] text-ink-soft mb-1">
                          <span className="text-muted">Passengers:</span> {r.passenger_names}
                        </p>
                      )}
                      {r.notes && (
                        <p className="text-[14.5px] text-ink-soft mb-1">
                          <span className="text-muted">Notes:</span> {r.notes}
                        </p>
                      )}

                      <form
                        key={`${r.status}-${r.driver_id}-${r.driver_fee}-${r.vehicle_cost}-${r.other_cost}`}
                        action={updateRequest}
                        className="mt-4 pt-4 rule-dashed grid gap-3 sm:grid-cols-[1.2fr_1fr_1fr_1fr_1fr_auto] items-end"
                      >
                        <input type="hidden" name="id" value={r.id} />
                        <div>
                          <label className="field-label">Driver</label>
                          <select name="driver_id" defaultValue={r.driver_id ?? 0} className="field-input h-10 text-[14px]">
                            <option value={0}>— unassigned —</option>
                            {drivers.map((d) => (
                              <option key={d.id} value={d.id}>
                                {d.name} · {d.seats} seats{d.owns_vehicle ? " · own car" : " · rental"}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div>
                          <label className="field-label">Status</label>
                          <select name="status" defaultValue={r.status} className="field-input h-10 text-[14px]">
                            <option value="pending">pending</option>
                            <option value="confirmed">confirmed</option>
                            <option value="completed">completed</option>
                            <option value="cancelled">cancelled</option>
                          </select>
                        </div>
                        <div>
                          <label className="field-label">Driver fee</label>
                          <input name="driver_fee" type="number" min={0} defaultValue={r.driver_fee ?? ""} className="field-input h-10 text-[14px]" placeholder="₩" />
                        </div>
                        <div>
                          <label className="field-label">Vehicle</label>
                          <input name="vehicle_cost" type="number" min={0} defaultValue={r.vehicle_cost ?? ""} className="field-input h-10 text-[14px]" placeholder="₩" />
                        </div>
                        <div>
                          <label className="field-label">Other</label>
                          <input name="other_cost" type="number" min={0} defaultValue={r.other_cost ?? ""} className="field-input h-10 text-[14px]" placeholder="₩" />
                        </div>
                        <button className="btn btn-dark btn-sm h-10">Save</button>
                      </form>
                    </div>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}
