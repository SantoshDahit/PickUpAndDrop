# 004 — Admin web app (separate SPA)

**Status:** Implemented (2026-07-26) — headless-browser pass: login guard, non-admin refusal, driver CRUD, status toggle, dispatch via picker
**Depends on:** 001 (JWT auth), 002 (bookings API), 003 (drivers/dispatch API)

## 1. Problem / Goal

The admin currently operates via curl. They need webpages: see bookings and groups, manage the driver roster, and dispatch drivers to rides. Decision (owner-confirmed): the admin UI is a **separate app** from the future customer site — separate audience, isolated attack surface, dense-table UI, independent deploys.

## 2. Decisions

- **Vite + React + TypeScript SPA** in `admin-web/` at the repo root. Internal tool: no SSR/SEO needs, builds to static files servable from anywhere (later `admin.landgreet.com`).
- **Plain `fetch` wrapper + JWT in `localStorage`**, `Authorization: Bearer` on every call; 401 → redirect to login; non-ADMIN logins rejected client-side after inspecting the login response (the API enforces the real gate).
- **No UI framework** — hand-rolled CSS carrying the LandGreet design tokens; tables and forms only. React Router for `/login`, `/bookings`, `/drivers`.
- **Backend change (only one):** CORS allowlist in `SecurityConfig` — `app.cors.allowed-origins`, default `http://localhost:5173`, env-overridable for production.

## 3. Pages (v1)

| Page | Backing API | Behaviour |
|---|---|---|
| `/login` | `POST /v1/auth/login` | Email+password → token; refuses non-ADMIN users with a clear message |
| `/bookings` | `GET /v1/admin/bookings/search` (+`GET /v1/groups/{id}` for the drawer) | Paged table: route, date, party, status, group, effective driver. Row action **Assign driver** → driver picker (active, seat-filtered) → `PUT .../groups/{id}/driver` or `PUT .../bookings/{id}/driver` for individuals; unassign |
| `/drivers` | `/v1/admin/drivers/*` | Roster table + create form; edit inline (PATCH), ACTIVE↔INACTIVE toggle, delete (surfaces `DRV_BR_003` nicely) |

Out (later): users management page (needs an admin users API — not yet ported to REST), dashboards/stats, i18n.

## 4. Acceptance criteria

- [ ] Admin logs in, sees bookings incl. group + driver columns; regular user login is refused with a message.
- [ ] Assign a driver to a grouped booking via the picker → table shows the driver; travellers see the same driver via their API.
- [ ] Seat-capacity and INACTIVE errors from the API surface as readable messages, not blank failures.
- [ ] Driver CRUD round-trip works from the UI; delete-with-upcoming-rides shows the API's message.
- [ ] Direct URL access to `/bookings` without a token bounces to `/login`.

## 5. Test plan

Backend contract is already covered by 23 integration tests; the SPA is verified by a headless-browser pass of the acceptance list (login → bookings → assign → drivers CRUD) against the dev stack. Frontend unit tests deferred until the app grows logic worth testing.
