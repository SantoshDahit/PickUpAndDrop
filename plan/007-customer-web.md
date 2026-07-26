# 007 — Customer web app

**Status:** Implemented (2026-07-26) — 10/10 two-traveller browser checks green
**Depends on:** 001–006 (full API surface)

## 1. Problem / Goal

Travellers have no UI — the Next.js prototype at the repo root is a frozen standalone demo on its own SQLite. Build the real customer web app against the `/v1` API: sign up, book (or join a published ride), coordinate in the group chat, manage trips and account.

## 2. Decisions

- **Vite + React + TypeScript SPA** in `customer-web/` (port 5174), same toolchain as `admin-web` — one frontend stack to maintain. SEO for the marketing landing page is consciously deferred (static prerender or a Next shell later); the app itself is behind login anyway.
- Design language ported from the frozen prototype (`app/globals.css` tokens) — same look travellers already saw.
- JWT in localStorage, fetch wrapper identical in shape to admin-web's. CORS: add `http://localhost:5174` to the backend allowlist.
- The prototype stays frozen until this app reaches parity (plan 000's cutover checklist); it still owns the fare-calculator demo (pricing is an unbuilt plan).

## 3. Pages

| Page | Backing API | Notes |
|---|---|---|
| `/` landing | `GET /v1/routes` | Hero + how-it-works, prototype copy; public |
| `/signup`, `/login` | `/v1/auth/*` | Auto-login on signup |
| `/book` | `POST /v1/bookings`, `GET /v1/groups/open` | The 002 form (route, date, party, group/individual, intro…) **plus** the 006 browse: published rides with seats/dates and a Join button that books straight into `groupId` |
| `/trips` | `GET /v1/bookings/me`, `DELETE /v1/bookings/{id}` | Cards with status, effective driver (name/vehicle/plate), group link, cancel |
| `/groups/:id` | group + messages endpoints | Members, agreement banner, driver card, chat (post), change my date, leave |
| `/account` | `/v1/users/me*` | Profile edit, password change, danger-zone delete |

## 4. Acceptance criteria

- [ ] Visitor lands, signs up, books with "group me" → trip card shows the group; a second user booking within window lands in the same group and they exchange chat messages from two browsers/sessions.
- [ ] `/book` lists published rides; Join books into that exact ride and it appears in `/trips`.
- [ ] Assigned driver (name, vehicle, plate) renders on the trip card and group page.
- [ ] Date change from the group page updates the member card; agreement banner appears when dates converge.
- [ ] Cancel, leave-group, profile edit, password change, and account deletion all round-trip with readable API error messages.
- [ ] Unauthenticated deep links bounce to `/login`; landing stays public.

## 5. Test plan

Backend contract is covered by the 33 integration tests; the SPA is verified by a headless-browser pass of the acceptance list (two users end-to-end). Frontend unit tests deferred, same policy as admin-web.
