"use client";

import { useMemo, useState } from "react";
import type { PriceTier, Route } from "@/lib/db";
import { createTripRequest } from "@/lib/actions";

function priceFor(tiers: PriceTier[], routeId: number, n: number): number | null {
  const routeTiers = tiers.filter((t) => t.route_id === routeId);
  if (routeTiers.length === 0) return null;
  let match: PriceTier | null = null;
  for (const t of routeTiers) {
    if (t.group_size <= n) match = t;
  }
  return (match ?? routeTiers[0]).price_per_person;
}

/** Next tier above n with a cheaper per-person price, if any. */
function nextSaving(tiers: PriceTier[], routeId: number, n: number) {
  const current = priceFor(tiers, routeId, n);
  if (current === null) return null;
  const above = tiers
    .filter((t) => t.route_id === routeId && t.group_size > n && t.price_per_person < current)
    .sort((a, b) => a.group_size - b.group_size)[0];
  if (!above) return null;
  return {
    add: above.group_size - n,
    saves: current - above.price_per_person,
  };
}

export default function BookingForm({
  routes,
  tiers,
  error,
}: {
  routes: Route[];
  tiers: PriceTier[];
  error?: string;
}) {
  const [routeId, setRouteId] = useState(routes[0]?.id ?? 0);
  const [people, setPeople] = useState(1);

  const perPerson = useMemo(() => priceFor(tiers, routeId, people), [tiers, routeId, people]);
  const saving = useMemo(() => nextSaving(tiers, routeId, people), [tiers, routeId, people]);

  return (
    <div className="grid gap-8 lg:grid-cols-[1.3fr_1fr] items-start">
      <form action={createTripRequest} className="card p-6 sm:p-8 grid gap-6">
        {error && <div className="notice-error">{error}</div>}

        <div>
          <label className="field-label" htmlFor="route_id">Route</label>
          <select
            className="field-input"
            id="route_id"
            name="route_id"
            value={routeId}
            onChange={(e) => setRouteId(Number(e.target.value))}
          >
            {routes.map((r) => (
              <option key={r.id} value={r.id}>
                {r.from_location} → {r.to_location}
              </option>
            ))}
          </select>
        </div>

        <div className="grid sm:grid-cols-2 gap-5">
          <div>
            <label className="field-label" htmlFor="travel_date">Arrival date</label>
            <input className="field-input" id="travel_date" name="travel_date" type="date" required />
          </div>
          <div>
            <label className="field-label" htmlFor="flight_no">Flight number <span className="normal-case font-normal">(if booked)</span></label>
            <input className="field-input" id="flight_no" name="flight_no" placeholder="KE 696" />
          </div>
        </div>

        <div>
          <label className="field-label">How many people?</label>
          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={() => setPeople((p) => Math.max(1, p - 1))}
              className="btn btn-ghost h-11 w-11 p-0 text-xl"
              aria-label="One less person"
            >
              −
            </button>
            <span className="font-display text-3xl w-10 text-center tabular">{people}</span>
            <button
              type="button"
              onClick={() => setPeople((p) => Math.min(12, p + 1))}
              className="btn btn-ghost h-11 w-11 p-0 text-xl"
              aria-label="One more person"
            >
              +
            </button>
            <input type="hidden" name="num_people" value={people} />
          </div>
        </div>

        {people > 1 && (
          <div>
            <label className="field-label" htmlFor="passenger_names">Passenger names</label>
            <textarea
              className="field-input"
              id="passenger_names"
              name="passenger_names"
              rows={2}
              placeholder="Sita Sharma, Ram Thapa, …"
            />
          </div>
        )}

        <div>
          <label className="field-label" htmlFor="contact">Contact that works before you land</label>
          <input
            className="field-input"
            id="contact"
            name="contact"
            placeholder="WhatsApp / Viber / Messenger"
          />
        </div>

        <div>
          <label className="field-label" htmlFor="notes">Anything else? <span className="normal-case font-normal">(luggage, exact address…)</span></label>
          <textarea className="field-input" id="notes" name="notes" rows={2} placeholder="3 big suitcases, going to SNU dormitory…" />
        </div>

        <button className="btn btn-primary w-full">Request this pickup</button>
        <p className="text-[13.5px] text-muted text-center -mt-2">
          No payment now — you pay the driver in cash when you arrive.
        </p>
      </form>

      {/* Live fare panel */}
      <aside className="ticket sticky top-24">
        <div className="px-6 pt-5 pb-4 border-b border-line">
          <p className="eyebrow mb-1">Your fare</p>
          <h2 className="text-lg">
            {routes.find((r) => r.id === routeId)?.from_location} →{" "}
            {routes.find((r) => r.id === routeId)?.to_location}
          </h2>
        </div>

        <div className="px-6 py-5 grid gap-4">
          <div className="flex items-baseline justify-between">
            <span className="text-ink-soft">
              {people} {people === 1 ? "person" : "people"} × per person
            </span>
            <span className="tabular font-semibold text-lg">
              {perPerson !== null ? `₩${perPerson.toLocaleString()}` : "—"}
            </span>
          </div>
          <div className="rule-dashed pt-4 flex items-baseline justify-between">
            <span className="font-semibold">Total, cash on arrival</span>
            <span className="font-display text-3xl tabular">
              {perPerson !== null ? `₩${(perPerson * people).toLocaleString()}` : "—"}
            </span>
          </div>
        </div>

        {saving && (
          <div
            className="px-6 py-4 text-[14.5px] font-medium rule-dashed"
            style={{ background: "var(--pine-tint)", color: "var(--pine)" }}
          >
            Bring {saving.add} more {saving.add === 1 ? "person" : "people"} and everyone
            pays ₩{saving.saves.toLocaleString()} less.
          </div>
        )}
      </aside>
    </div>
  );
}
