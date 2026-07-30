# 002 — Booking with group matching and group chat

**Status:** Draft
**Revision 2026-07-30:** the operator joined the conversation — [012](./012-admin-chat-moderation.md) adds an admin chat index, a staff reply (`group_message.staff`, shown to travellers as "Pickup & Drop team"), and admin add/remove of group members. §4.5's privacy rules are unchanged for travellers; the admin views are separate response types.
**Revision 2026-07-26:** implemented as REST API per `springboot/conventions/` (see 000): `POST /v1/bookings` (matching), `GET /v1/bookings/me`, `PATCH /v1/bookings/{id}` (travel date), `DELETE /v1/bookings/{id}` (cancel), `GET /v1/groups/{id}`, `GET|POST /v1/groups/{id}/messages`, `DELETE /v1/groups/{id}/members/me` (leave), `GET /v1/admin/bookings/search`. Matching rules (§4.1), privacy rules (§4.5) and edge cases (§5) apply unchanged; the web-page/chat-transport details (§4.4, §4.6) are superseded by the JSON API.
**Stack:** Spring Boot + Thymeleaf (see [000](./000-stack-migration.md))
**Depends on:** [001](./001-user-accounts.md) (accounts, sessions, avatars)

## 1. Problem / Goal

A traveller books an airport pickup and tells us about themselves and their trip. The product's core promise: **if other travellers on the same route land within a week of each other, we group them** so they can split one van. Group members get a shared chat to introduce themselves and converge on a final landing day. Travellers who don't want strangers can choose to ride individually.

User stories:

- As a traveller I book a pickup: route, my preferred landing date, party size, flight number, a short intro about myself, and whether I'm open to being grouped or want to ride alone.
- As a grouped traveller I see who else is in my group (name, photo, party size, preferred date, intro), chat with them, and adjust my preferred date until we agree.
- As any booker I can cancel, and as a grouped booker I can leave the group and continue as an individual.
- As the admin I see all bookings and their groups in one place.

## 2. Current state

- Spring side has users/auth/avatars only (001). No booking tables yet.
- The frozen Next.js app has a `trip_requests` table (`lib/db.ts`) and a one-shot booking form (`components/BookingForm.tsx`) — **no grouping, no chat**; "group" there meant one person booking for N companions. That concept survives as `party_size` within a booking.
- `/trips` is a stub page; the header links to it.

## 3. Scope

**In:** routes reference table (seeded ICN→Seoul, ICN→Daejeon), booking creation with matching, travel groups, group page with chat and preferred-date editing, leave-group, cancel booking, my-trips page, read-only admin bookings overview.

**Out (deliberately):** pricing/fare tiers and the public fare calculator (003 — pricing deserves its own plan), admin driver assignment & booking status workflow (004), notifications of any kind (chat is pull-based; email/push later) — **superseded for one case: creating a booking now emails a receipt, [011](./011-transactional-email.md) (2026-07-30). Chat, group changes and driver assignment still notify nobody** — realtime chat transport (see §4.6), editing route list from admin UI, pagination.

## 4. Design

### 4.1 Matching semantics — the decisions that matter

1. **Window:** two bookings can share a group when they are on the **same route** and the group's **date span stays ≤ 7 days** after joining (span = max preferred date − min preferred date across members, including the joiner). This is stricter than "within 7 days of *someone*" and prevents chain drift (A=1st, B=7th, C=14th would otherwise end up together while A and C are two weeks apart).
2. **Capacity:** groups hold at most **6 seats** (typical van minus driver); a booking occupies `party_size` seats. A group at capacity stops matching (`full`).
3. **Assignment is greedy and immediate:** on booking, join the **oldest open qualifying group**; if none qualifies, found a new group. No re-balancing, no background jobs — deterministic, transactional, and understandable to users. SQLite's single-writer transaction is our race protection.
4. **The 7-day rule applies only at matching time.** After joining, members may move their preferred date freely — converging on a final day is exactly what the chat is for. The group page surfaces agreement instead of policing it: when every member's preferred date is identical, the page shows "Everyone agrees on {date}".
5. **Individual preference is honoured absolutely:** `match_pref = individual` bookings never enter matching, and there is no path by which a grouped stranger can see them.
6. **Leaving:** leaving a group flips the booking to individual and frees the seats; cancelling does the same and marks the booking cancelled. A group whose last active booking disappears is closed (kept for history, messages retained). Groups never merge.

