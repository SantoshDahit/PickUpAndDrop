# 007 — Customer frontend: the Next.js app, ported to the API

**Status:** Implemented (2026-07-26) — 11/11 two-traveller browser checks green
**Depends on:** 001–006 (full API surface)

## 1. Decision (owner, 2026-07-26)

**The Next.js app at the repo root is the main user pages** — the owner designed it and it stays.
An earlier same-day iteration of this plan built a separate Vite SPA (`customer-web/`); it was
removed the same day in favour of porting the Next.js app (git history keeps it). Plan 000's
"delete the prototype at cutover" language is void: the Next.js app is the product, the thing
that got deleted was its private SQLite data layer.

## 2. What the port changed (design untouched)

- `lib/db.ts` (SQLite/Turso) **deleted**; `lib/api.ts` server-side fetch wrapper to `/v1`
  (token from an httpOnly cookie — better than localStorage). `lib/session.ts` now stores the
  API JWT + user snapshot in cookies; `getSession()` keeps its old shape so layout/pages didn't change.
- `lib/actions.ts`: every server action proxies the API (signup/login/logout, password change,
  create booking incl. published-ride join, cancel, chat message, date change, leave group).
- New page in the same design language: `/groups/[id]` — members, agreement banner, driver card,
  chat, change-my-date, leave.
- `/book` gains the published-rides section ("Or join a ride that's already going") wired to 006.
- `/trips` reads `/v1/bookings/me`: ticket design kept; fare stub computed from the pricing tiers
  by party size (booking-level price snapshots are a future pricing plan).
- Admin pages/nav removed from the Next app — the admin console (004) owns that; the header shows
  an "Admin console" link for ADMIN sessions (`NEXT_PUBLIC_ADMIN_URL`).
- Backend addition: `price_tier` table seeded with the prototype's fares; `GET /v1/routes` now
  returns tiers, feeding the home fare calculator and BookingForm's live fare panel unchanged.
- Removed deps: `@libsql/client`, `bcryptjs`, `jose`.

## 3. Verified (headless, two travellers)

Home fares from API → signup → book (group default) → ticket with fare stub → second traveller
auto-matched → chat both ways → date convergence banner → published-ride join from `/book` →
account from API → auth guards. Zero page errors.
