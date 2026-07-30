# 012 — Admin chat moderation: read, reply, and manage who is in a group

**Status:** Implemented (2026-07-30) — 65/65 `./gradlew test` green (12 new), verified end-to-end against a live API and in the admin console + customer app via headless Chromium
**Depends on:** [002](./002-booking-and-group-matching.md) (group chat), [006](./006-admin-rides-and-console.md) (admin console), [008](./008-week-bucket-groups.md) (landing-week join rules)

## 1. Problem / Goal

Group chat is where a ride actually gets organised — travellers agree a landing day, arrange
where to meet, ask what the driver's van looks like. Today **the operator cannot take part**.
A traveller asking "is the driver confirmed?" in the group gets silence, and the only way the
admin learns a group is stuck is by opening each one by hand.

The owner needs four things: **see the chats**, **open any of them**, **reply**, and **fix who is
in the group** (someone booked into the wrong ride, or should be moved into one that is filling).

## 2. Current state

Chat lives in `TravelGroupFacade` + `GroupMessage`; the customer UI is `/groups/[id]` (007).
Measured against the four asks:

| Ask | State today |
|---|---|
| See the list of chats | **Missing.** `/v1/admin/groups` only has `POST` (publish) and `PATCH /{id}/close` — no list, and `GET /v1/groups/open` is the public browse card list, not a chat index |
| Open any chat | **Already works.** `getById` and `getMessages` take `isAdmin` and skip the membership check (`requireMembershipOrAdmin`) — but there is no admin screen that calls them, and the payload is the *member* view (first names only) |
| Reply | **Blocked.** `postMessage` calls `requireMembership(...)` with no admin bypass, so an admin gets 404 `GRP_NF_002` on their own product |
| Add / remove a member | **Missing.** Only `DELETE /v1/groups/{groupId}/members/me` exists — self-service, one person, themselves |

Membership is not a join table: **a member is a `Booking` whose `travel_group_id` points at the
group**. `Booking.joinGroup` / `leaveGroup` are the primitives, and `BookingFacade.refreshGroupStatus`
recomputes OPEN/FULL/CLOSED from the members afterwards. Any "add/remove user" feature is
therefore really "attach/detach a booking", which is what makes seats and the driver roster add up.

## 3. Scope

**In:** admin chat index; admin group detail with real identities; admin reply posted as staff;
add a booking to a group; remove a booking from a group; an eligible-bookings list to pick from;
a Chats page in the admin console; staff replies visually distinct in the customer app.

**Out (deliberately):** email/push on membership changes (owner's call 2026-07-30 — plan 002's
"group changes notify nobody" still holds; the traveller sees it next time they open `/trips`.
One `MailService` method away if that changes); admin editing or deleting a traveller's message
(moderation-by-deletion is a bigger policy question — hiding evidence of a dispute is rarely
what you want, and nothing has needed it yet); realtime delivery (chat stays pull-based per 002
§4.6 — the admin page refreshes on open and after sending); unread counts per admin (needs a
per-admin read cursor; the list carries last-message time instead, which is enough to spot a
stale thread); admin-to-one-traveller direct messages (the group is the unit of work here);
attachments; adding a traveller who has **no booking** (there would be no seat, no route and no
travel date — the thing being added to a ride is a booking, not a person).

## 4. Design

### 4.1 Join rules apply to the admin too (owner's call, 2026-07-30)

