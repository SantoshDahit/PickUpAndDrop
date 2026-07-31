# 007 — Customer frontend: the Next.js app, ported to the API

**Status:** Implemented (2026-07-26) — 11/11 two-traveller browser checks green
**Revision 2026-07-31 (b):** `/contact` added by [014](./014-support-chat.md) — direct chat with the team, reachable from the nav, footer, each trip card and the services card; group chat needed a group, so individual riders previously had no way to reach anyone
**Revision 2026-07-31:** `/services` added by [013](./013-traveller-services.md) — SIM card requests and an informational services shelf; **Services** joins the signed-in nav
**Revision 2026-07-30:** account-recovery pages and a form-failure fix, see §4
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

## 4. Revision 2026-07-30 — failed forms, and account recovery

Reported as "the signup button is not working". It wasn't: signup worked in production and
locally (303 → `/book`, cookies set, account created — reconfirmed in headless Chromium). The
defect was the **error path**. `signup` redirected to `/signup?error=…` and the page re-rendered
the form from scratch, so a rejected attempt — nearly always a duplicate email — threw away
everything typed. Click, fields empty, no obvious cause: indistinguishable from a dead button.

What changed, in the same design language:

- **Failed forms keep their input.** `signup`/`login` hand back the submitted name/email/phone
  (never the password) as search params; the pages render them as `defaultValue`.
- **Duplicate email offers a way out** — the notice links to "Log in instead" and "Forgot your
  password?" instead of only stating the problem. Keyed on `errorCode` (`USR_BR_001`), not on
  matching the message text.
- **`SubmitButton`** (`useFormStatus`) disables and shows a spinner while an action runs, so a
  click always acknowledges itself. Applied to signup, login and both recovery forms.
- **The one genuinely silent case is now visible:** a password under 6 characters was blocked by
  `minLength` with only a native tooltip, submitting nothing at all. The requirement is now
  printed under the field.
- **New pages** `/forgot-password` and `/reset-password` for [011](./011-transactional-email.md),
  plus a "Forgot your password?" link on `/login` and a `?reset=1` success banner. The reset page
  handles a missing token with an explanation and a "request a new link" button rather than a
  form that cannot work; a mismatched confirmation is caught client-side of the API.

Verified headless end-to-end: signup → forgot → emailed link → new password → login with it →
old password rejected; duplicate-email retry keeps all three fields and shows both links; zero
page errors.
