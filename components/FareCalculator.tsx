"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
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
  const [routeId, setRouteId] = useState(routes[0]?.id ?? 0);
  const [people, setPeople] = useState(2);

  const perPerson = useMemo(() => priceFor(tiers, routeId, people), [tiers, routeId, people]);
  const soloPrice = useMemo(() => priceFor(tiers, routeId, 1), [tiers, routeId]);
  const savesEach = soloPrice !== null && perPerson !== null ? soloPrice - perPerson : 0;

  return (
    <div className="ticket p-6 sm:p-7">
      <h2 className="font-display text-lg font-bold mb-5">Get an instant fare</h2>

      <div className="grid gap-4">
        <div>
          <label className="field-label" htmlFor="hero_route">Route</label>
          <select
            className="field-input"
            id="hero_route"
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

        <div>
          <label className="field-label">Passengers</label>
          <div className="flex items-center rounded-[10px] border border-line-strong overflow-hidden" style={{ borderColor: "var(--line-strong)" }}>
            <button
              type="button"
              onClick={() => setPeople((p) => Math.max(1, p - 1))}
              className="h-[46px] w-14 text-xl text-ink-soft hover:bg-paper-deep transition-colors cursor-pointer"
              aria-label="One less person"
            >
              −
            </button>
            <span className="flex-1 text-center font-display font-bold text-lg tabular border-x" style={{ borderColor: "var(--line)" }}>
              {people}
            </span>
            <button
              type="button"
              onClick={() => setPeople((p) => Math.min(12, p + 1))}
              className="h-[46px] w-14 text-xl text-ink-soft hover:bg-paper-deep transition-colors cursor-pointer"
              aria-label="One more person"
            >
              +
            </button>
          </div>
        </div>

        <div className="rounded-[10px] border border-line px-4 py-3.5 grid gap-1.5" style={{ background: "#fafafa" }}>
          <div className="flex items-baseline justify-between text-[14.5px]">
            <span className="text-ink-soft">Per person</span>
            <span className="tabular font-semibold">
              {perPerson !== null ? `₩${perPerson.toLocaleString()}` : "—"}
            </span>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-[14.5px] text-ink-soft">Total for {people}</span>
            <span className="font-display font-bold text-[22px] tabular">
              {perPerson !== null ? `₩${(perPerson * people).toLocaleString()}` : "—"}
            </span>
          </div>
          {savesEach > 0 && (
            <p className="text-[13px] font-medium" style={{ color: "var(--pine)" }}>
              Each of you saves ₩{savesEach.toLocaleString()} vs. riding alone
            </p>
          )}
        </div>

        <Link href="/book" className="btn btn-primary w-full">
          Book this pickup
        </Link>
        <p className="text-[13px] text-muted text-center -mt-1">
          No payment now — pay the driver in cash when you land.
        </p>
      </div>
    </div>
  );
}
