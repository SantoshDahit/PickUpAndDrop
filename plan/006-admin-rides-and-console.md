# 006 — Admin-published rides (joinable groups) + formal admin console

**Status:** Implemented (2026-07-26) — 33/33 backend tests green + 7/7 console browser checks
**Depends on:** [002](./002-booking-and-group-matching.md) (groups/matching), [003](./003-driver-management.md), [004](./004-admin-web.md), [005](./005-driver-accounts.md)

## 1. Problem / Goal

Today groups only form *organically* — a traveller books and matching finds companions. The admin wants to **publish rides proactively** ("ICN → Seoul on Sep 10") that travellers can see and join, seeding groups instead of waiting for coincidence. Separately, the admin console needs the formal resource pattern: list → detail → update/delete, plus create — including driver login creation (005) and ride creation (this plan).

## 2. The privacy line (decision)

002 rejected letting users browse groups because *user* groups carry strangers' travel plans. That stands. What becomes browsable is only **admin-published rides**, and their public listing shows **zero personal data**: route, target date, member count, seats left, current date span. Member names/intros stay members-only exactly as before. User-created (organic) groups remain unlistable and cannot be joined by id.

## 3. Design — backend

### 3.1 Schema (additive migration)

`travel_group` + `is_public BIT NOT NULL DEFAULT 0`, `target_date DATE NULL`. Public rides are created by the admin with a target date; organic groups keep both fields empty.

### 3.2 Rules

1. **Publish:** `POST /v1/admin/groups {routeId, targetDate}` → OPEN public group. Target date must be today…+365.
2. **Browse:** `GET /v1/groups/open` (any authenticated user) → public+OPEN rides with seats left ≥ 1, target date ≥ today, soonest first.
3. **Join by id:** `POST /v1/bookings` gains optional `groupId`. Valid only for public OPEN groups (`GROUP_NOT_JOINABLE` otherwise — including any organic-group id, which stays a 400 without confirming existence details). Seat capacity (`GROUP_SEATS_FULL`) and the 7-day span rule (`GROUP_DATE_OUT_OF_WINDOW`) apply; for public groups the span always includes the **target date**, keeping the ride anchored to what was advertised. `matchPref` is forced to GROUP.
4. **Organic matching seeds into public rides too:** empty public groups qualify via their target date (002's "never match into an empty group" applies only to organic groups, which have no anchor date).
5. **Close:** `PATCH /v1/admin/groups/{id}/close` — refused while active members exist (`GROUP_HAS_MEMBERS`); members must be handled first. Closed rides vanish from the browse list.
6. Everything else (chat, leave, cancel, status upkeep, driver assignment) works identically for public rides — they are ordinary groups once joined.

New ErrorCodes: `GROUP_NOT_JOINABLE(GRP_BR_001)`, `GROUP_DATE_OUT_OF_WINDOW(GRP_BR_002)`, `GROUP_SEATS_FULL(GRP_BR_003)`, `GROUP_HAS_MEMBERS(GRP_BR_004)`.

### 3.3 API summary

| Endpoint | Who | Behaviour |
|---|---|---|
| `POST /v1/admin/groups` | ADMIN | Publish a ride |
| `PATCH /v1/admin/groups/{id}/close` | ADMIN | Unpublish (no active members) |
| `GET /v1/groups/open` | any user | Browse joinable rides (no personal data) |
| `POST /v1/bookings` (+`groupId`) | USER | Join a specific published ride |

## 4. Design — admin console redesign

Formal resource pattern on every page: **list → row click → detail panel → edit / delete / actions**.

- **Rides** (new page): create form (route, target date), list of published rides (target date, members, seats left, span, driver), actions: assign driver, close.
- **Drivers**: list stays; row click opens a **detail panel** — full fields, inline edit (PATCH), status toggle, delete, and **Create login** (email + password, 005) with linked-account indicator.
- **Bookings**: table stays (list) with the existing assign/replace/unassign actions; group rows link contextually to the ride.

## 5. Acceptance criteria

- [ ] Admin publishes ICN→Seoul ride for +30d → appears in `/v1/groups/open` for a normal user with 6 seats left.
- [ ] User joins with `groupId` (+32d, party 2) → member; browse shows 4 seats left; a +45d join attempt → `GRP_BR_002`; joins totalling >6 seats → `GRP_BR_003`.
- [ ] Joining an organic group's id → `GRP_BR_001`; organic groups never appear in the browse list.
- [ ] A plain GROUP-pref booking within window auto-matches into the published ride (empty or not).
- [ ] Close with members → `GRP_BR_004`; after members leave/cancel, close works and the ride disappears from browse.
- [ ] Console: publish + close a ride from the Rides page; open a driver's detail, edit vehicle, create a login, delete a driver; all API errors render readable.
- [ ] All existing tests stay green.

## 6. Test plan

`OpenRideControllerTest` (Testcontainers): publish/browse/join happy path, window + seats + not-joinable guards, organic seeding into a published ride, close guards, role gates. Console verified by headless-browser pass (publish ride → join via API → see member count change → driver detail edit + login creation).