`POST /v1/admin/groups/{id}/members` runs **the same three checks as a traveller's own join**
(008 §4.1, reusing `BookingFacade`'s logic): same route, same landing-week bucket, seats still
fit within `TravelGroup.MAX_SEATS`. A group is a physically real van on a real day; an admin
override would let one hold travellers bound for different cities, or nine people in a six-seat
vehicle, at which point the seat maths and the driver roster stop meaning anything.

The admin is not left guessing which bookings qualify: `GET /v1/admin/groups/{id}/candidates`
returns exactly the bookings that would pass, so the UI offers only valid choices and the
validation is a backstop rather than the interaction.

Rejected: a `force` flag (see above — the invariant is physical, not bureaucratic); silently
relaxing the week rule (it exists because a ride happens on one day; relaxing it produces groups
that can never agree a date).

### 4.2 Admin replies are staff replies

An admin's message must not arrive looking like another traveller's. `group_message` gains
`staff BOOLEAN NOT NULL DEFAULT FALSE`, set when the message is posted through the admin
endpoint, and the author label becomes **"Pickup & Drop team"** for those rows.

Persisted at write time rather than derived from `user.role == ADMIN`, because role is mutable:
deriving would silently relabel every historical message if an admin were ever demoted, and
would mislabel a message an admin posted as an ordinary member of a group they are travelling in.

`GroupMessageDto.Response` gains `staff`; the customer app renders those with a badge and the
team name instead of a first name (`mine` stays as-is, so a staff message is never "mine" to a
traveller).

### 4.3 Endpoints

All under the existing ADMIN route rule (`/v1/admin/**`), so authorization is inherited, not
re-implemented:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/v1/admin/groups/chats` | Chat index: route, week, status, member/seat counts, message count, last message preview + time, driver assigned. Sorted most-recently-active first |
| `GET` | `/v1/admin/groups/{id}/detail` | Admin group view — **full** member identities (name, email, phone, contact, party size, travel date, booking id), driver, week window |
| `GET` | `/v1/admin/groups/{id}/messages` | Transcript with real author names and a `staff` flag |
| `POST` | `/v1/admin/groups/{id}/messages` | Post a staff reply (201) |
| `GET` | `/v1/admin/groups/{id}/candidates` | Bookings eligible to add (§4.1) |
| `POST` | `/v1/admin/groups/{id}/members` | Add `{bookingId}` (204) |
| `DELETE` | `/v1/admin/groups/{id}/members/{bookingId}` | Remove (204) |

`GET /{id}/detail` and `GET /{id}/messages` are deliberately **separate from** the customer
`/v1/groups/{id}` endpoints rather than a widened payload on them: the customer response is a
privacy contract (002 §4.5 — members see first names, never email/phone/notes), and the safest
way to keep that true is for the admin view to be a different response object that no traveller
route can return.

New controller `AdminGroupChatController` rather than more methods on `AdminRideController`,
whose subject is publishing rides. Flow lives in `TravelGroupFacade` beside the existing chat
logic; the member add/remove reuses `BookingFacade.refreshGroupStatus` so status upkeep can't
drift between the traveller and admin paths.

### 4.4 Removal semantics

Removing a member **detaches the booking and leaves it active** — identical to the traveller's
own `leave`: the trip is still happening, just individually, and the group's status is recomputed
(possibly to CLOSED if it empties, unless it is a published ride). It is not a cancellation; an
admin cancelling a booking is `DELETE /v1/admin/bookings/{id}` and already exists.

An admin removing the last member of an organic group closes it, which is the same outcome as
the last traveller leaving — no special case.

## 5. Security & edge cases

- [x] Every new endpoint is under `/v1/admin/**`; a USER token gets 403, verified by test.
- [x] The customer group/chat responses are unchanged — no email/phone/notes leak into them.
- [x] Admin reply still validates body length 1–1000 (`MESSAGE_BODY_IS_INVALID`), same as members.
- [x] Adding a booking already in **another** group moves it (and refreshes both groups' status).
- [x] Adding a booking already in **this** group is a no-op, not a duplicate member.
- [x] Cancelled bookings are never candidates and cannot be added.
- [x] Removing a booking that isn't in the group → 404, not a silent success.
- [x] Seat overflow, route mismatch and week mismatch each return their existing error code.
- [x] Staff flag cannot be set through the customer endpoint.

## 6. Migration & rollout

One migration adding `group_message.staff` with `DEFAULT FALSE`, so every existing message stays
a traveller message and nothing needs backfilling. Additive to the API; the customer app tolerates
the new `staff` field before it is deployed (it simply ignores it), so backend and admin console
can ship independently of the Next.js app.

## 7. Acceptance

- [x] Admin console has a **Chats** page listing every group, most recently active first, showing
      route, landing week, members/seats, message count and the last message.
- [x] Opening a chat shows the full transcript plus each member's real name and contact details.
- [x] Admin can send a reply; it appears in the transcript and in the traveller's `/groups/{id}`
      labelled as the Pickup & Drop team, not as another traveller.
- [x] Admin can add an eligible booking; the picker lists only eligible ones; the member count
      and seats-left update.
- [x] Admin can remove a member; the booking stays active and continues individually.
- [x] A non-admin token is refused on every new endpoint.
- [x] Full suite green.

## 8. Test plan

`AdminGroupChatControllerTest` (integration, per convention 23): the chat index shape and
ordering; admin reply appears with `staff = true` and is refused for a blank/over-long body;
a USER token is refused on each endpoint; add/remove happy paths with status recomputation;
the three add rejections (wrong route, wrong week, seats full); moving a booking between groups;
re-adding an existing member is a no-op; removing a non-member is 404; and a check that the
**customer** transcript for the same group labels the staff message as staff while a member's
own message stays `mine`.

Manual verification: admin console Chats page against a live API — list, open, reply, add,
remove — with the traveller's `/groups/{id}` open alongside to confirm the staff badge.