Rejected alternatives: global optimal matching (bipartite/clustering — needless complexity at this scale, and re-shuffling existing groups after people have chatted is hostile), letting users browse/pick open groups (privacy: strangers' travel plans must not be listable), deferring matching to a nightly job (users want instant feedback that a group exists).

### 4.2 Schema (Flyway `V3__bookings.sql`)

```sql
CREATE TABLE routes (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  from_location TEXT NOT NULL,
  to_location   TEXT NOT NULL,
  active        INTEGER NOT NULL DEFAULT 1
);
INSERT INTO routes (from_location, to_location) VALUES
  ('Incheon Airport (ICN)', 'Seoul'),
  ('Incheon Airport (ICN)', 'Daejeon');

CREATE TABLE travel_groups (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  route_id   INTEGER NOT NULL REFERENCES routes(id),
  status     TEXT    NOT NULL DEFAULT 'open',   -- open | full | closed
  created_at TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE bookings (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id     INTEGER NOT NULL REFERENCES users(id),
  route_id    INTEGER NOT NULL REFERENCES routes(id),
  group_id    INTEGER REFERENCES travel_groups(id),  -- NULL = individual
  travel_date TEXT    NOT NULL,                      -- preferred landing day, YYYY-MM-DD
  flight_no   TEXT,
  party_size  INTEGER NOT NULL DEFAULT 1,            -- seats this booking occupies
  match_pref  TEXT    NOT NULL DEFAULT 'group',      -- group | individual
  intro       TEXT,                                  -- "about me", shown to group members
  contact     TEXT,
  notes       TEXT,
  status      TEXT    NOT NULL DEFAULT 'active',     -- active | cancelled
  created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
  updated_at  TEXT
);
CREATE INDEX idx_bookings_group ON bookings(group_id);
CREATE INDEX idx_bookings_user  ON bookings(user_id);

CREATE TABLE group_messages (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  group_id   INTEGER NOT NULL REFERENCES travel_groups(id),
  user_id    INTEGER NOT NULL REFERENCES users(id),
  body       TEXT    NOT NULL,
  created_at TEXT    NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX idx_messages_group ON group_messages(group_id);
```

Routes are seeded in the migration (reference data; an admin CRUD comes with 003/004). Booking `status` is deliberately just `active|cancelled` here — confirmed/completed/driver assignment belong to the admin workflow plan.

### 4.3 Domain (`com.landgreet.booking`)

- Entities: `Route`, `TravelGroup`, `Booking`, `GroupMessage` (thin, as per conventions).
- `BookingService` owns all mutations, `@Transactional`:
  - `createBooking(userId, form)` → validates route, runs matching (§4.1), returns the booking. Matching loads open groups for the route with their active bookings and checks span+capacity in Java — group counts are tiny; no clever SQL needed.
  - `cancelBooking(userId, bookingId)` / `leaveGroup(userId, bookingId)` — ownership checked by `user_id`, never trust a posted id alone (IDOR).
  - `updateTravelDate(userId, bookingId, date)`.
  - `postMessage(userId, groupId, body)` — membership required.
  - `groupView(userId, groupId)` — membership (or admin) required; assembles members + messages.
  - Group status upkeep: recompute `open|full|closed` after every join/leave/cancel.

### 4.4 Web layer

| Route | Behaviour |
|---|---|
| `GET/POST /book` | Booking form: route select, date picker, party size (1–6), flight no, intro, contact, notes, and the choice **"Group me with other travellers" / "I'd rather ride alone"**. Date must be today…+365d. On success redirect to `/trips` with a flash saying whether they joined a group, founded one, or booked solo. |
| `GET /trips` | Replaces the 001 stub: my active and past bookings — route, date, party size, status, and for grouped ones a card: "You're in a group of N travellers" + link. Cancel button per active booking. |
| `GET /groups/{id}` | **Members only** (admin may view). Left: member cards — avatar, first name, party size, preferred date, intro. A banner when all preferred dates agree. Right: chat — messages (author, time, body) + post form. Actions: change my preferred date, leave group. Non-members get **404, not 403** — a 403 confirms the group exists (information leak). |
| `POST /groups/{id}/messages` | Post a chat message (≤ 1000 chars). |
| `POST /groups/{id}/date` | Update my preferred date for this trip. |
| `POST /groups/{id}/leave` | Leave → booking becomes individual; redirect `/trips`. |
| `POST /trips/{bookingId}/cancel` | Cancel own booking. |
| `GET /admin/bookings` | Read-only: groups with members, then individual bookings. Ops overview only; management workflow is a later plan. |

Header gains a **Book** link; the home hero "Book a pickup" points at `/book`.

### 4.5 Privacy

- Group members see each other's **first name, avatar, intro, party size, preferred date** — not email, not phone, not notes (`notes`/`contact` are for the driver/admin only).
- Groups are unlistable and their ids are not enumerable in any UI; access is membership-checked server-side and non-membership returns 404.
- Chat bodies render through `th:text` (escaped) — no HTML injection.

### 4.6 Chat transport — decision

Plain **POST-redirect-GET with page reload**, no realtime. Members coordinate over hours/days, not seconds; a reload-on-post chat is completely adequate for an MVP, needs zero JS, and works on every device. When usage proves the need, the upgrade path is htmx polling of a messages fragment (10s interval), then SSE — in that order. WebSockets are explicitly rejected at this scale.

## 5. Security & edge cases

- [ ] Every mutation re-checks ownership (`booking.user_id == session user`) or membership; admin-only pages check role. Group 404 for non-members.
- [ ] Matching honours `individual` absolutely; a `group`-pref booking on a route with no candidates founds a group of one (they're told "we'll add travellers who match your dates").
- [ ] Span rule uses the **current preferred dates** of active members at match time.
- [ ] Capacity counts `party_size`, not bookings.
- [ ] Cancelled/left bookings free seats and can re-trigger `full → open`.
- [ ] Last member leaving/cancelling closes the group; closed groups never match; messages retained for audit.
- [ ] A user may hold several active bookings (two trips) — matching treats each independently; user's own two bookings may land in the same group (harmless).
- [ ] Message length cap (1000), intro cap (300), notes cap (1000); blank messages rejected.
- [ ] Date input validated as a real date within today…+365d.
- [ ] Deactivated users (001) lose access with their sessions; their bookings stay visible to the admin.

## 6. Migration & rollout

Additive only: `V3__bookings.sql` on existing DBs; no changes to 001 tables. `/trips` stub is replaced by the real page — no URL changes. Fresh boot seeds the two starter routes via the migration itself.

## 7. Acceptance criteria

- [ ] Booking with "group me" on a route with no matches → group of one is created; `/trips` shows the group card; flash explains it.
- [ ] Second user books the same route within 7 days → joins the same group; both see two member cards on the group page.
- [ ] Third user books the same route 10+ days away from the group's span → gets a **new** group, not that one.
- [ ] Booking with "ride alone" never appears in any group; no group page exists for it.
- [ ] Capacity: bookings totalling 6 seats mark the group full; the next matching booking founds a fresh group.
- [ ] Group chat: member posts a message → visible with author name and time; a 1001-char message is rejected; `<script>` in a message renders as text.
- [ ] Non-member requesting `/groups/{id}` gets 404. Anonymous gets redirected to login.
- [ ] Member changes preferred date → member card updates; when all members' dates are equal the agreement banner shows.
- [ ] Leave group → booking continues as individual, group seats freed; last leaver closes the group.
- [ ] Cancel booking → status cancelled on `/trips`, seats freed.
- [ ] `/admin/bookings` shows groups with members and individual bookings; regular users get 403.
- [ ] All 001 tests still pass; migration applies once on an existing 001 database.

## 8. Test plan

- **Unit (`BookingServiceTest`):** matching window boundaries (exactly 7 days joins; 8 days doesn't), span rule with three members, capacity counting party sizes, individual never matches, leave/cancel seat freeing and group closing, oldest-group preference.
- **Web (`GroupAccessIT`):** non-member 404, member 200, anonymous redirect, message post + escaping, CSRF on all POSTs.
- **Flow (`BookingFlowIT`):** two users end-to-end — book, land in same group, chat, converge dates, banner appears; third user out of window gets new group.
- **Manual pass** with the headless-browser scripts against `bootRun`.
