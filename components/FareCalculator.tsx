"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import type { PriceTier, Route } from "@/lib/db";

function priceFor(tiers: PriceTier[], routeId: number, n: number): number | null {
  const routeTiers = tiers.filter((t) => t.route_id === routeId);
  if (routeTiers.length === 0) return null;
  let match: PriceTier | null = null;
  for (const t of routeTiers) {
    if (t.group_size <= n) match = t;
  }
  return (match ?? routeTiers[0]).price_per_person;
}

export default function FareCalculator({
  routes,
  tiers,
}: {
  routes: Route[];
  tiers: PriceTier[];
}) {
  const router = useRouter();
  const [routeId, setRouteId] = useState(routes[0]?.id ?? 0);
  const [people, setPeople] = useState(2);
  const [date, setDate] = useState("");

  const perPerson = useMemo(() => priceFor(tiers, routeId, people), [tiers, routeId, people]);
  const total = perPerson !== null ? perPerson * people : null;

  function book() {
    const params = new URLSearchParams({ route: String(routeId), people: String(people) });
    if (date) params.set("date", date);
    router.push(`/book?${params.toString()}`);
  }

  const cell = "px-6 py-4 sm:py-3 text-left";
  const label = "block text-[12.5px] font-medium text-muted mb-0.5";

  return (
    <div className="bg-surface rounded-3xl sm:rounded-full shadow-lg border border-line p-3 grid sm:grid-cols-[1.35fr_1fr_1fr_auto] items-center gap-1">
      <div className={cell + " sm:border-r border-line"}>
        <span className={label}>Route</span>
        <select
          className="w-full bg-transparent font-display text-[15px] text-ink cursor-pointer focus:outline-none appearance-none"
          value={routeId}
          onChange={(e) => setRouteId(Number(e.target.value))}
          aria-label="Route"
        >
          {routes.map((r) => (
            <option key={r.id} value={r.id}>
              {r.from_location} → {r.to_location}
            </option>
          ))}
        </select>
      </div>

      <div className={cell + " sm:border-r border-line"}>
        <span className={label}>Arrival date</span>
        <input
          type="date"
          className="w-full bg-transparent font-display text-[15px] text-ink cursor-pointer focus:outline-none"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          aria-label="Arrival date"
        />
      </div>

      <div className={cell}>
        <span className={label}>Passengers</span>
        <span className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setPeople((p) => Math.max(1, p - 1))}
            className="flex h-7 w-7 items-center justify-center rounded-full border border-line-strong text-ink-soft hover:border-accent hover:text-accent-deep transition-colors cursor-pointer"
            aria-label="One less person"
          >
            −
          </button>
          <span className="font-display text-[15px] text-ink tabular w-5 text-center">{people}</span>
          <button
            type="button"
            onClick={() => setPeople((p) => Math.min(12, p + 1))}
            className="flex h-7 w-7 items-center justify-center rounded-full border border-line-strong text-ink-soft hover:border-accent hover:text-accent-deep transition-colors cursor-pointer"
            aria-label="One more person"
          >
            +
          </button>
        </span>
      </div>

      <button onClick={book} className="btn btn-primary !h-[60px] !px-8 m-1 w-full sm:w-auto">
        <span className="flex flex-col items-center leading-tight">
          <span className="text-[15px]">Book now</span>
          {total !== null && (
            <span className="text-[12.5px] font-normal text-white/85 tabular">
              ₩{total.toLocaleString()} total
            </span>
          )}
        </span>
      </button>
    </div>
  );
}
